package com.neo.audiokit.sox;

import com.lib.sox.SoxJni;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoxHandler extends AudioChain {
    private int mSampleRate;
    private int mChannels;
    private ReverbBean reverbBean;

    public void init(int sampleRate, int channels) {
        mSampleRate = sampleRate;
        mChannels = channels;
    }

    @Override
    protected boolean isActive() {
        return reverbBean == null ? false : reverbBean.isUnset();
    }

    public void setParam(ReverbBean bean) {
        reverbBean = bean;
    }

    private ByteBuffer cacheBuffer;

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        byte[] bytedata = new byte[audioFrame.info.size];
        audioFrame.buffer.position(audioFrame.info.offset);
        audioFrame.buffer.get(bytedata);
        byte[] outBuffer = new byte[bytedata.length * 2];
        int outSize = SoxJni.processBuffer(bytedata, bytedata.length, outBuffer, mSampleRate, mChannels,
                reverbBean.reverberance, reverbBean.hFDamping, reverbBean.roomScale, reverbBean.stereoDepth,
                reverbBean.preDelay, reverbBean.wetGain);
        if (cacheBuffer == null || cacheBuffer.capacity() < outSize) {
            cacheBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder());
        }

        cacheBuffer.clear();
        cacheBuffer.put(outBuffer, 0, outSize);
        cacheBuffer.position(0);
        cacheBuffer.limit(outSize);
        audioFrame.buffer = cacheBuffer;
        audioFrame.info.offset = 0;
        audioFrame.info.size = outSize;

        return audioFrame;
    }

    @Override
    public void release() {

    }
}
