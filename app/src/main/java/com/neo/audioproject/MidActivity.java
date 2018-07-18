package com.neo.audioproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.neo.audiokit.midi.MidiNoteInfo;
import com.neo.audiokit.midi.MidiParser;

import java.io.File;
import java.util.List;

public class MidActivity extends AppCompatActivity {
    private String midFile;
    private List<MidiNoteInfo> processedSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mid);

        prepareFile();

        try {
            MidiParser parser = new MidiParser(new File(midFile));
            processedSamples = parser.generateNoteInfo();
            Log.e("test", "size:" + processedSamples.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareFile() {
        try {
            midFile = getExternalFilesDir("midi").getAbsolutePath() + "/jingle_bells.mid";
            FileUtils.copyFileFromAssets(this, "jingle_bells.mid", midFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
