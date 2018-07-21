package com.neo.audiokit.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.AttributeSet;

import com.zlm.hp.lyrics.LyricsReader;
import com.zlm.hp.lyrics.widget.AbstractLrcView;
import com.zlm.hp.lyrics.widget.ManyLyricsView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by boyo on 18/7/21.
 */

public class AudioLyricView extends ManyLyricsView implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private MediaPlayer audioPlayer;
    private boolean isLooping = false;
    private IPlayerCallback callback;

    private boolean isPlayerReady, isLyricReady;

    public AudioLyricView(Context context) {
        super(context);
        init();
    }

    public AudioLyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        audioPlayer = new MediaPlayer();
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnCompletionListener(this);
        audioPlayer.setOnErrorListener(this);
    }

    public void setCallback(IPlayerCallback callback) {
        this.callback = callback;
    }

    public void setDataSource(String audioPath, String lyricPath) {
        loadLyric(lyricPath);
        try {
            audioPlayer.setDataSource(audioPath);
            audioPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLyric(final String path) {
        new AsyncTask<String, Integer, Boolean>() {

            @Override
            protected Boolean doInBackground(String... strings) {
                try {
                    String lyricPath = strings[0];
                    File lyricFile = new File(lyricPath);
                    LyricsReader lyricsReader = new LyricsReader();
                    lyricsReader.loadLrc(lyricFile);
                    setLyricsReader(lyricsReader);
                    //
                    setExtraLrcStatus(AbstractLrcView.EXTRALRCSTATUS_NOSHOWEXTRALRC);
                } catch (Exception e) {
                    setLrcStatus(AbstractLrcView.LRCSTATUS_ERROR);
                    e.printStackTrace();

                    return false;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean ret) {
                super.onPostExecute(ret);
                isLyricReady = ret;
                if (isPlayerReady && isLyricReady) {
                    if (callback != null) {
                        callback.onPrepared();
                    }
                }

                if (!isLyricReady) {
                    if (callback != null) {
                        callback.onError();
                    }
                }
            }
        }.execute(path);
    }

    public void start() {
        if (isPlayerReady && isLyricReady && !audioPlayer.isPlaying()) {
            audioPlayer.start();
            play();
        }
    }

    public void seekto(int timeMs) {
        super.seekto(timeMs);
        audioPlayer.seekTo(timeMs);
    }

    public boolean isPlaying() {
        return audioPlayer.isPlaying();
    }

    public void pause() {
        if (audioPlayer.isPlaying()) {
            super.pause();
            audioPlayer.pause();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (isLooping) {
            audioPlayer.seekTo(0);
            audioPlayer.start();
            play(0);
        }

        if (callback != null) {
            callback.onCompletion();
        }
    }

    public void setLooping(boolean loop) {
        isLooping = loop;
    }

    @Override
    protected void updateView(long playProgress) {
        super.updateView(playProgress);
        if (callback != null) {
            callback.onPlayProgressChanged(playProgress);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        if (callback != null) {
            callback.onError();
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        isPlayerReady = true;
        if (isPlayerReady && isLyricReady) {
            if (callback != null) {
                callback.onPrepared();
            }
        }
    }

    public long getDuration() {
        return audioPlayer.getDuration();
    }

    public void reset() {
        audioPlayer.reset();
        resetData();
        isPlayerReady = false;
        isLyricReady = false;
    }

    public interface IPlayerCallback {
        void onPrepared();

        void onPlayProgressChanged(long curTimeMs);

        void onCompletion();

        void onError();
    }

    public void release() {
        audioPlayer.stop();
        audioPlayer.release();
    }
}
