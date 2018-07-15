package com.lib.sox;

public class SoxJni {
    static {
        System.loadLibrary("sox-lib");
    }

    public native String getString();
}
