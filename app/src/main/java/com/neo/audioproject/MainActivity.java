package com.neo.audioproject;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.neo.audiokit.AudioPlayer;
import com.neo.audiokit.AudioRecorder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AudioPlayer.AudioPlayerCallBack, SeekBar.OnSeekBarChangeListener, AudioRecorder.IRecordCallback {

    AudioPlayer audioPlayer;
    AudioRecorder audioRecorder;
    Button mBtnRec;
    TextView mTxtRec;
    String musicPath;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_play).setOnClickListener(this);
        mBtnRec = findViewById(R.id.btn_rec);
        mBtnRec.setOnClickListener(this);
        mTxtRec = findViewById(R.id.txt_rec_time);
        findViewById(R.id.btn_play_rec).setOnClickListener(this);

        audioPlayer = new AudioPlayer(this, null);

        Spinner sp = findViewById(R.id.spinner);
        sp.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, audioPlayer.getPresetValues()));
        // 为Spinner的列表项选中事件设置监听器
        sp.setOnItemSelectedListener(new Spinner
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0
                    , View arg1, int arg2, long arg3) {
                // 设定音场
//                audioPlayer.setReverb(arg2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        SeekBar seekbar = findViewById(R.id.seekbar_decay);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_room_level);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_pitch);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar = findViewById(R.id.seekbar_volume);
        seekbar.setOnSeekBarChangeListener(this);

        audioRecorder = new AudioRecorder(getExternalFilesDir("record").getAbsolutePath(), this);

        prepareFile();

    }

    private void prepareFile() {
        try {
            musicPath = getExternalFilesDir("ex").getAbsolutePath() + "/test.mp3";
            FileUtils.copyFileFromAssets(this, "test.mp3", musicPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_play) {
            audioPlayer.stop();
            audioPlayer.setDataSource(musicPath);
            audioPlayer.start();

//            if (audioPlayer.isPlaying()) {
//                audioPlayer.pause();
//            } else {
//                audioPlayer.start();
//            }
        } else if (view.getId() == R.id.btn_rec) {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecord();
                mBtnRec.setText("开始录制");
            } else {
                audioRecorder.startRecord();
                mBtnRec.setText("停止录制");
            }
        } else if (view.getId() == R.id.btn_play_rec) {
//            try {
//                audioPlayer.stop();
//                audioPlayer.setDataSource(audioRecorder.getFilePath());
//                audioPlayer.start();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(audioRecorder.getFilePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioPlayer.pause();
//        audioRecorder.stopCapture();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        audioRecorder.startCapture();
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onPrepared(AudioPlayer audioPlayer) {

    }

    @Override
    public void onPositionChanged(long timeMs) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        audioPlayer.stop();
        audioPlayer.release();

        audioRecorder.release();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekbar_decay:
//                audioPlayer.setDecayTime(progress / 100f);
                break;
            case R.id.seekbar_room_level:
//                audioPlayer.setRoomLevel(progress / 100f);
                break;
            case R.id.seekbar_pitch:
                float pitch = 0.5f + 1.3f * progress / 100f;
                audioPlayer.setPitch(pitch);
                break;
            case R.id.seekbar_volume:
                audioPlayer.setVolume(progress / 200f);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onRecordStart() {

    }

    @Override
    public void onRecordProgress(final long timeMs) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTxtRec.setText("录制时间:" + (timeMs / 1000f) + "秒");
            }
        });
    }

    @Override
    public void onRecordStop() {

    }

    @Override
    public void onDetectPitch(float pitchInHz) {

    }
}
