package com.neo.audiokit;

import android.os.Build;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.framework.IAudioTarget;

import java.nio.ByteBuffer;

public class FileWriter implements IAudioTarget {
    private final static String TAG = "FileWriter";
    public final static int AUDIO_ENC_BLOCK_SIZE = (100 * 1024);
    private QHMp4Writer mMp4Writer;
    private boolean mIsSoftVideoEncode = false;
    private boolean mPreferSoftEncode = false;
    private android.media.MediaFormat audioFormat;
    /**
     * 编码渲染
     */
    private int mSampleRate = 44100;
    private int mChannelNum = 1;
    private int mAudioBitsrate = 48000;
    private long mAudioEncodeTimestamp;
    private Object mAudioSync = new Object();

    private Object mMp4WriterLock = new Object();


    public FileWriter() {
    }

    public void setSoftEncode(boolean isOn) {
        mPreferSoftEncode = isOn;
    }

    public int setAudioParma(int sampleRate, int channel, int bitsrate) {
        mSampleRate = sampleRate;
        mChannelNum = channel;
        mAudioBitsrate = bitsrate;
        return 0;
    }

    public int startRecord(String outFileName) {
        if (mMp4Writer != null) {
            return -1;
        }

        audioFormat = createAudioEncodeFormat();
        //mMp4Writer类创建
        synchronized (mAudioSync) {
            mMp4Writer = new QHMp4Writer();
            mIsSoftVideoEncode = mPreferSoftEncode || isUnderAPI18();
            mMp4Writer.openWriter(outFileName, mIsSoftVideoEncode, null, false, audioFormat, true, null);
            mMp4Writer.start();

            mAudioEncodeTimestamp = 0;
        }
        return 0;
    }

    @Override
    public void newDataReady(AudioFrame audioFrame) {
        synchronized (mAudioSync) {
            if (audioFrame != null && audioFrame.info.size > 0 && mMp4Writer != null) {

//                if (mUseFrameTime) {

//                synchronized (mMp4WriterLock) {
//                    if (mMp4Writer != null) {
//                        mMp4Writer.sendData(audioFrame.buffer, audioFrame.info, audioFrame.isRawData, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
//                    }
//                }

//                } else if (mStartEncodeTime != Long.MIN_VALUE) {
//
                int nSample = audioFrame.info.size / 2 / mChannelNum;
                long step = (long) ((nSample * 1000000.0f) / (mSampleRate));
                audioFrame.info.presentationTimeUs = mAudioEncodeTimestamp;
                mAudioEncodeTimestamp += step;

                synchronized (mMp4WriterLock) {
                    if (mMp4Writer != null) {
                        mMp4Writer.sendData(audioFrame.buffer, audioFrame.info, audioFrame.isRawData, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
                    }
                }
//                }
            }
        }
    }

    public int stopRecord() {

        synchronized (mMp4WriterLock) {
            if (mMp4Writer != null) {
                mMp4Writer.closeWriter();

                if (mIsSoftVideoEncode) {
                    mMp4Writer.unInitReadPixel();
                }
                mMp4Writer = null;
            }
        }

        return 0;
    }

    public long getCurrentDuration() {
        return mAudioEncodeTimestamp / 1000;
    }

    private android.media.MediaFormat createAudioEncodeFormat() {
        android.media.MediaFormat encodeFormat = android.media.MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, mChannelNum);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, mAudioBitsrate);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, CodecBufferInfo.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_ENC_BLOCK_SIZE);
        return encodeFormat;
    }

    private boolean isUnderAPI18() {
        return Build.VERSION.SDK_INT < 18;
    }

    public void sendData(ByteBuffer data, CodecBufferInfo info, boolean isRawData, int trackType) {
        if (mMp4Writer != null) {
            mMp4Writer.sendData(data, info, isRawData, trackType);
        }
    }
}