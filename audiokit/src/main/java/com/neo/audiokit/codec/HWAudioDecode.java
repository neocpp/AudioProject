
package com.neo.audiokit.codec;

import java.nio.ByteBuffer;

public class HWAudioDecode extends BaseCodec {
    private String TAG = "HWAudioDecode";

    @Override
    protected int processMediaFormatChange(MediaFormat mediaFormat) {
        if (mMediaDataCallBack != null) {
            mMediaDataCallBack.onMediaFormatChange(mediaFormat, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
        return 0;
    }

    @Override
    protected int processOutBuffer(ByteBuffer byteBuffer, CodecBufferInfo info){
        if(mSurface != null|| byteBuffer == null){
            byteBuffer = null;
        }else {
            byteBuffer.position(info.offset);
            byteBuffer.limit(info.offset + info.size);
        }
        if(mMediaDataCallBack != null){
            mMediaDataCallBack.onMediaData(byteBuffer,info,true,IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }
       // LogUtils.d(TAG,"HWAudioDecode::processOutBuffer" + info.presentationTimeUs  + " " + info.size);
        return 0;
    }

    @Override
    public int sendEndFlag() {
        CodecBufferInfo info = new CodecBufferInfo();
        info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
        sendData(null,info);
        return 0;
    }

}
