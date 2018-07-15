package com.neo.audiokit.codec;

import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Created by liqian-ps on 2017/11/14.
 */

public class FFVideoEncode implements ICodec {

    private  final  String TAG = "FFVideoEncode";
    private long mHandle;
    private IMediaDataCallBack mCallback;
    private ProcessThread processThread;

    public FFVideoEncode() {
        mHandle = NativeMediaLib.videoEncoderCreate();
    }

    @Override
    public int openCodec(String mimeType, MediaFormat format, Surface surface, boolean isEncode) {
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        int igap = format.getInteger(MediaFormat.KEY_FRAME_RATE);
        NativeMediaLib.videoEncoderOpenEncode(mHandle, width, height, bitrate, igap);
//
        byte[] sps = NativeMediaLib.videoEncoderGetSPS(mHandle);
        byte[] pps = NativeMediaLib.videoEncoderGetPPS(mHandle);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        if(mCallback != null){
            mCallback.onMediaFormatChange(mediaFormat, IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo);
        }
        return 0;
    }

    @Override
    public int start() {
        processThread =  new ProcessThread();
        processThread.start();
        return 0;
    }

    @Override
    public int sendData(ByteBuffer data, CodecBufferInfo info) {
        int size;
        if ((info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0) {
            size = info.size;
        }else {
            size = 0;
        }
        while (true){
            int ret = NativeMediaLib.videoEncoderSendData(mHandle,data,size,info.presentationTimeUs);
            if(ret == 0){
                break;
            }else {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    public void initReadPixel(int width,int height,int apiLevel){
        NativeMediaLib.videoEncoderInitReadPixel(mHandle,width,height,apiLevel);
    }

    public void bind(){
        NativeMediaLib.videoEncoderBind(mHandle);
    }
    public void notifyReadPixel(long timeUs){
        NativeMediaLib.videoEncoderNotifyReadPixel(mHandle,timeUs);
    }
    public void unBind(){
        NativeMediaLib.videoEncoderUnBind(mHandle);
    }

    public void unInitReadPixel(){
        NativeMediaLib.videoEncoderUnInitReadPixel(mHandle);
    }

    //callback from native
    public int nativeCallBack(ByteBuffer byteBuffer,int size ,long pts,long dts,int picType){
        if(mCallback != null){
            CodecBufferInfo bufferInfo = new CodecBufferInfo();
            bufferInfo.size = size;
            bufferInfo.offset = 0;
            bufferInfo.flags = picType == 1 ? CodecBufferInfo.BUFFER_FLAG_KEY_FRAME : 0;
            bufferInfo.presentationTimeUs = pts;
            bufferInfo.decodeTimeUs = dts;
            mCallback.onMediaData(byteBuffer,bufferInfo,true,IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo);
        }
        return 0;
    }
    @Override
    public int closeCodec() {
        CodecBufferInfo bufferInfo = new CodecBufferInfo();
        bufferInfo.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
        sendData(null, bufferInfo);
        try {
            processThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        NativeMediaLib.videoEncoderCloseEncode(mHandle);
        NativeMediaLib.videoEncoderDestory(mHandle);
        return 0;
    }

    @Override
    public int sendEndFlag() {
        return 0;
    }

    @Override
    public int foreEndThread() {
        return 0;
    }

    @Override
    public int setCallBack(IMediaDataCallBack callBack) {
        mCallback = callBack;
        return 0;
    }

    @Override
    public Surface getInputSurface() {
        return null;
    }


    class ProcessThread extends Thread {
        @Override
        public void run() {
            int ret;
            while (true) {
                long time1 = SystemClock.elapsedRealtime();
                ret = NativeMediaLib.videoEncoderEncodeFrame(mHandle,FFVideoEncode.this);
                long time2 = SystemClock.elapsedRealtime();
               // RecorderLogUtils.d(TAG,"NativeMediaLib.videoEncoderEncodeFrame" + (time2 - time1));
                if(ret == -100){
                    break;
                }else if(ret <= 0){
                    try {
                        sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "processThread end");
        }
    }

}
