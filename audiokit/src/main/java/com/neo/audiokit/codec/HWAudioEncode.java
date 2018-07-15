
package com.neo.audiokit.codec;


import android.annotation.SuppressLint;

import java.nio.ByteBuffer;

public class HWAudioEncode extends BaseCodec {
    private String TAG = "HWAudioEncode";

    @Override
    protected int processMediaFormatChange(MediaFormat mediaFormat) {
        if (mMediaDataCallBack != null) {
            mMediaDataCallBack.onMediaFormatChange(mediaFormat, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
        return 0;
    }

    @Override
    protected int processOutBuffer(ByteBuffer byteBuffer, CodecBufferInfo info){

        byteBuffer.position(info.offset);
        byteBuffer.limit(info.offset + info.size);
        if(mMediaDataCallBack != null ){
            info.decodeTimeUs = info.presentationTimeUs;
            mMediaDataCallBack.onMediaData(byteBuffer,info,false,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
       // LogUtils.d(TAG,"HWVideoEncode::processOutBuffer" + info.presentationTimeUs + " " + info.size);
        return 0;
    }

    @Override
    public int sendEndFlag() {
        if(mSurface == null){
            CodecBufferInfo info = new CodecBufferInfo();
            info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
            sendData(null,info);
        }
        return 0;
    }

    @SuppressLint("NewApi")
    @Override
    public int closeCodec(){
        if(mSurface != null){
            mMediaCodec.signalEndOfInputStream();;
        }else {
            CodecBufferInfo info = new CodecBufferInfo();
            info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
            sendData(null,info);
        }
        super.closeCodec();
        return 0;
    }
}
