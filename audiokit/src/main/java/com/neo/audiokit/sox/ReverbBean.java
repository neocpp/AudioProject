package com.neo.audiokit.sox;

public class ReverbBean {
    public int reverberance;
    public int hFDamping;
    public int roomScale;
    public int stereoDepth;
    public int preDelay;
    public int wetGain;

    public boolean isUnset() {
        return (reverberance | hFDamping | roomScale | stereoDepth | preDelay | wetGain) != 0;
    }
}
