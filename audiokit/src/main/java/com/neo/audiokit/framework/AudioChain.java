package com.neo.audiokit.framework;

public abstract class AudioChain implements IAudioTarget {
    protected IAudioTarget mTarget;

    protected abstract boolean isActive();

    public void setAudioTarget(IAudioTarget target) {
        mTarget = target;
    }

    protected AudioFrame processData(AudioFrame audioFrame) {
        if (audioFrame.isRawData && isActive()) {
            return doProcessData(audioFrame);
        }
        return audioFrame;
    }

    protected abstract AudioFrame doProcessData(AudioFrame audioFrame);

    @Override
    public void newDataReady(AudioFrame audioFrame) {
        AudioFrame outFrame = processData(audioFrame);

        deliverData(outFrame);
    }

    protected void deliverData(AudioFrame audioFrame) {
        if (mTarget != null) {
            mTarget.newDataReady(audioFrame);
        }
    }

    public abstract void release();
}
