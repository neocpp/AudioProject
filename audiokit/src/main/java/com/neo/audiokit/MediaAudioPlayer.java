package com.neo.audiokit;

import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boyo on 18/7/15.
 */

public class MediaAudioPlayer extends MediaPlayer {

    Equalizer mEqualizer;
    PresetReverb mPresetReverb;
    private List<String> reverbVals = new ArrayList<>();
    private List<Short> reverbNames = new ArrayList<>();

    public MediaAudioPlayer() {
        super();

        mEqualizer = new Equalizer(0, getAudioSessionId());
        // 启用均衡控制效果
        mEqualizer.setEnabled(true);

        mPresetReverb = new PresetReverb(0, getAudioSessionId());
        mPresetReverb.setEnabled(true);

        for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
            reverbNames.add(i);
            reverbVals.add(mEqualizer.getPresetName(i));
        }

    }

    public List<String> getReverbValues() {
        return reverbVals;
    }

    public void setReverb(int idx) {
        try {
            mEqualizer.usePreset(reverbNames.get(idx));
            mPresetReverb.setPreset(reverbNames.get(idx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        super.release();
        mEqualizer.release();
        mPresetReverb.release();
    }
}
