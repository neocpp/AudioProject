package com.neo.audioproject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zlm.hp.lyrics.LyricsReader;
import com.zlm.hp.lyrics.widget.AbstractLrcView;
import com.zlm.hp.lyrics.widget.ManyLyricsView;

import java.io.InputStream;

public class LyricActivity extends AppCompatActivity {
    private ManyLyricsView lyricsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric);

        lyricsView = findViewById(R.id.lyric_view);
        loadLrcFile();
    }

    private void loadLrcFile() {

        new AsyncTask<String, Integer, String>() {

            @Override
            protected String doInBackground(String... strings) {
                InputStream inputStream = getResources().openRawResource(R.raw.aiqingyu_krc);
                try {
                    //延迟看一下加载效果
                    Thread.sleep(500);

                    LyricsReader lyricsReader = new LyricsReader();
                    byte[] data = new byte[inputStream.available()];
                    inputStream.read(data);
                    lyricsReader.loadLrc(data, null, "aiqingyu_krc.krc");
                    lyricsView.setLyricsReader(lyricsReader);
                    //
                    lyricsView.setExtraLrcStatus(AbstractLrcView.EXTRALRCSTATUS_NOSHOWEXTRALRC);
                    lyricsView.play(0);

                    inputStream.close();
                } catch (Exception e) {
                    lyricsView.setLrcStatus(AbstractLrcView.LRCSTATUS_ERROR);
                    e.printStackTrace();
                }
                inputStream = null;

                return null;
            }
        }.execute("");
    }
}
