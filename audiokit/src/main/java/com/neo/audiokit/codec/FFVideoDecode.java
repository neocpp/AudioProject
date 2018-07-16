
package com.neo.audiokit.codec;

import android.view.Surface;

import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;

public class FFVideoDecode implements ICodec {
    private String TAG = "FFVideoDecode";
    private final static int DEF_PIXEL_FORMAT_YUV420SP = 1;
    private final static int DEF_PIXEL_FORMAT_YUV420P = 2;
    private final static int DEF_PIXEL_FORMAT_RGBA888 = 3;
    private long mVideoDecodeHandle;
    private IMediaDataCallBack mCallBack;
    @Override
    public int openCodec(String mimeType, MediaFormat format, Surface surface, boolean isEncode) {
        if(mVideoDecodeHandle == 0){
            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            mVideoDecodeHandle = NativeMediaLib.videoDecoderCreate();
            NativeMediaLib.videoDecoderOpenDecode(mVideoDecodeHandle,width,height,DEF_PIXEL_FORMAT_RGBA888,surface);
            if(format.containsKey("csd-0")){
                ByteBuffer byteBuffer = format.getByteBuffer("csd-0");
                byte[]data = new byte[byteBuffer.limit()];
                byteBuffer.get(data);
                NativeMediaLib.videoDecoderSetSPS(mVideoDecodeHandle,data,data.length);
            }
            if(format.containsKey("csd-1")){
                ByteBuffer byteBuffer = format.getByteBuffer("csd-1");
                byte[]data = new byte[byteBuffer.limit()];
                byteBuffer.get(data);
                NativeMediaLib.videoDecoderSetPPS(mVideoDecodeHandle,data,data.length);
            }
//            if(mCallBack != null){
//                mCallBack.onMediaFormatChange(format,IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo);
//            }
        }
        return 0;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int sendData(ByteBuffer data, CodecBufferInfo info) {
        if(mVideoDecodeHandle != 0 && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0){
            NativeMediaLib.videoDecoderDecodeFrame(mVideoDecodeHandle,FFVideoDecode.this,data,info.size,info.presentationTimeUs);
        }
        return 0;
    }

    @Override
    public int closeCodec() {
        if(mVideoDecodeHandle != 0){
            NativeMediaLib.videoDecoderCloseDecode(mVideoDecodeHandle,FFVideoDecode.this);
            NativeMediaLib.videoDecoderDestory(mVideoDecodeHandle);
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
            mCallBack.onMediaData(byteBuffer,bufferInfo,true,IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo);
        }
        return 0;
    }
}
