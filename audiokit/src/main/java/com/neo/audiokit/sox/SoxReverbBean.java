package com.neo.audiokit.sox;

public class SoxReverbBean {
    public int reverberance; // 0~100
    public int hFDamping; // 0~100
    public int roomScale; // 0~100
    public int stereoDepth;
    public int preDelay;
    public int wetGain;

    public boolean isUnset() {
        return (reverberance | hFDamping | roomScale | stereoDepth | preDelay | wetGain) != 0;
    }
}
