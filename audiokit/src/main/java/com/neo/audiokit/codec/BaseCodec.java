
package com.neo.audiokit.codec;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@SuppressLint("NewApi")
public class BaseCodec implements ICodec {
    private static final String TAG = "BaseCodec";
    final int TIMEOUT_USEC = 10000;
    protected MediaCodec mMediaCodec;
    protected ByteBuffer[] mOutputBuffers;
    protected ByteBuffer[] mInputBuffers;
    protected Surface mSurface;
    private BlockingQueue<BaseQHCodecFrame> mFrameQueue;
    private Thread mEatThread;
    private Thread mFeedThread;
    private MediaCodec.BufferInfo mBufferInfo;
    protected IMediaDataCallBack mMediaDataCallBack;
    private boolean mIsEncode = false;
    private boolean mIsThreadRunning =  true;
    private boolean mNotifyEatThreadStopFlag = false;
    private  int mIInterval = 1;

    private static class BaseQHCodecFrame{
        byte[] frameData;
        MediaCodec.BufferInfo frameInfo;
    }
    @Override
    public int openCodec(String mimeType, MediaFormat format, Surface surface, boolean isEncode) {
        int codeFlag;
        mSurface = surface;
        mIsEncode = isEncode;
        mIsThreadRunning = true;
        if(format.containsKey(android.media.MediaFormat.KEY_I_FRAME_INTERVAL)){
            mIInterval = format.getInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL);
        }
        try {
            if(isEncode){
                mMediaCodec = MediaCodec.createEncoderByType(mimeType);
                codeFlag = MediaCodec.CONFIGURE_FLAG_ENCODE;
            }else {
                mMediaCodec = MediaCodec.createDecoderByType(mimeType);
                codeFlag = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"BaseCodec::openCodec create codec faled" + mIsEncode);
            return -1;
        }
        android.media.MediaFormat mediaFormat = QHMediaFormatToMediaFormat(format);
        try {
            mMediaCodec.configure(mediaFormat,mSurface,null,codeFlag);
        } catch (Exception e) {
            e.printStackTrace();
            if (mIInterval == 0 && mIsEncode) {
                //增加兼容性逻辑，部分机型设置全I帧编码的时候可能会执行这个异常捕获。
                format.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, -1);
                mediaFormat.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, -1);
                mMediaCodec.configure(mediaFormat,mSurface,null,codeFlag);
            }
        }
        if(format.containsKey(android.media.MediaFormat.KEY_COLOR_FORMAT) && format.getInteger(android.media.MediaFormat.KEY_COLOR_FORMAT) ==  MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface && isEncode){
            mSurface = mMediaCodec.createInputSurface();
        }
        mFrameQueue = new ArrayBlockingQueue<>(5, true);
        mBufferInfo = new MediaCodec.BufferInfo();
        Log.d(TAG,"BaseCodec::openCodec end" + mIsEncode);


        return 0;
    }

    @Override
    public int start() {
        mMediaCodec.start();
        mOutputBuffers = mMediaCodec.getOutputBuffers();
        mInputBuffers = mMediaCodec.getInputBuffers();

        if(mSurface == null || !mIsEncode){
            mFeedThread = new FeedThread();
        }else {
            mNotifyEatThreadStopFlag = true;
        }
        mEatThread = new EatThread();
        if(mEatThread != null){
            mEatThread.start();
        }
        if(mFeedThread != null){
            mFeedThread.start();
        }
        Log.d(TAG,"BaseCodec::start end" + mIsEncode);
        return 0;
    }

    @Override
    public int sendData(ByteBuffer data, CodecBufferInfo info) {
        BaseQHCodecFrame frame = new BaseQHCodecFrame();
        byte[]bytesdata = null;
        if((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0){
            bytesdata = new byte[info.size];
            data.position(info.offset);
            data.get(bytesdata);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.presentationTimeUs = info.presentationTimeUs;
        bufferInfo.flags = info.flags;
        bufferInfo.offset = info.offset;
        bufferInfo.size = info.size;
        frame.frameData = bytesdata;
        frame.frameInfo = bufferInfo;
        try {
            mFrameQueue.put(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int closeCodec() {
        Log.d(TAG,"BaseCodec::closeCodec start " + mIsEncode);
        try {
            if(mFeedThread != null){
                mFeedThread.join();
            }
            if(mEatThread != null){
                mEatThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        Log.d(TAG,"BaseCodec::closeCodec end" + mIsEncode);
        return 0;
    }

    @Override
    public int sendEndFlag() {
        return 0;
    }

    @Override
    public int foreEndThread() {
        mIsThreadRunning = false;
        return 0;
    }

    @Override
    public int setCallBack(IMediaDataCallBack callBack) {
        mMediaDataCallBack = callBack;
        return 0;
    }

    @Override
    public Surface getInputSurface() {
        return mSurface;
    }

    protected int processOutBuffer(ByteBuffer byteBuffer, CodecBufferInfo info){
        return 0;
    }
    protected int processMediaFormatChange(MediaFormat mediaFormat){
        return 0;
    }

    private class EatThread extends Thread {
        public EatThread() {
            super("EatThread");
        }

        @Override
        public void run() {
            Log.d(TAG,"BaseCodec EatThread start " + mIsEncode);
            while (mIsThreadRunning || !mNotifyEatThreadStopFlag) {
                //如果要求是全I帧的话，每次都设置一下，解决OPPO R7型编码出来的视频不是全I帧的bug。
                boolean needForceIFrame = (Build.MANUFACTURER.equals("OPPO") && Build.MODEL.equals("OPPO R7")) || (Build.MANUFACTURER.equals("Xiaomi")) || Build.MODEL.equals("Coolpad 8675-HD");
                if (Build.VERSION.SDK_INT >= 19 && mIInterval == 0 && needForceIFrame && mIsEncode) {
                    Bundle params = new Bundle();
                    params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                    mMediaCodec.setParameters(params);
                }
                int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    continue;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    android.media.MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    MediaFormat format = MediaFormatToQHMediaFormat(newFormat);
                    processMediaFormatChange(format);
                    Log.d(TAG,"EatThread newFormat " + mIsEncode + " " + newFormat);
                } else if (encoderStatus < 0) {
                } else {
                    boolean bRender = (!mIsEncode && mSurface != null);
                    if(bRender){
                        mMediaCodec.releaseOutputBuffer(encoderStatus, true);
                    }
                    ByteBuffer outData = mOutputBuffers[encoderStatus];
//                    if (outData == null) {
//                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
//                                " was null");
//                    }


                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && mIsEncode) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.");
                        mBufferInfo.size = 0;
                    }
                    if (mBufferInfo.size != 0 || (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        CodecBufferInfo bufferInfo = BufferInfoToQHBufferInfo(mBufferInfo);
                        processOutBuffer(outData,bufferInfo);
                    }
                    if(!bRender){
                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG,"BaseCodec EatThread end return " + mIsEncode );
                        return;      // out of while
                    }
                }
            }
            mBufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            CodecBufferInfo bufferInfo = BufferInfoToQHBufferInfo(mBufferInfo);
            processOutBuffer(null,bufferInfo);
            Log.d(TAG,"BaseCodec EatThread end" + mIsEncode);
        }
    }
    private class FeedThread extends Thread {
        public FeedThread() {
            super("FeedThread");
        }

        private boolean process(byte[]datas, MediaCodec.BufferInfo bufferInfo, boolean isEOS) {
            while (true) {
                int inputbufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputbufferIndex >= 0) {
                    ByteBuffer buffers = mInputBuffers[inputbufferIndex];
                    buffers.clear();
                    int size = datas == null ? 0 : datas.length;
                    if (size > 0) {
                        buffers.limit(size);
                        buffers.put(datas);
                    }
                    mMediaCodec.queueInputBuffer(inputbufferIndex, 0, size, bufferInfo.presentationTimeUs,
                            isEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    //Log.d(TAG,"BaseCodec FeedThread " + mIsEncode + " " + bufferInfo.presentationTimeUs + " " + bufferInfo.size + " " + bufferInfo.flags);
                    return true;
                }
            }
        }

        @Override
        public void run() {
            Log.d(TAG,"BaseCodec FeedThread start " + mIsEncode);
            while (true) {
                try {

                    BaseQHCodecFrame frame = mFrameQueue.take();
                    if (frame == null) {
                        continue;
                    }
                    boolean isEOS = (frame.frameInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    process(frame.frameData,frame.frameInfo, isEOS);
                    if (isEOS) {
                        mNotifyEatThreadStopFlag = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG,"BaseCodec FeedThread end " + mIsEncode);

        }
    }

    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
    public static int[] VIDEO_ENCODE_SIZE;
    public  static  int[] getVideoEncodeAlignmentSize(){
        if(VIDEO_ENCODE_SIZE == null) {
            int[] ret = {16, 16};

            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    MediaCodecInfo codecInfo = selectCodec("video/avc");
                    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
                    MediaCodecInfo.VideoCapabilities videoCaps = capabilities.getVideoCapabilities();
                    ret[0] = videoCaps.getWidthAlignment();
                    ret[1] = videoCaps.getHeightAlignment();
                    Log.d(TAG, "wsddebug alignmentWidth=" + ret[0] + ", alignmentHeigth=" + ret[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            VIDEO_ENCODE_SIZE = ret;
        }
        return VIDEO_ENCODE_SIZE;
    }

    private MediaFormat MediaFormatToQHMediaFormat(android.media.MediaFormat mediaFormat){
        MediaFormat qhMediaFormat = new MediaFormat();
        //common
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_MIME)){
            qhMediaFormat.setString(android.media.MediaFormat.KEY_MIME,mediaFormat.getString(android.media.MediaFormat.KEY_MIME));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_BIT_RATE)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_BIT_RATE,mediaFormat.getInteger(android.media.MediaFormat.KEY_BIT_RATE));
        }
        if(mediaFormat.containsKey("csd-0")){
            ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-0");
            qhMediaFormat.setByteBuffer("csd-0",byteBuffer);
        }
        if(mediaFormat.containsKey("csd-1")){
            ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-1");
            qhMediaFormat.setByteBuffer("csd-1",byteBuffer);
        }
        //video
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_WIDTH)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_WIDTH,mediaFormat.getInteger(android.media.MediaFormat.KEY_WIDTH));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_HEIGHT)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_HEIGHT,mediaFormat.getInteger(android.media.MediaFormat.KEY_HEIGHT));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_COLOR_FORMAT)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,mediaFormat.getInteger(android.media.MediaFormat.KEY_COLOR_FORMAT));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_FRAME_RATE)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_FRAME_RATE,mediaFormat.getInteger(android.media.MediaFormat.KEY_FRAME_RATE));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_I_FRAME_INTERVAL)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL,mediaFormat.getInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_ROTATION)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_ROTATION,mediaFormat.getInteger(android.media.MediaFormat.KEY_ROTATION));
        }
        //audio
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_SAMPLE_RATE,mediaFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT,mediaFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_AAC_PROFILE)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_AAC_PROFILE,mediaFormat.getInteger(android.media.MediaFormat.KEY_AAC_PROFILE));
        }
        if(mediaFormat.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)){
            qhMediaFormat.setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE,mediaFormat.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE));
        }
        return qhMediaFormat;
    }
    private android.media.MediaFormat QHMediaFormatToMediaFormat(MediaFormat qhMediaFormat){
        android.media.MediaFormat mediaFormat = new android.media.MediaFormat();
        //common
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_MIME)){
            mediaFormat.setString(android.media.MediaFormat.KEY_MIME,qhMediaFormat.getString(android.media.MediaFormat.KEY_MIME));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_BIT_RATE)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_BIT_RATE,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_BIT_RATE));
        }
        if(qhMediaFormat.containsKey("csd-0")){
            ByteBuffer byteBuffer = qhMediaFormat.getByteBuffer("csd-0");
            mediaFormat.setByteBuffer("csd-0",byteBuffer);
        }
        if(qhMediaFormat.containsKey("csd-1")){
            ByteBuffer byteBuffer = qhMediaFormat.getByteBuffer("csd-1");
            mediaFormat.setByteBuffer("csd-1",byteBuffer);
        }
        //video
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_WIDTH)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_WIDTH,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_WIDTH));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_HEIGHT)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_HEIGHT,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_HEIGHT));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_COLOR_FORMAT)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_COLOR_FORMAT));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_FRAME_RATE)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_FRAME_RATE,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_FRAME_RATE));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_I_FRAME_INTERVAL)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_ROTATION)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_ROTATION,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_ROTATION));
        }
        //audio
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_SAMPLE_RATE,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_AAC_PROFILE)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_AAC_PROFILE,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_AAC_PROFILE));
        }
        if(qhMediaFormat.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)){
            mediaFormat.setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE,qhMediaFormat.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE));
        }
        return mediaFormat;
    }
//    private MediaCodec.BufferInfo QHBufferInfoToBufferInfo(CodecBufferInfo qhCodecBufferInfo){
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        bufferInfo.flags = qhCodecBufferInfo.flags;
//        bufferInfo.presentationTimeUs = qhCodecBufferInfo.presentationTimeUs;
//        bufferInfo.offset = qhCodecBufferInfo.offset;
//        bufferInfo.size = qhCodecBufferInfo.size;
//        return bufferInfo;
//    }
    private CodecBufferInfo BufferInfoToQHBufferInfo(MediaCodec.BufferInfo bufferInfo){
        CodecBufferInfo qhCodecBufferInfo = new CodecBufferInfo();
        qhCodecBufferInfo.flags = bufferInfo.flags;
        qhCodecBufferInfo.offset = bufferInfo.offset;
        qhCodecBufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs;
        qhCodecBufferInfo.size = bufferInfo.size;
        return qhCodecBufferInfo;
    }
}
