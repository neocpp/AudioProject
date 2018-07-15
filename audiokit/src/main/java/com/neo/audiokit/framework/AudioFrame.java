package com.neo.audiokit.framework;

import com.neo.audiokit.codec.CodecBufferInfo;

import java.nio.ByteBuffer;

public class AudioFrame {
    public ByteBuffer buffer;
    public CodecBufferInfo info;
    public boolean isRawData;
}
