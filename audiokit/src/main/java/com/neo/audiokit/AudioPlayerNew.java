package com.neo.audiokit;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.exo.SonicExoHandler;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.framework.IAudioTarget;
import com.neo.audiokit.sox.SoxHandler;
import com.neo.audiokit.sox.SoxReverbBean;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AudioPlayerNew extends AudioChain implements IMediaDataCallBack, Handler.Callback {

    private static final int MSG_START = 11;
    private static final int MSG_PAUSE = 12;
    private static final int MSG_STOP = 13;
    private static final int MSG_READ = 14;
    private static final int MSG_PREPARE = 15;
    private static final int MSG_SEEK = 16;

    private WeakReference<Context> mContextRef;
    private AudioFileReader mAudioReader;
    private String mPath;
    private long mAudioDurationMs;
    private Handler mExecuteHandler;
    private Handler mMainHandler;
    private AudioTrack mAudioTrack;
    private AudioPlayerCallback mPlayCallback;

    private SonicExoHandler mSonicHandler;
    private SoxHandler soxHandler;
    private float mPlaySpeed = 1.0f;
    private float mPlayPitch = 1f;
    private long mCurrentPlayTime = Long.MIN_VALUE;

    private SoxReverbBean soxReverbBean;
    private AudioFrame audioFrame = new AudioFrame();

    private enum STATUS {
        INITIAL, PREPARED, STARTED, PAUSED, STOPED, ERROR
    }

    private STATUS mStatus = STATUS.INITIAL;

    public AudioPlayerNew(Context context) {
        mContextRef = new WeakReference<>(context);

        HandlerThread handlerThread = new HandlerThread("play_thread");
        handlerThread.start();
        mExecuteHandler = new Handler(handlerThread.getLooper(), this);

        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void setPlayCallback(AudioPlayerCallback callback) {
        mPlayCallback = callback;
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void prepareAsyn() {
        mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_PREPARE));
    }

    public void start() {
        mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_START));
    }

    public void pause() {
        mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_PAUSE));
    }

    public void stop() {
        mExecuteHandler.removeCallbacksAndMessages(null);
        mStatus = STATUS.STOPED;
        mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_STOP));
    }

    public void seekTo(long timeMs) {
        mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_SEEK, timeMs));
    }

    public boolean isPlaying() {
        return mStatus == STATUS.STARTED;
    }

    public void setPitch(float pitch) {
        mPlayPitch = pitch;
        if (mSonicHandler != null) {
            mSonicHandler.setSpeedPitch(mPlaySpeed, mPlayPitch);
        }
    }

    public void setReverb(SoxReverbBean bean) {
        soxReverbBean = bean;
        if (soxHandler != null) {
            soxHandler.setParam(bean);
        }
    }

    public long getDuration() {
        return mAudioDurationMs;
    }

    public long getCurrentPlayTime() {
        return mCurrentPlayTime;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_PREPARE:
                handlePrepare();
                break;
            case MSG_START:
                handleStart();
                break;
            case MSG_READ:
                handleRead();
                break;
            case MSG_SEEK:
                mExecuteHandler.removeMessages(MSG_READ);
                long timeMs = (Long) msg.obj;
                mAudioTrack.flush();
                mAudioReader.seekTo(timeMs * 1000);
                mCurrentPlayTime = timeMs;
                break;
            case MSG_PAUSE:
                if (mStatus == STATUS.STARTED) {
                    mStatus = STATUS.PAUSED;
                }
                mAudioTrack.pause();
                break;
            case MSG_STOP:
                handleStop();
                break;
        }
        return false;
    }

    private void handleStop() {
        if (mAudioReader != null) {
            mAudioReader.forceClose();
        }
        if (mAudioTrack != null) {
            mAudioTrack.release();
        }
        if (mSonicHandler != null) {
            mSonicHandler.release();
        }
        if (soxHandler != null) {
            soxHandler.release();
        }
        mExecuteHandler.getLooper().quit();
    }

    private void handleRead() {
        if (mStatus == STATUS.STARTED) {
            boolean hasData = mAudioReader.readSync();
            mCurrentPlayTime = mAudioReader.getReadTime() / 1000;

            if (!hasData) {
                notifyCompletion();
                mStatus = STATUS.PREPARED;
            }
        }
    }

    private void notifyCompletion() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayCallback != null) {
                    mPlayCallback.onCompletion();
                }
            }
        });
    }

    private void handleStart() {
        if (mStatus == STATUS.PREPARED || mStatus == STATUS.PAUSED) {
            mStatus = STATUS.STARTED;
            mAudioTrack.play();
            handleRead();
        }
    }

    private void handlePrepare() {
        mAudioReader = new AudioFileReader();
        int ret = mAudioReader.openReader(mPath, Long.MIN_VALUE, Long.MAX_VALUE, this);
        if (ret < 0) {
            notifyError(-1, "open file failed");
            return;
        }
        mAudioDurationMs = mAudioReader.getAudioDuration() / 1000;

        mSonicHandler = new SonicExoHandler(mPlaySpeed, mPlayPitch);
        mSonicHandler.init(mAudioReader.getSampleRate(), mAudioReader.getChannelNum());
        TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(mAudioReader.getSampleRate(), 16,
                mAudioReader.getChannelNum(), true, false);
        soxHandler = new SoxHandler(mContextRef.get());
        soxHandler.init(mAudioReader.getSampleRate(), mAudioReader.getChannelNum());
        if (soxReverbBean != null) {
            soxHandler.setParam(soxReverbBean);
        }
        setAudioTarget(mSonicHandler);
        mSonicHandler.setAudioTarget(soxHandler);
        soxHandler.setAudioTarget(audioConsumer);

        int streamType = AudioManager.STREAM_MUSIC;
        int channelConfig = mAudioReader.getChannelNum() == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audiof = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSizeInBytes = 1024 * 100;
        int mode = AudioTrack.MODE_STREAM;
        mAudioTrack = new AudioTrack(streamType, mAudioReader.getSampleRate(), channelConfig, audiof, bufferSizeInBytes, mode);

        mCurrentPlayTime = 0;

        mAudioReader.startSync();

        notifyPrepared();
    }

    private byte[] mAudioData = new byte[8192];
    private IAudioTarget audioConsumer = new IAudioTarget() {
        @Override
        public void newDataReady(AudioFrame audioFrame) {
            if (audioFrame.info.size > 0) {
                if (mAudioData.length < audioFrame.info.size) {
                    mAudioData = new byte[audioFrame.info.size];
                }
                audioFrame.buffer.get(mAudioData, 0, audioFrame.info.size);
                mAudioTrack.write(mAudioData, 0, audioFrame.info.size);

                notifyPositionChanged();
            }

            if (mStatus != STATUS.STOPED) {
                mExecuteHandler.sendMessage(mExecuteHandler.obtainMessage(MSG_READ));
            }
        }
    };

    private void notifyPositionChanged() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayCallback != null) {
                    mPlayCallback.onPositionChanged(mCurrentPlayTime);
                }
            }
        });
    }

    private void notifyError(final int code, final String msg) {
        mStatus = STATUS.ERROR;
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayCallback != null) {
                    mPlayCallback.onError(code, msg);
                }
            }
        });
    }

    private void notifyPrepared() {
        mStatus = STATUS.PREPARED;
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayCallback != null) {
                    mPlayCallback.onPrepared();
                }
            }
        });
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

    }

    @Override
    public void onMediaFormatChange(MediaFormat format, int trackType) {

    }

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

    public interface AudioPlayerCallback {
        void onCompletion();

        void onPrepared();

        void onPositionChanged(long timeMs);

        void onError(int code, String msg);
    }

    public void setVolume(float gain) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(gain, gain);
        }
    }

}
