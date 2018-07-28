package com.lib.sox;

public class SoxJni {
    static {
        System.loadLibrary("sox-lib");
    }

    public static native void addReverb(String inFile, String outFile, int reverberance, int hFDamping, int roomScale, int stereoDepth,
                                        int preDelay, int wetGain);

    public static native int processBuffer(byte[] inData, int size, byte[] outData, int sampleRate, int channels,
                                           int reverberance, int hFDamping, int roomScale, int stereoDepth,
                                           int preDelay, int wetGain);
}
