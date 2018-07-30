package com.neo.audiokit;

import com.neo.audiokit.sox.SoxReverbBean;

public class ReverbBean {
    public float reverbLevel;
    public float reverbDelay;
    public float roomLevel;
    public float roomHFLevel;

    private SoxReverbBean soxReverbBean = new SoxReverbBean();

    public SoxReverbBean getSoxReverbBean() {
        soxReverbBean.roomScale = (int) (roomLevel * 100);
        soxReverbBean.hFDamping = (int) (roomHFLevel * 100);
        soxReverbBean.preDelay = (int) (100 * reverbDelay);
        soxReverbBean.stereoDepth = 0;
        soxReverbBean.reverberance = (int) (reverbLevel * 100);
        soxReverbBean.wetGain = 0;

        return soxReverbBean;
    }
}
