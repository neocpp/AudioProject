package com.neo.audiokit.codec;

import android.view.Surface;

import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FFAudioDecode implements ICodec {
    private String TAG = "FFAudioDecode";
    private static final int AudioDecoderCodeID_MP3 = 1;
    private static final int AudioDecoderCodeID_AAC = 2;
    private long mAudioDecoderHandle;
    private IMediaDataCallBack mCallBack;
    @Override
    public int openCodec(String mimeType, android.media.MediaFormat format, Surface surface, boolean isEncode) {
        int codeId = 0;
        int sampleRate = 0;
        int channelNum = 0;
        byte[]extData = null;
        int extDataSize = 0;
        if(format.getString(MediaFormat.KEY_MIME).equals(MediaFormat.MIMETYPE_AUDIO_AAC)){
            codeId = AudioDecoderCodeID_AAC;
        }else {
            codeId = AudioDecoderCodeID_MP3;
        }
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelNum = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mAudioDecoderHandle = NativeMediaLib.audioDecoderCreate();
        if(format.containsKey("csd-0")){
            ByteBuffer byteBuffer = format.getByteBuffer("csd-0");
            extData = new byte[byteBuffer.limit()];
            byteBuffer.get(extData);
            extDataSize = extData.length;
        }
        NativeMediaLib.audioDecoderOpenDecode(mAudioDecoderHandle,sampleRate,channelNum,codeId,extData,extDataSize);
        if(mCallBack != null){
//            mCallBack.onMediaFormatChange(format,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
        return 0;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int sendData(ByteBuffer data, CodecBufferInfo info) {
        if(mAudioDecoderHandle != 0 && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0){
            int ret = NativeMediaLib.audioDecoderDecodeFrame(mAudioDecoderHandle,data,info.size,info.presentationTimeUs);
            if(ret > 0){
                CodecBufferInfo bufferInfo = new CodecBufferInfo();
                bufferInfo.presentationTimeUs =  NativeMediaLib.audioDecoderGetFramePts(mAudioDecoderHandle);
                bufferInfo.flags = 0;
                bufferInfo.offset = 0;
                bufferInfo.size = ret;
                ByteBuffer byteBuffer = NativeMediaLib.audioDecoderGetFrameOutBuffer(mAudioDecoderHandle);

                byteBuffer.position(0);
                byteBuffer.limit(ret);
                byteBuffer.order(ByteOrder.nativeOrder());

                if(mCallBack != null){
                    mCallBack.onMediaData(byteBuffer,bufferInfo,true,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
                }
            }
        }else if(mAudioDecoderHandle != 0 && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) != 0){
            if(mCallBack != null){
                mCallBack.onMediaData(data,info,true,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
            }
        }
        return 0;
    }

    @Override
    public int closeCodec() {
        if(mAudioDecoderHandle != 0){
            NativeMediaLib.audioDecoderCloseDecode(mAudioDecoderHandle);
            NativeMediaLib.audioDecoderDestory(mAudioDecoderHandle);
            mAudioDecoderHandle = 0;
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
}
