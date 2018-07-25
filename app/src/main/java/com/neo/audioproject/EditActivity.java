package com.neo.audioproject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.neo.audiokit.AudioPlayer;
import com.neo.audiokit.sox.ReverbBean;
import com.neo.audiokit.widget.waveform.view.WaveformView;


public class EditActivity extends AppCompatActivity {
    private String wavFile;
    private WaveformView waveformView;
    private AudioPlayer audioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave_form);
        prepareFile();

        waveformView = findViewById(R.id.waveform);
        waveformView.loadFile(wavFile);
        waveformView.setDefaultColor(Color.BLUE);

        audioPlayer = new AudioPlayer(null);
        audioPlayer.setDataSource(wavFile);
        ReverbBean reverbBean = new ReverbBean();
        reverbBean.reverberance = 50;
        reverbBean.hFDamping = 50;
        reverbBean.roomScale = 85;
        reverbBean.preDelay = 30;
        reverbBean.stereoDepth = 0;
        reverbBean.wetGain = 0;
        audioPlayer.setReverb(reverbBean);

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioPlayer.start();
            }
        });
    }

    private void prepareFile() {
        try {
            wavFile = getExternalFilesDir("ex").getAbsolutePath() + "/because_of_you.wav";
            FileUtils.copyFileFromAssets(this, "because_of_you.wav", wavFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayer.stop();
        audioPlayer.release();
    }
}
