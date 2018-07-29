package com.neo.audiokit;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.EnvironmentalReverb;
import android.os.SystemClock;
import android.util.Log;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.exo.SonicExoHandler;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.framework.IAudioTarget;
import com.neo.audiokit.sox.SoxHandler;
import com.neo.audiokit.tarsor.TarsorDispatcher;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AudioPlayer extends AudioChain implements IMediaDataCallBack {
    private final static String TAG = "AudioSpeedPlayer";
    private String mPath;
    private PlayThread mPlayThread;
    private AudioTrack mAudioTrack;
    private AudioFileReader mAudioReader;
    private long mCurrentPos;
    private boolean mIsPause;
    private boolean mIsPlayThreadStart = false;
    private byte[] mAudioData = new byte[8192];
    private long mFirstLocalTime = Long.MIN_VALUE;
    private long mFirstFileTime = Long.MIN_VALUE;
    private long mCurrentFileTime = Long.MIN_VALUE;
    private AudioPlayerCallBack mCallBack;
    //是否需要回调播放结束标志
    private boolean mIsNeedNotifyCompletionFlag;
    private long mFirstSeektPos;
    private SonicExoHandler mSonicHandler;
    private TarsorDispatcher mTarsorProcesser;
    private float mPlaySpeed = 1.0f;
    private float mPlayPitch = 1f;
    private ReverbBean mReverbBean;
    private boolean mIsStopWait = true;
    private SoxHandler soxHandler;

    //    Equalizer mEqualizer;
    EnvironmentalReverb mEvnReverb;
    private List<String> reverbVals = new ArrayList<>();
    private List<Short> reverbNames = new ArrayList<>();


    private List<AudioProcessor> audioProcessorList;
    private WeakReference<Context> mContextRef;

    private long mAudioDuration;

    public interface AudioPlayerCallBack {
        void onCompletion();

        void onPrepared(AudioPlayer audioPlayer);

        void onPositionChanged(long timeMs);

    }

    public long getDuration() {
        return mAudioDuration;
    }

    public AudioPlayer(Context context, AudioPlayerCallBack callBack) {
        mCallBack = callBack;
        mContextRef = new WeakReference<Context>(context);

        try {
//            mEqualizer = new Equalizer(0, 0);
//            // 启用均衡控制效果
//            mEqualizer.setEnabled(true);

//            for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
//                reverbNames.add(i);
//                reverbVals.add(mEqualizer.getPresetName(i));
//            }

            mEvnReverb = new EnvironmentalReverb(0, 0);
            mEvnReverb.setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public List<String> getPresetValues() {
        return reverbVals;
    }

    public void setPreset(int idx) {
//        try {
//            if (mEqualizer != null) {
//                mEqualizer.usePreset(reverbNames.get(idx));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

//    public void setReverb(int idx) {
//        try {
//            mPresetReverb.setPreset(reverbNames.get(idx));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public int setDataSource(String path) {
        mPath = path;
        return 0;
    }

    public int setSpeed(float speed) {
        mPlaySpeed = speed;
        if (mSonicHandler != null) {
            mSonicHandler.setSpeedPitch(mPlaySpeed, mPlayPitch);
        }

//        if (mAudioTrack != null) {
//            mAudioTrack.setSpeed(speed);
//        }
        return 0;
    }

    public void setPitch(float pitch) {
        mPlayPitch = pitch;
        if (mSonicHandler != null) {
            mSonicHandler.setSpeedPitch(mPlaySpeed, mPlayPitch);
        }
    }

    public void setReverb(ReverbBean bean) {
        mReverbBean = bean;
//        if (soxHandler != null) {
//            soxHandler.setParam(bean);
//        }

        if (mEvnReverb != null) {
            mEvnReverb.setProperties(mReverbBean.getReverbSettings());
        }

//        if (soxHandler != null) {
//            soxHandler.setParam(mReverbBean.getSoxReverbBean());
//        }
    }

    public int prepare() {
        mAudioReader = new AudioFileReader();
        mAudioReader.openReader(mPath, Long.MIN_VALUE, Long.MAX_VALUE, AudioPlayer.this);
        mAudioDuration = mAudioReader.getAudioDuration() / 1000;
        if (mCallBack != null) {
            mCallBack.onPrepared(AudioPlayer.this);
        }
        return 0;
    }

    synchronized public int start() {
        if (!mIsPause) {
            mFirstLocalTime = Long.MIN_VALUE;
        }
        mIsPause = false;
        mIsNeedNotifyCompletionFlag = true;
        if (mAudioTrack != null) {
            mAudioTrack.play();
        }
        if (mPlayThread == null) {
            mIsPlayThreadStart = false;
            mPlayThread = new PlayThread();
            mPlayThread.start();
            while (!mIsPlayThreadStart) {
                sleep(1);
            }
        }
        return 0;
    }

    public int pause() {
        if (mPlayThread == null) {
            return 0;
        }
        mIsPause = true;
        if (mAudioTrack != null) {
            mAudioTrack.pause();
//            mAudioReader.seekTo(mAudioTrack.getPlayPos() * 1000);
        }

        return 0;
    }

    public boolean isPlaying() {
        return mIsPlayThreadStart && !mIsPause;
    }

    public void stop() {
        stop(false);
    }

    synchronized public int stop(boolean isWait) {
        if (mPlayThread == null) {
            return 0;
        }
        mIsStopWait = isWait;
        mIsNeedNotifyCompletionFlag = false;
        mIsPause = false;
        if (mAudioReader != null) {
            mAudioReader.forceClose();
        }
        try {
            mPlayThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mFirstSeektPos = 0;
        mPlayThread = null;
        return 0;
    }

    private Object readLock = new Object();

    @Override
    protected boolean isActive() {
        return true;
    }

    private IAudioTarget audioConsumer = new IAudioTarget() {
        @Override
        public void newDataReady(AudioFrame audioFrame) {
            int totalSample = 0;
            if (audioFrame.info.size > 0) {
                if (mAudioData.length < audioFrame.info.size) {
                    mAudioData = new byte[audioFrame.info.size];
                }
                audioFrame.buffer.get(mAudioData, 0, audioFrame.info.size);
                mAudioTrack.write(mAudioData, 0, audioFrame.info.size);
                totalSample = audioFrame.info.size / 2;
            }

            if (totalSample == 0) {
                return;
            }

            if (mFirstLocalTime == Long.MIN_VALUE) {
                mFirstLocalTime = SystemClock.elapsedRealtime();
                mFirstFileTime = 0;
                mCurrentFileTime = 0;
            }
//            else {
//                while (SystemClock.elapsedRealtime() - mFirstLocalTime < mCurrentFileTime - mFirstFileTime) {
//                    sleep(1);
//                }
//            }
            long step = (long) (totalSample * 1000000.0f / (mAudioReader.getChannelNum() * mAudioReader.getSampleRate()));
//            mCurrentPos = mAudioTrack.getPlayPos();
            mCurrentFileTime += step;
            Log.e(TAG, "audiotrack mCurrentPos:" + mCurrentPos + "; fileTime: " + mCurrentFileTime);

            if (mCallBack != null) {
                mCallBack.onPositionChanged(mCurrentFileTime / 1000);
            }

            synchronized (readLock) {
                if (mCurrentFileTime >= mAudioReader.getReadTime()) {
                    readLock.notify();
                }
            }

            while (mIsPause) {
                sleep(5);
            }
        }
    };

    public void addAudioProcess(AudioProcessor audioProcessor) {
        if (mTarsorProcesser != null) {
            mTarsorProcesser.addAudioProcessor(audioProcessor);
        } else {
            if (audioProcessorList == null) {
                audioProcessorList = new ArrayList<>();
            }
            audioProcessorList.add(audioProcessor);
        }
    }

    public void removeAudioProcess(AudioProcessor audioProcessor) {
        if (mTarsorProcesser != null) {
            mTarsorProcesser.removeAudioProcessor(audioProcessor);
        } else {
            if (audioProcessorList == null) {
                audioProcessorList = new ArrayList<>();
            }
            audioProcessorList.remove(audioProcessor);
        }
    }

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        return audioFrame;
    }

    public void release() {
        stop(true);
    }

    synchronized public long getCurrentPosition() {
        if (mPlayThread == null) {
            return 0;
        }
        return mCurrentPos;
    }

    public int seekTo(long pos) {
//        RecorderLogUtils.d(TAG, TAG + ".seekTo" + pos);
        if (mPlayThread == null) {
            mFirstSeektPos = pos;
            return 0;
        }
//        mAudioTrack.seekTo(pos);
        mAudioReader.seekTo(pos * 1000);
        mCurrentFileTime = pos * 1000;
        return 0;
    }

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

    class PlayThread extends Thread {
        @Override
        public void run() {
            if (mAudioReader == null) {
                prepare();
            }

            mSonicHandler = new SonicExoHandler(mPlaySpeed, 1f);
            mSonicHandler.init(mAudioReader.getSampleRate(), mAudioReader.getChannelNum());
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(mAudioReader.getSampleRate(), 16,
                    mAudioReader.getChannelNum(), true, false);
            mTarsorProcesser = new TarsorDispatcher(audioFormat, mAudioReader.getAudioBufferSize(), 0);
            if (audioProcessorList != null) {
                for (AudioProcessor ap : audioProcessorList) {
                    mTarsorProcesser.addAudioProcessor(ap);
                }
            }
            soxHandler = new SoxHandler(mContextRef.get());
            soxHandler.init(mAudioReader.getSampleRate(), mAudioReader.getChannelNum());
//            if (mReverbBean != null) {
//                soxHandler.setParam(mReverbBean.getSoxReverbBean());
//            }
            setAudioTarget(mSonicHandler);
            mSonicHandler.setAudioTarget(mTarsorProcesser);
            mTarsorProcesser.setAudioTarget(soxHandler);
            soxHandler.setAudioTarget(audioConsumer);

            int streamType = AudioManager.STREAM_MUSIC;
            int channelConfig = mAudioReader.getChannelNum() == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            int audiof = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSizeInBytes = 1024 * 100;
            int mode = AudioTrack.MODE_STREAM;
            mAudioTrack = new AudioTrack(streamType, mAudioReader.getSampleRate(), channelConfig, audiof, bufferSizeInBytes, mode);
//            mAudioTrack = new AudioTrack(mAudioReader.getSampleRate(), mAudioReader.getChannelNum());
//            if (mEvnReverb != null) {
//                mAudioTrack.attachAuxEffect(mEvnReverb.getId());
//                mAudioTrack.setAuxEffectSendLevel(1f);
//            }
//            mAudioTrack.setSpeed(mPlaySpeed);
            if (mFirstSeektPos > 0) {
//                mAudioTrack.seekTo(mFirstSeektPos);
                mAudioReader.seekTo(mFirstSeektPos * 1000);
            }
            mAudioTrack.play();
            mAudioReader.startSync();
            mIsPlayThreadStart = true;
            while (mAudioReader.readSync()) {
                synchronized (readLock) {
                    try {
                        readLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            mAudioReader.closeReader();
            mAudioReader = null;
//            while (mAudioTrack.getQueueSize() > 0 && mIsStopWait) {
//                AudioPlayer.sleep(1);
//                mCurrentPos = mAudioTrack.getPlayPos();
//            }

            mAudioTrack.stop();
            mAudioTrack.release();
            mSonicHandler.release();
//            mAudioReader = null;
            mAudioTrack = null;
            mPlayThread = null;
            if (mCallBack != null && mIsNeedNotifyCompletionFlag) {
                mCallBack.onCompletion();
            }
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setVolume(float gain) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(gain, gain);
        }
    }
}
