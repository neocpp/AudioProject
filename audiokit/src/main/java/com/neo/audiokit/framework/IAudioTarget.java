package com.neo.audiokit.framework;

public interface IAudioTarget {
    void newDataReady(AudioFrame audioFrame);
}
