package com.neo.audiokit;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.lib.sox.SoxJni;
import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.exo.SonicExoHandler;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.io.WavWriter;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class AudioEffectPlayManager extends AudioChain implements AudioPlayer.AudioPlayerCallBack, IMediaDataCallBack {
    private AudioPlayer recPlayer; // 人声
    private MediaPlayer musicPlayer; // 伴奏
    private float recVolume = 1f;
    private float musicVolume = 1f;
    private float pitch = 1f;
    private String recPath;
    private String musicPath;
    private ReverbBean reverbBean;
    private IPlayListener playListener;
    private Handler mHandler;

    public AudioEffectPlayManager(Context context, String recPath, String musicPath) {
        recPlayer = new AudioPlayer(context, this);
        recPlayer.setDataSource(recPath);
        recPlayer.prepare();

        mHandler = new Handler(Looper.getMainLooper());

        if (!TextUtils.isEmpty(musicPath)) {
            musicPlayer = new MediaPlayer();
            try {
                musicPlayer.setDataSource(musicPath);
                musicPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.recPath = recPath;
        this.musicPath = musicPath;
    }

    public void setIPlayListener(IPlayListener l) {
        playListener = l;
    }

    public long getDuration() {
        return recPlayer.getDuration();
    }

    public List<String> getPresetValues() {
        return recPlayer.getPresetValues();
    }

    public void setReverb(ReverbBean bean) {
        reverbBean = bean;
        recPlayer.setReverb(bean);
    }

    public void setPreset(int idx) {
        recPlayer.setPreset(idx);
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        recPlayer.setPitch(.5f + pitch);
    }

    public void setRecVolume(float volume) {
        recVolume = volume;
        recPlayer.setVolume(volume);
    }

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        musicPlayer.setVolume(volume, volume);
    }

    public void start() {
        recPlayer.start();
        if (musicPlayer != null) {
            musicPlayer.start();
        }
    }

    public boolean isPlaying() {
        return recPlayer.isPlaying();
    }

    public void pause() {
        recPlayer.pause();
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
    }

    public void seekTo(long timeMs) {
        recPlayer.seekTo(timeMs);
        if (musicPlayer != null) {
            musicPlayer.seekTo((int) timeMs);
        }
    }

    public void stop() {
        recPlayer.stop();
        if (musicPlayer != null) {
            musicPlayer.pause();
            musicPlayer.stop();
            musicPlayer.release();
        }
    }

    @Override
    public void onCompletion() {
        recPlayer.start();
        if (musicPlayer != null) {
            musicPlayer.seekTo(0);
            musicPlayer.start();
        }
    }

    @Override
    public void onPrepared(AudioPlayer audioPlayer) {

    }

    @Override
    public void onPositionChanged(final long timeMs) {
        if (playListener == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (playListener != null) {
                    playListener.onPlayProgressChanged(timeMs);
                }
            }
        });
    }

    private Thread composeThread;
    private String outFile;
    private IComposeCallback composeCallback;

    public void composeFile(String outFile, IComposeCallback callback) {
        if (composeThread == null) {
            this.outFile = outFile;
            composeCallback = callback;
            composeThread = new Thread(composeRun);
            composeThread.start();
        }
    }

    private Runnable composeRun = new Runnable() {
        @Override
        public void run() {
            File outf = new File(outFile);
            if (Math.abs(pitch - 1) > .02) {
                String tmpF = outf.getParent() + "/pitch.wav";
                AudioFileReader audioFileReader = new AudioFileReader();
                audioFileReader.openReader(recPath, Long.MIN_VALUE, Long.MAX_VALUE, AudioEffectPlayManager.this);
                SonicExoHandler mSonicHandler = new SonicExoHandler(1f, pitch);
                mSonicHandler.init(audioFileReader.getSampleRate(), audioFileReader.getChannelNum());
                setAudioTarget(mSonicHandler);
                WavWriter fileWriter = new WavWriter();
                fileWriter.setAudioParma(audioFileReader.getSampleRate(), audioFileReader.getChannelNum());
                fileWriter.startRecord(tmpF);
                mSonicHandler.setAudioTarget(fileWriter);
                audioFileReader.start();
                audioFileReader.closeReader();
                fileWriter.stopRecord();
                recPath = tmpF;
            }


            if (reverbBean != null) {
                String tmpF = outf.getParent() + "/mux.wav";
                MediaMux.mux(recPath, 0, musicPath, 0, tmpF, recVolume, musicVolume);
                SoxJni.addReverb(tmpF, outFile,
                        (int) (reverbBean.reverbLevel * 100),
                        (int) (reverbBean.roomHFLevel * 100),
                        (int) (reverbBean.roomLevel * 100),
                        0,
                        (int) (reverbBean.decayTime * 100),
                        0
                );
            } else {
                MediaMux.mux(recPath, 0, musicPath, 0, outFile, recVolume, musicVolume);
            }

            if (composeCallback != null) {
                composeCallback.onComposeFinish(true, outFile);
            }
        }
    };

    @Override
    public void onMediaFormatChange(MediaFormat format, int trackType) {

    }

    private AudioFrame audioFrame = new AudioFrame();

    @Override
    public int onMediaData(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType) {
        if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio && isRawData && (info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0) {
            audioFrame.buffer = mediaData;
            audioFrame.info = info;
            audioFrame.isRawData = isRawData;

            newDataReady(audioFrame);
        }
        return 0;
    }

    @Override
    protected boolean isActive() {
        return true;
    }

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        return audioFrame;
    }

    @Override
    public void release() {
        recPlayer.stop();
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.release();
        }
    }

    public interface IComposeCallback {

        void onComposeFinish(boolean success, String outPath);
    }

    public interface IPlayListener {
        void onPlayProgressChanged(long timeMs);
    }
}
