
package com.neo.audiokit.codec;

import java.nio.ByteBuffer;

public interface IMediaDataCallBack {
    final static int IMediaDataCallBackTrackTypeAudio = 1;
    final static int IMediaDataCallBackTrackTypeVideo = 2;
    void onMediaFormatChange(MediaFormat format, int trackType);
    int onMediaData(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType);
}
