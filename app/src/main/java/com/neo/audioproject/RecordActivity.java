package com.neo.audioproject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.neo.audiokit.AudioRecorder;
import com.neo.audiokit.widget.AudioLyricView;
import com.neo.audiokit.widget.MidiView;

public class RecordActivity extends AppCompatActivity implements AudioLyricView.IPlayerCallback,
        AudioRecorder.IRecordCallback {
    private AudioLyricView lyricsView;
    private String musicPath;
    private String accomPath; // 伴奏
    private String lyricPath;
    private String midPath;
    private AudioRecorder audioRecorder;
    private MidiView midiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        lyricsView = findViewById(R.id.lyric_view);
        lyricsView.setPaintColor(new int[]{Color.WHITE, Color.WHITE});
        lyricsView.setPaintHLColor(new int[]{Color.RED, Color.RED});

        lyricsView.setCallback(this);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!audioRecorder.isRecording()) {
                    lyricsView.start();
                    midiView.reset();
                    audioRecorder.startRecord();
                }
            }
        });

        ((CheckBox) findViewById(R.id.origin)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                lyricsView.switchOriginAccom(b);
            }
        });

        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioRecorder.isRecording()) {
                    lyricsView.pause();
                    audioRecorder.stopRecord();
                    finishRecord();
                }
            }
        });


        prepareFile();

        midiView = (MidiView) findViewById(R.id.mid_view);
        midiView.loadMid(midPath);

        lyricsView.setDataSource(musicPath, accomPath, lyricPath);
        audioRecorder = new AudioRecorder(getExternalFilesDir("record").getAbsolutePath(), this);

    }

    private void prepareFile() {
        try {
            musicPath = getExternalFilesDir("ex").getAbsolutePath() + "/test.mp3";
            FileUtils.copyFileFromAssets(this, "test.mp3", musicPath);

            accomPath = getExternalFilesDir("ex").getAbsolutePath() + "/because_of_you.wav";
            FileUtils.copyFileFromAssets(this, "because_of_you.wav", accomPath);

            lyricPath = getExternalFilesDir("ex").getAbsolutePath() + "/aiqingyu_krc.krc";
            FileUtils.copyFileFromAssets(this, "aiqingyu_krc.krc", lyricPath);

            try {
                midPath = getExternalFilesDir("midi").getAbsolutePath() + "/tiger.mid";
                FileUtils.copyFileFromAssets(this, "tiger.mid", midPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    }

    @Override
    public void onPlayProgressChanged(long curTimeMs) {
        midiView.setProgress(curTimeMs);
    }

    @Override
    public void onCompletion() {
        audioRecorder.stopRecord();

        // TODO
        finishRecord();
    }

    private void finishRecord() {
        String recFile = audioRecorder.getFilePath();
        int score = midiView.getScore();
        Log.e("test", "score:" + score);
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
    public void onRecordStart() {

    }

    @Override
    public void onRecordProgress(long timeMs) {

    }

    @Override
    public void onRecordStop() {

    }

    @Override
    public void onDetectPitch(float pitchInHz) {
        if (midiView != null) {
            midiView.setPitch(pitchInHz);
        }
    }
}
