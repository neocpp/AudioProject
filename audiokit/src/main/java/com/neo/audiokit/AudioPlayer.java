package com.neo.audiokit;

import android.os.SystemClock;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.exo.SonicExoHandler;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.framework.IAudioTarget;
import com.neo.audiokit.tarsor.TarsorDispatcher;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AudioPlayer extends AudioChain implements IMediaDataCallBack {
    private final static String TAG = "AudioSpeedPlayer";
    private String mPath;
    private PlayThread mPlayThread;
    private AudioTrackNonBlock mAudioTrack;
    private QHMp4Reader mMp4Reader;
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
    private boolean mIsStopWait = true;

    private List<AudioProcessor> audioProcessorList;

    public interface AudioPlayerCallBack {
        void onCompletion();

        void onDuration(long fileDuration);
    }

    public AudioPlayer(AudioPlayerCallBack callBack) {
        mCallBack = callBack;
    }

    public int setDataSource(String path) {
        mPath = path;
        return 0;
    }

    public int setSpeed(float speed) {
        mPlaySpeed = speed;
        if (mSonicHandler != null) {
            mSonicHandler.setSpeedPitch(mPlaySpeed, mPlayPitch);
        }

        if (mAudioTrack != null) {
            mAudioTrack.setSpeed(speed);
        }
        return 0;
    }

    public void setPitch(float pitch) {
        mPlayPitch = pitch;
        if (mSonicHandler != null) {
            mSonicHandler.setSpeedPitch(mPlaySpeed, mPlayPitch);
        }
    }

    public int prepareAsync() {
        return 0;
    }

    synchronized public int start() {
        mFirstLocalTime = Long.MIN_VALUE;
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
            mMp4Reader.seekTo(mAudioTrack.getPlayPos() * 1000);
        }

        return 0;
    }

    public boolean isPlaying() {
        return mIsPlayThreadStart && !mIsPause;
    }

    synchronized public int stop(boolean isWait) {
        if (mPlayThread == null) {
            return 0;
        }
        mIsStopWait = isWait;
        mIsNeedNotifyCompletionFlag = false;
        mIsPause = false;
        if (mMp4Reader != null) {
            mMp4Reader.forceClose();
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
                audioFrame.buffer.get(mAudioData, audioFrame.info.offset, audioFrame.info.size);
                mAudioTrack.write(mAudioData, 0, audioFrame.info.size);
                totalSample = audioFrame.info.size / 2;
            }

            if (totalSample == 0) {
                return;
            }

            if (mAudioTrack.getSegmentPlayPos() == 0 || mFirstLocalTime == Long.MIN_VALUE) {
                mFirstLocalTime = SystemClock.elapsedRealtime();
                mFirstFileTime = 0;
                mCurrentFileTime = 0;
            } else {
                while (SystemClock.elapsedRealtime() - mFirstLocalTime < mCurrentFileTime - mFirstFileTime) {
                    sleep(1);
                }
            }
            long step = (long) (totalSample * 1000.0f / (mMp4Reader.getChannelNum() * mMp4Reader.getSampleRate()));
            mCurrentPos = mAudioTrack.getPlayPos();
            mCurrentFileTime += step;
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
        mAudioTrack.seekTo(pos);
        mMp4Reader.seekTo(pos * 1000);
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
            if (mMp4Reader != null) {
//                RecorderLogUtils.e(TAG, TAG + ".PlayThread run error");
                return;
            }
            mMp4Reader = new QHMp4Reader();
            mMp4Reader.setTrackConfig(IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio, true, true);
            mMp4Reader.setTrackConfig(IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo, false, false);
            mMp4Reader.openReader(mPath, Long.MIN_VALUE, Long.MAX_VALUE, AudioPlayer.this);
            if (mCallBack != null) {
                mCallBack.onDuration(mMp4Reader.getDuration());
            }
            mSonicHandler = new SonicExoHandler(mPlaySpeed, 1f);
            mSonicHandler.init(mMp4Reader.getSampleRate(), mMp4Reader.getChannelNum());
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(mMp4Reader.getSampleRate(), 16,
                    mMp4Reader.getChannelNum(), true, false);
            mTarsorProcesser = new TarsorDispatcher(audioFormat, mMp4Reader.getAudioBufferSize(), 0);
            if (audioProcessorList != null) {
                for (AudioProcessor ap : audioProcessorList) {
                    mTarsorProcesser.addAudioProcessor(ap);
                }
            }
            setAudioTarget(mSonicHandler);
            mSonicHandler.setAudioTarget(mTarsorProcesser);
            mTarsorProcesser.setAudioTarget(audioConsumer);
            mAudioTrack = new AudioTrackNonBlock(mMp4Reader.getSampleRate(), mMp4Reader.getChannelNum());
            mAudioTrack.setSpeed(mPlaySpeed);
            if (mFirstSeektPos > 0) {
                mAudioTrack.seekTo(mFirstSeektPos);
                mMp4Reader.seekTo(mFirstSeektPos * 1000);
            }
            mAudioTrack.play();
            mMp4Reader.start(null);
            mIsPlayThreadStart = true;
            mMp4Reader.closeReader();
            while (mAudioTrack.getQueueSize() > 0 && mIsStopWait) {
                AudioPlayer.sleep(1);
                mCurrentPos = mAudioTrack.getPlayPos();
            }
            mAudioTrack.stop();
            mAudioTrack.release();
            mSonicHandler.release();
            mMp4Reader = null;
            mAudioTrack = null;
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
}
