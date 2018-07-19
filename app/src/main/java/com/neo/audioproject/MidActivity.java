package com.neo.audioproject;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.lib.sox.SoxJni;
import com.neo.audiokit.midi.MidiNoteInfo;

import java.util.List;

public class MidActivity extends AppCompatActivity implements View.OnClickListener {
    private String midFile;
    private String wavFile;
    private String reverbFile;
    private List<MidiNoteInfo> processedSamples;
    private MediaPlayer mediaPlayer;
    private SeekBar sbReverbrance;
    private SeekBar sbRoomScale;
    private SeekBar sbDecay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mid);
        mediaPlayer = new MediaPlayer();
        findViewById(R.id.reverb).setOnClickListener(this);
        findViewById(R.id.origin).setOnClickListener(this);
        sbReverbrance = findViewById(R.id.seekbar_reverbrance);
        sbRoomScale = findViewById(R.id.seekbar_room_size);
        sbDecay = findViewById(R.id.seekbar_decay);

        prepareFile();
        reverbFile = getExternalFilesDir("wav").getAbsolutePath() + "/reverb.wav";
//
//        try {
//            MidiParser parser = new MidiParser(new File(midFile));
//            processedSamples = parser.generateNoteInfo();
//            Log.e("test", "size:" + processedSamples.size());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.reverb) {
            final AsyncTask task = new AsyncTask<Object, Void, Void>() {
                @Override
                protected Void doInBackground(Object... voids) {
                    SoxJni.addReverb(wavFile, reverbFile, sbReverbrance.getProgress(),
                            50, sbRoomScale.getProgress(),
                            100, sbDecay.getProgress(), 0);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Toast.makeText(MidActivity.this, "处理完成", Toast.LENGTH_SHORT).show();
                }
            };
            task.execute();
        } else if (v.getId() == R.id.origin) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(wavFile);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.reverb) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(reverbFile);
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
        mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    private void prepareFile() {
//        try {
//            midFile = getExternalFilesDir("midi").getAbsolutePath() + "/jingle_bells.mid";
//            FileUtils.copyFileFromAssets(this, "jingle_bells.mid", midFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            wavFile = getExternalFilesDir("wav").getAbsolutePath() + "/because_of_you.wav";
            FileUtils.copyFileFromAssets(this, "because_of_you.wav", wavFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
