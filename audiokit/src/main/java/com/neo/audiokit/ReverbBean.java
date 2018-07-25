package com.neo.audiokit;

import android.media.audiofx.EnvironmentalReverb;

public class ReverbBean {
    public float roomLevel;
    public float roomHFLevel;
    public float decayTime;
    public float decayHFRatio;
    public float reflectionsLevel;
    public float reflectionsDelay;
    public float reverbLevel;
    public float reverbDelay;
    public float diffusion;
    public float density;

    private EnvironmentalReverb.Settings settings = new EnvironmentalReverb.Settings();

    public EnvironmentalReverb.Settings getReverbSettings() {
        settings.decayHFRatio = (short) (100 + 1900 * decayHFRatio);
        settings.decayTime = (short) (100 + 19900 * decayTime);
        settings.density = (short) (1000 * density);
        settings.diffusion = (short) (1000 * diffusion);
        settings.reflectionsDelay = (int) (300 * reflectionsDelay);
        settings.reflectionsLevel = (short) (1000 - 10000 * reflectionsLevel);
        settings.reverbDelay = (int) (100 * reverbDelay);
        settings.reverbLevel = (short) (2000 - 11000 * reverbLevel);
        settings.roomLevel = (short) (-9000 * roomLevel);
        settings.roomHFLevel = (short) (-9000 * roomHFLevel);

        return settings;
    }
}
