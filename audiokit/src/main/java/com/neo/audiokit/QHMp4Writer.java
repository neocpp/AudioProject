package com.neo.audiokit;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Surface;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.FFVideoEncode;
import com.neo.audiokit.codec.HWAudioEncode;
import com.neo.audiokit.codec.HWVideoEncode;
import com.neo.audiokit.codec.ICodec;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;


@SuppressLint("NewApi")
public class QHMp4Writer implements IMediaDataCallBack {
    private ICodec mVideoEncoder;
    private ICodec mAudioEncoder;
    private long mMp4MuxerHandle;
    private long mStartTime = 0;
    private double mSpeed = 1;
    private static final String TAG = "QHMp4Writer";
    private IMediaDataCallBack mMediaDataCallBack;
    private boolean mNeedWriteAudioTrack = false;
    private boolean mNeedWriteVideoTrack = false;
    private boolean mForceClose = false;
    private boolean mIsOpened = false;
    private MediaFormat mVideoCodecParamFormat;
    private MediaFormat mAudioCodecParamFormat;

    public int openWriter(String fileName, boolean isSoftVideoEncode, MediaFormat videoFormat, boolean bVideoIsRaw, MediaFormat audioFormat, boolean bAudioIsRaw, IMediaDataCallBack callBack) {

        mNeedWriteAudioTrack = (audioFormat != null);
        mNeedWriteVideoTrack = (videoFormat != null);

        mMp4MuxerHandle = NativeMediaLib.createMp4Muxer();
        if (mNeedWriteVideoTrack) {
            int width = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            int bitrate = videoFormat.containsKey(MediaFormat.KEY_BIT_RATE) ?
                    videoFormat.getInteger(MediaFormat.KEY_BIT_RATE) : (1024 * 1024 * 3);
            NativeMediaLib.setVideoConfig(mMp4MuxerHandle, 1, width, height, bitrate);
        } else {
            NativeMediaLib.setVideoConfig(mMp4MuxerHandle, 0, 0, 0, 0);
        }
        if (mNeedWriteAudioTrack) {
            int sampleRate = audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channelNum = audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            int bitrate = audioFormat.containsKey(MediaFormat.KEY_BIT_RATE) ? audioFormat.getInteger(MediaFormat.KEY_BIT_RATE) : 48000;
            NativeMediaLib.setAudioConfig(mMp4MuxerHandle, 1, sampleRate, channelNum, bitrate);
        } else {
            NativeMediaLib.setAudioConfig(mMp4MuxerHandle, 0, 0, 0, 0);
        }
        NativeMediaLib.openFile(mMp4MuxerHandle, fileName);
        if (mNeedWriteVideoTrack) {
            int rotation = videoFormat.containsKey(MediaFormat.KEY_ROTATION) ?
                    videoFormat.getInteger(MediaFormat.KEY_ROTATION) : 0;
            NativeMediaLib.setMetadata(mMp4MuxerHandle, "rotate", String.valueOf(rotation));
        }
        //根据IsRaw决定是否创建解码器
        if (videoFormat != null && bVideoIsRaw) {
            if (isSoftVideoEncode) {
                mVideoEncoder = new FFVideoEncode();
            } else {
                mVideoEncoder = new HWVideoEncode();
            }
            mVideoEncoder.setCallBack(this);
//            mVideoEncoder.openCodec(MediaFormat.MIMETYPE_VIDEO_AVC, videoFormat, null, true);
        } else {
            mVideoCodecParamFormat = videoFormat;
        }
        if (audioFormat != null && bAudioIsRaw) {
            mAudioEncoder = new HWAudioEncode();
            mAudioEncoder.setCallBack(this);
//            mAudioEncoder.openCodec(MediaFormat.MIMETYPE_AUDIO_AAC, audioFormat, null, true);
        } else {
            mAudioCodecParamFormat = audioFormat;
        }
        mMediaDataCallBack = callBack;
        mIsOpened = true;
        Log.d(TAG, TAG + ".openWriter");
        return 0;
    }

    public int setMetaData(String key, String value) {
        if (mMp4MuxerHandle != 0) {
            NativeMediaLib.setMetadata(mMp4MuxerHandle, key, value);
        }
        return 0;
    }

    public int start() {
        if (mVideoEncoder != null) {
            mVideoEncoder.start();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.start();
        }
        return 0;
    }

    public int closeWriter() {
        if (mVideoEncoder != null) {
            mVideoEncoder.closeCodec();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.closeCodec();
            mAudioEncoder = null;
        }
        closeMp4Muxer();
        mIsOpened = false;
        Log.d(TAG, TAG + "closeWriter");
        return 0;
    }

    public void forceClose() {
        if (mVideoEncoder != null) {
            mVideoEncoder.foreEndThread();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.foreEndThread();
        }
        mForceClose = true;
    }

    public void initReadPixel(int width, int height, int apiLevel) {
        waitForOpen();
        if (mVideoEncoder instanceof FFVideoEncode) {
            FFVideoEncode encoder = (FFVideoEncode) mVideoEncoder;
            encoder.initReadPixel(width, height, apiLevel);
        }
    }

    public void bind() {
        waitForOpen();
        if (mVideoEncoder instanceof FFVideoEncode) {
            FFVideoEncode encoder = (FFVideoEncode) mVideoEncoder;
            encoder.bind();
        }
    }

    public void notifyReadPixel(long timeUs) {
        waitForOpen();
        if (mVideoEncoder instanceof FFVideoEncode) {
            FFVideoEncode encoder = (FFVideoEncode) mVideoEncoder;
            encoder.notifyReadPixel(timeUs);
        }
    }

