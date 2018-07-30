package com.neo.audioproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;

import com.neo.audiokit.AudioPlayerNew;

public class PlayTestActivity extends AppCompatActivity implements AudioPlayerNew.AudioPlayerCallback,
        SeekBar.OnSeekBarChangeListener {

    private AudioPlayerNew audioPlayerNew;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_test);

        audioPlayerNew = new AudioPlayerNew(this);
        audioPlayerNew.setPlayCallback(this);

        String musicPath = getExternalFilesDir("ex").getAbsolutePath() + "/test.mp3";
        audioPlayerNew.setDataSource(musicPath);
        audioPlayerNew.prepareAsyn();

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioPlayerNew.isPlaying()) {
                    audioPlayerNew.pause();
                } else {
                    audioPlayerNew.start();
                }
            }
        });

        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayerNew.stop();
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onPrepared() {
        seekBar.setMax((int) audioPlayerNew.getDuration());
    }

    @Override
    public void onPositionChanged(long timeMs) {
        seekBar.setProgress((int) timeMs);
    }

    @Override
    public void onError(int code, String msg) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b) {
            audioPlayerNew.seekTo(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
