
package com.neo.audiokit.codec;

import android.view.Surface;

import java.nio.ByteBuffer;

public interface ICodec {
    int openCodec(String mimeType, MediaFormat format, Surface surface, boolean isEncode);
    int start();
    int sendData(ByteBuffer data, CodecBufferInfo info);
    int closeCodec();
    int sendEndFlag();
    int foreEndThread();
    int setCallBack(IMediaDataCallBack callBack);
    Surface getInputSurface();
}
