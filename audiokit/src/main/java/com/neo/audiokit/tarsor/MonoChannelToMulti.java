package com.neo.audiokit.tarsor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class MonoChannelToMulti implements AudioProcessor {

    private int channels;

    public MonoChannelToMulti(int numberOfChannels) {
        channels = numberOfChannels;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] buffer = audioEvent.getFloatBuffer();
        float[] newBuffer = new float[buffer.length * channels];

        for (int i = 0; i < buffer.length; i++) {
            int dstI = i * channels;
            for (int j = 0; j < channels; j++) {
                newBuffer[dstI + j] = buffer[i];
            }
        }

        audioEvent.setFloatBuffer(newBuffer);
        return true;
    }

    @Override
    public void processingFinished() {
    }
}

