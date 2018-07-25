package com.neo.audiokit;

import android.media.MediaPlayer;
import android.text.TextUtils;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

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

    public AudioEffectPlayManager(String recPath, String musicPath) {
        recPlayer = new AudioPlayer(this);
        recPlayer.setDataSource(recPath);

        if (!TextUtils.isEmpty(musicPath)) {
            musicPlayer = new MediaPlayer();
            try {
                musicPlayer.setDataSource(musicPath);
                musicPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.recPath = recPath;
        this.musicPath = musicPath;
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
            musicPlayer.stop();
        }
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onDuration(long fileDuration) {

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
//            if (Math.abs(pitch - 1) > .02) {
//                String tmpF = outf.getParent() + "/pitch.aac";
//                AudioFileReader audioFileReader = new AudioFileReader();
//                audioFileReader.openReader(recPath, Long.MIN_VALUE, Long.MAX_VALUE, AudioEffectPlayManager.this);
//                SonicExoHandler mSonicHandler = new SonicExoHandler(1f, pitch);
//                mSonicHandler.init(audioFileReader.getSampleRate(), audioFileReader.getChannelNum());
//                setAudioTarget(mSonicHandler);
//                FileWriter fileWriter = new FileWriter();
//                mSonicHandler.setAudioTarget(fileWriter);
//                fileWriter.startRecord(tmpF);
//                audioFileReader.start();
//                audioFileReader.closeReader();
//                fileWriter.stopRecord();
//                recPath = tmpF;
//            }


//            if (reverbBean != null) {
//                String tmpF = outf.getParent() + "/mux.aac";
//                MediaMux.mux(recPath, 0, musicPath, 0, tmpF, recVolume, musicVolume);
//                SoxJni.addReverb(tmpF, outFile,
//                        (int) (reverbBean.reflectionsLevel * 100),
//                        50,
//                        (int) (reverbBean.roomLevel * 100),
//                        0,
//                        (int) (reverbBean.decayTime * 100),
//                        0
//                );
//            } else {
                MediaMux.mux(recPath, 0, musicPath, 0, outFile, recVolume, musicVolume);
//            }

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
}
