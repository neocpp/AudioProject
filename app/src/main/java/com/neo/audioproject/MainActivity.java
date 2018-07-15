package com.neo.audioproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.neo.audiokit.AudioPlayer;
import com.neo.audiokit.MediaAudioPlayer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AudioPlayer.AudioPlayerCallBack {

    MediaAudioPlayer audioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_play).setOnClickListener(this);

        audioPlayer = new MediaAudioPlayer();
        try {
            audioPlayer.setDataSource("/sdcard/a.mp3");
            audioPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Spinner sp = findViewById(R.id.spinner);
        sp.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, audioPlayer.getReverbValues()));
        // 为Spinner的列表项选中事件设置监听器
        sp.setOnItemSelectedListener(new Spinner
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0
                    , View arg1, int arg2, long arg3) {
                // 设定音场
                audioPlayer.setReverb(arg2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_play) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
            } else {
                audioPlayer.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        audioPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onDuration(long fileDuration) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        audioPlayer.stop();
        audioPlayer.release();
    }
}