    public void unBind() {
        waitForOpen();
        if (mVideoEncoder instanceof FFVideoEncode) {
            FFVideoEncode encoder = (FFVideoEncode) mVideoEncoder;
            encoder.unBind();
        }
    }

    public void unInitReadPixel() {
        if (mVideoEncoder instanceof FFVideoEncode) {
            FFVideoEncode encoder = (FFVideoEncode) mVideoEncoder;
            encoder.unInitReadPixel();
        }
    }

    public int sendData(ByteBuffer data, CodecBufferInfo info, boolean isRawData, int trackType) {
        waitForOpen();
        if (isRawData) {
            if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
                if (mNeedWriteAudioTrack) mAudioEncoder.sendData(data, info);
            } else {
                if (mNeedWriteVideoTrack) mVideoEncoder.sendData(data, info);
            }
        } else {
            writeFrame(data, info, trackType);
        }

        return -1;
    }

    public Surface getInputSurface() {
        if (mVideoEncoder == null)
            return null;
        return mVideoEncoder.getInputSurface();
    }

    public int setSpeed(double dSpeed) {
        mSpeed = dSpeed;
        return 0;
    }

    private int writeFrame(ByteBuffer data, CodecBufferInfo info, int trackType) {
        if (data != null && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0) {
            byte[] bytedata = new byte[info.size];
            data.position(info.offset);
            data.get(bytedata);
            if (trackType == IMediaDataCallBackTrackTypeVideo) {
                if (mSpeed != 1) {
                    changePts(info);
                }
            }
            long duration = trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo ? 33333 : 23220;
            int isKeyFrame = (info.flags & CodecBufferInfo.BUFFER_FLAG_KEY_FRAME);
            Log.d(TAG, TAG + ".writeFrame pts:" + info.presentationTimeUs + " dts:" + info.decodeTimeUs +
                    " duration:" + duration);
            NativeMediaLib.writeSampleData(mMp4MuxerHandle, trackType, bytedata, info.size, info.presentationTimeUs, info.decodeTimeUs,
                    duration, isKeyFrame);
        }
        return 0;
    }


    @Override
    public void onMediaFormatChange(MediaFormat format, int trackType) {
        // addTrack(format,trackType);
        Log.d(TAG, TAG + ".onMediaFormatChange" + trackType);
        if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo) {
            mVideoCodecParamFormat = format;
        } else {
            mAudioCodecParamFormat = format;
        }
        if (mMediaDataCallBack != null) {
            mMediaDataCallBack.onMediaFormatChange(format, trackType);
        }
    }

    @Override
    public int onMediaData(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType) {
        writeFrame(mediaData, info, trackType);
        return 0;
    }

    private void changePts(CodecBufferInfo bufferInfo) {
        long pts = bufferInfo.presentationTimeUs;
        Log.i(TAG, "wsddebug encodec pts curtime" + pts);
        if (mStartTime == 0) {
            mStartTime = pts;
            Log.i(TAG, " mStartTime " + pts);

        } else {
            long interval;
            if (mSpeed > 1) {
                interval = (long) ((double) (pts - mStartTime) / (mSpeed));
                pts = mStartTime + interval;
            } else {
                interval = (long) ((double) (pts - mStartTime) / (mSpeed));
                pts = mStartTime + interval;
            }
        }
        bufferInfo.presentationTimeUs = pts;
        bufferInfo.decodeTimeUs = bufferInfo.presentationTimeUs;
    }

    private int closeMp4Muxer() {
        int width = 0;
        int height = 0;
        byte[] extVideoData = null;
        int extVideoDataSize = 0;
        int offset = 0;
        byte[] extAudioData = null;
        int extAudioDataSize = 0;
        if (mNeedWriteVideoTrack) {
            width = mVideoCodecParamFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = mVideoCodecParamFormat.getInteger(MediaFormat.KEY_HEIGHT);
            if (mVideoCodecParamFormat.containsKey("csd-0")) {
                ByteBuffer csd0Buffer = mVideoCodecParamFormat.getByteBuffer("csd-0");
                extVideoDataSize += csd0Buffer.limit();
            }
            if (mVideoCodecParamFormat.containsKey("csd-1")) {
                ByteBuffer csd1Buffer = mVideoCodecParamFormat.getByteBuffer("csd-1");
                extVideoDataSize += csd1Buffer.limit();
            }
            if (extVideoDataSize > 0) {
                extVideoData = new byte[extVideoDataSize];
            }
            if (mVideoCodecParamFormat.containsKey("csd-0")) {
                ByteBuffer csd0Buffer = mVideoCodecParamFormat.getByteBuffer("csd-0");
                int size = csd0Buffer.limit();
                ;
                offset = size;
                csd0Buffer.get(extVideoData, 0, size);
            }
            if (mVideoCodecParamFormat.containsKey("csd-1")) {
                ByteBuffer csd1Buffer = mVideoCodecParamFormat.getByteBuffer("csd-1");
                int size = csd1Buffer.limit();
                ;
                csd1Buffer.get(extVideoData, offset, size);
            }
        }
        if (mNeedWriteAudioTrack) {
            if (mAudioCodecParamFormat.containsKey("csd-0")) {
                ByteBuffer csd0Buffer = mAudioCodecParamFormat.getByteBuffer("csd-0");
                extAudioDataSize = csd0Buffer.limit();
                extAudioData = new byte[extAudioDataSize];
                csd0Buffer.get(extAudioData, 0, extAudioDataSize);
            }
        }
        NativeMediaLib.closeFile(mMp4MuxerHandle, width, height, extVideoData, extVideoDataSize, extAudioData, extAudioDataSize);
        NativeMediaLib.destroyMp4Muxer(mMp4MuxerHandle);
        return 0;
    }

    private void waitForOpen() {
        while (true) {
            if (mIsOpened || mForceClose) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
