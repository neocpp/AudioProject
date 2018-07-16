package com.neo.audiokit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;


public class AudioTrackNonBlock {
    private final static String TAG = "AudioTrackNonBlock";
    private AudioTrack mAudioTrack;
    private ConsumeThread mConsumeThread;
    private boolean mConsumeThreadRunning;
    private int mSampleRateInHz;
    private int mChannelCnt;
    private long mTotalPlayPos;
    private float mPlaySpeed = 1.0f;
    Queue<byte[]> mAudioQueue = new ArrayBlockingQueue<byte[]>(5, true);

    public AudioTrackNonBlock(int sampleRateInHz, int channelCnt) {
        mSampleRateInHz = sampleRateInHz;
        mChannelCnt = channelCnt;
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        byte[] data = new byte[sizeInBytes];
        System.arraycopy(audioData, offsetInBytes, data, 0, sizeInBytes);
        synchronized (mAudioQueue) {
            mAudioQueue.offer(data);
        }
        return 0;
    }

    synchronized public void play() {
        if (mConsumeThread != null) {
            return;
        }
        mConsumeThreadRunning = false;
        mConsumeThread = new ConsumeThread();
        mConsumeThread.start();
        while (!mConsumeThreadRunning) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSpeed(float speed) {
        mPlaySpeed = speed;
    }

    public void seekTo(long pos) {
        mTotalPlayPos = pos;
    }

    synchronized public void pause() {
        mAudioTrack.pause();
        long playPos = (long) (mAudioTrack.getPlaybackHeadPosition() * 1000.0f / mAudioTrack.getSampleRate());
        mTotalPlayPos += (playPos * mPlaySpeed);
        stop();
    }

    public int getQueueSize() {
        synchronized (mAudioQueue) {
            return mAudioQueue.size();
        }
    }

    synchronized public void stop() {
        if (mConsumeThread == null) {
            return;
        }
        mConsumeThreadRunning = false;
        try {
            mConsumeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mConsumeThread = null;
        synchronized (mAudioQueue) {
            mAudioQueue.clear();
        }
    }

    public void release() {
        mTotalPlayPos = 0;
    }

    synchronized public long getPlayPos() {
        if (mConsumeThread == null) {
            return mTotalPlayPos;
        }
        long playPos = (long) (mAudioTrack.getPlaybackHeadPosition() * 1000.0f / mAudioTrack.getSampleRate());
        return (long) (mTotalPlayPos + (playPos * mPlaySpeed));
    }

    synchronized public long getSegmentPlayPos() {
        if (mConsumeThread == null) {
            return 0;
        }
        return (long) (mPlaySpeed * (mAudioTrack.getPlaybackHeadPosition() * 1000.0f / mAudioTrack.getSampleRate()));
    }

    class ConsumeThread extends Thread {
        @Override
        public void run() {
            int streamType = AudioManager.STREAM_MUSIC;
            int channelConfig = mChannelCnt == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSizeInBytes = 1024 * 100;
            int mode = AudioTrack.MODE_STREAM;
            mAudioTrack = new AudioTrack(streamType, mSampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);
            mAudioTrack.play();
            mConsumeThreadRunning = true;
            while (mConsumeThreadRunning) {
                byte[] data = null;
                synchronized (mAudioQueue) {
                    if (mAudioQueue.size() > 0) {
                        data = mAudioQueue.poll();
                    }
                }
                if (data == null) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioTrack.write(data, 0, data.length);
                    }
                }
            }
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public void setVolume(float gain) {
        if(mAudioTrack != null){
            mAudioTrack.setStereoVolume(gain, gain);
        }
    }
}
