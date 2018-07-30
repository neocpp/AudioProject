package com.neo.audioproject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.neo.audiokit.AudioEffectPlayManager;
import com.neo.audiokit.ReverbBean;
import com.neo.audiokit.widget.waveform.view.WaveformView;


public class EditActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AudioEffectPlayManager.IPlayListener {
    private String wavFile;
    private String outFile;
    private String musicPath;
    private WaveformView waveformView;
    private ReverbBean reverbBean;
    private AudioEffectPlayManager playManager;
    private SeekBar playSeekbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        prepareFile();

        waveformView = findViewById(R.id.waveform);
        waveformView.loadFile(wavFile);
        waveformView.setDefaultColor(Color.BLUE);

        reverbBean = new ReverbBean();

        playManager = new AudioEffectPlayManager(this, wavFile, musicPath);
        playManager.setIPlayListener(this);
        playManager.setReverb(reverbBean);

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playManager.isPlaying()) {
                    playManager.pause();
                } else {
                    playManager.start();
                }
            }
        });

        SeekBar seekbar = findViewById(R.id.seekbar_room_level);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_roomhf_level);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_reverb_decay);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_reverb_level);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_pitch);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_rec_volume);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_music_volume);
        seekbar.setOnSeekBarChangeListener(this);

        playSeekbar = findViewById(R.id.seekbar);
        playSeekbar.setOnSeekBarChangeListener(this);
        playSeekbar.setMax((int) playManager.getDuration());

        findViewById(R.id.btn_compose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playManager.pause();
                playManager.composeFile(outFile, new AudioEffectPlayManager.IComposeCallback() {
                    @Override
                    public void onComposeFinish(boolean success, String outPath) {
                        Log.e("test", "compose finish");
                    }
                });
            }
        });
    }

    @Override
    public void onPlayProgressChanged(long timeMs) {
        playSeekbar.setProgress((int) timeMs);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float progressf = progress / 100f;
        switch (seekBar.getId()) {
            case R.id.seekbar_room_level:
                reverbBean.roomLevel = progressf;
                playManager.setReverb(reverbBean);
                break;
            case R.id.seekbar_roomhf_level:
                reverbBean.roomHFLevel = progressf;
                playManager.setReverb(reverbBean);
                break;
            case R.id.seekbar_reverb_level:
                reverbBean.reverbLevel = progressf;
                playManager.setReverb(reverbBean);
                break;
            case R.id.seekbar_reverb_decay:
                reverbBean.reverbDelay = progressf;
                playManager.setReverb(reverbBean);
                break;
            case R.id.seekbar_pitch:
                playManager.setPitch(progressf);
                break;
            case R.id.seekbar_rec_volume:
                playManager.setRecVolume(progressf);
                break;
            case R.id.seekbar_music_volume:
                playManager.setMusicVolume(progressf);
                break;
            case R.id.seekbar:
                if (fromUser) {
                    playManager.seekTo(progress);
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void prepareFile() {
        try {
            outFile = getExternalFilesDir("ex").getAbsolutePath() + "/out.wav";
            wavFile = getExternalFilesDir("record").getAbsolutePath() + "/rec.wav";
//            FileUtils.copyFileFromAssets(this, "because_of_you.wav", wavFile);
//            wavFile = getExternalFilesDir("record").getAbsolutePath() + "/rec.aac";
            musicPath = getExternalFilesDir("ex").getAbsolutePath() + "/test.mp3";
            FileUtils.copyFileFromAssets(this, "test.mp3", musicPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playManager.stop();
    }


}
