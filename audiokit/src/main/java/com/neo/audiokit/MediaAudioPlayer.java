package com.neo.audiokit;

import android.media.MediaPlayer;
import android.media.audiofx.EnvironmentalReverb;
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
    EnvironmentalReverb mEnvReverb;
    private List<String> reverbVals = new ArrayList<>();
    private List<Short> reverbNames = new ArrayList<>();

    public MediaAudioPlayer() {
        super();

        mPresetReverb = new PresetReverb(1, 0);
        mPresetReverb.setEnabled(true);


        mEqualizer = new Equalizer(0, getAudioSessionId());
        // 启用均衡控制效果
        mEqualizer.setEnabled(true);

//        mEnvReverb = new EnvironmentalReverb(0, getAudioSessionId());
//        mEnvReverb.setEnabled(true);

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
//            mPresetReverb.setPreset(reverbNames.get(idx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDecayTime(float ratio) {
//        mEnvReverb.setDecayTime((int) (100 + 19900 * ratio));
    }

    public void setRoomLevel(float ratio) {
//        short level = (short) (-9000 * ratio);
//        mEnvReverb.setRoomLevel(level);
//        level = (short) (11000 * ratio - 9000);
//
//        mEnvReverb.setReverbLevel(level);
//        mEnvReverb.setReverbDelay((int) (100 * ratio));
//
//        level = (short) (10000 * ratio - 9000);
//        mEnvReverb.setReflectionsLevel(level);
//        mEnvReverb.setReflectionsDelay((int) (300 * ratio));

        PresetReverb.Settings settings = mPresetReverb.getProperties();
        String str = settings.toString();
        settings = new PresetReverb.Settings(str);
        short preset = (ratio < 1.4f) ? PresetReverb.PRESET_NONE :
                (ratio < 2.8f) ? PresetReverb.PRESET_SMALLROOM :
                        (ratio < 4.2f) ? PresetReverb.PRESET_MEDIUMROOM :
                                (ratio < 5.6f) ? PresetReverb.PRESET_LARGEROOM :
                                        (ratio < 7f) ? PresetReverb.PRESET_MEDIUMHALL :
                                                (ratio < 8.4f) ? PresetReverb.PRESET_LARGEHALL :
                                                        PresetReverb.PRESET_PLATE;
        settings.preset = preset;
        mPresetReverb.setProperties(settings);
    }

    @Override
    public void release() {
        super.release();
        mEqualizer.release();
        mPresetReverb.release();
    }
}
