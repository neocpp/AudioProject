package com.neo.audiokit.exo;


import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

import java.nio.ByteBuffer;

public class SonicExoHandler extends AudioChain {
    private static final String TAG = "SonicExoHandler";

    private SonicAudioProcessor sonicAudioProcessor;
    protected float mSpeed;
    protected float mPitch;

    public SonicExoHandler(float speed, float pitch) {
        mSpeed = speed;
        mPitch = pitch;
    }

    public long init(int sampleRate, int channelNum) {
        sonicAudioProcessor = new SonicAudioProcessor();
        try {
            sonicAudioProcessor.configure(sampleRate, channelNum, C.ENCODING_PCM_16BIT);
            sonicAudioProcessor.setSpeed(mSpeed);
            sonicAudioProcessor.setPitch(mPitch);

            sonicAudioProcessor.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public void setSpeedPitch(float speed, float pitch) {
        if (mSpeed != speed || mPitch != pitch) {
            mSpeed = speed;
            mPitch = pitch;
            sonicAudioProcessor.setSpeed(speed);
            sonicAudioProcessor.setPitch(pitch);
            sonicAudioProcessor.flush();
        }
    }

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        if (audioFrame.isRawData) {
            sonicAudioProcessor.queueInput(audioFrame.buffer);
            ByteBuffer output = sonicAudioProcessor.getOutput();
            audioFrame.buffer = output;
            audioFrame.info.offset = 0;
            audioFrame.buffer.position(0);
            audioFrame.info.size = output.limit();
        }

        return audioFrame;
    }

    @Override
    protected boolean isActive() {
        return sonicAudioProcessor.isActive();
    }

    @Override
    public void release() {
        sonicAudioProcessor.reset();
    }

}
