package com.neo.audiokit.codec;

import android.view.Surface;

import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;

public class FFAudioEncode implements ICodec {
    private String TAG = "FFAudioEncode";
    private long mEncodeHandle;
    private IMediaDataCallBack mCallBack;
    @Override
    public int openCodec(String mimeType, android.media.MediaFormat format, Surface surface, boolean isEncode) {
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelNum = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int bitsrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        if(mEncodeHandle == 0){
            mEncodeHandle = NativeMediaLib.audioEncoderCreate();
            NativeMediaLib.audioEncoderOpenEncode(mEncodeHandle,sampleRate,channelNum,bitsrate);
        }
        if(mCallBack != null){
            MediaFormat newFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,channelNum);
            byte[]csd0 = NativeMediaLib.audioEncoderGetCsd0(mEncodeHandle);
            ByteBuffer byteBuffer = ByteBuffer.wrap(csd0);
            newFormat.setByteBuffer("csd-0",byteBuffer);
            mCallBack.onMediaFormatChange(newFormat,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
        return 0;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int sendData(ByteBuffer data, CodecBufferInfo info) {
        if(mEncodeHandle != 0 && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0){
            NativeMediaLib.audioEncoderEncodeFrame(mEncodeHandle,FFAudioEncode.this,data,info.size,info.presentationTimeUs);
        }
        return 0;
    }

    @Override
    public int closeCodec() {
        if(mEncodeHandle != 0){
            NativeMediaLib.audioEncoderCloseEncode(mEncodeHandle);
            NativeMediaLib.audioEncoderDestory(mEncodeHandle);
            mEncodeHandle = 0;
        }
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
        mCallBack = callBack;
        return 0;
    }

    @Override
    public Surface getInputSurface() {
        return null;
    }

    //callback from native
    public int nativeCallBack(ByteBuffer byteBuffer,int size ,long pts){
        if(mCallBack != null){
            CodecBufferInfo bufferInfo = new CodecBufferInfo();
            bufferInfo.size = size;
            bufferInfo.offset = 0;
            bufferInfo.flags = 0;
            bufferInfo.presentationTimeUs = pts;
            bufferInfo.decodeTimeUs = pts;
            mCallBack.onMediaData(byteBuffer,bufferInfo,false,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
        return 0;
    }
}
