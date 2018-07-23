package com.neo.audioproject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;

import com.neo.audiokit.widget.AudioLyricView;

public class LyricPlayActivity extends AppCompatActivity implements AudioLyricView.IPlayerCallback, SeekBar.OnSeekBarChangeListener {
    private AudioLyricView lyricsView;
    private String musicPath;
    private String lyricPath;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric);

        lyricsView = findViewById(R.id.lyric_view);
        lyricsView.setPaintColor(new int[]{Color.WHITE, Color.WHITE});
        lyricsView.setPaintHLColor(new int[]{Color.RED, Color.RED});

        lyricsView.setCallback(this);
        lyricsView.setLooping(true);

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lyricsView.isPlaying()) {
                    lyricsView.pause();
                } else {
                    lyricsView.start();
                }
            }
        });

        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        prepareFile();

        lyricsView.setDataSource(musicPath, lyricPath);
    }

    private void prepareFile() {
        try {
            musicPath = getExternalFilesDir("ex").getAbsolutePath() + "/test.mp3";
            FileUtils.copyFileFromAssets(this, "test.mp3", musicPath);

            lyricPath = getExternalFilesDir("ex").getAbsolutePath() + "/aiqingyu_krc.krc";
            FileUtils.copyFileFromAssets(this, "aiqingyu_krc.krc", lyricPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        lyricsView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lyricsView.start();
    }

    @Override
    public void onPrepared() {
        seekBar.setMax((int) lyricsView.getDuration());
        lyricsView.start();

    }

    @Override
    public void onPlayProgressChanged(long curTimeMs) {
        seekBar.setProgress((int) curTimeMs);

    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onError() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lyricsView.release();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b) {
            lyricsView.seekto(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
