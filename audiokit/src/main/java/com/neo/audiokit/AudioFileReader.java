package com.neo.audiokit;

import android.media.MediaExtractor;
import android.util.Log;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.FFAudioDecode;
import com.neo.audiokit.codec.HWAudioDecode;
import com.neo.audiokit.codec.ICodec;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;

import java.nio.ByteBuffer;

public class AudioFileReader implements IMediaDataCallBack {

    private static final String TAG = "AudioFileReader";
    private long mAudioDuration;
    private int mChannleNum = 1;
    private int mSampleRate = 44100;
    private long mStartTime = Long.MIN_VALUE;
    private long mFirstAudioFrameTime = Long.MIN_VALUE;
    private long mEndTime;
    private android.media.MediaFormat mAudioTrackFormat = null;
    private ICodec mAudioDecode;
    private IMediaDataCallBack mMediaDataCallBack;
    private ReadThead mReadThread;
    private boolean mRunningRead = false;
    private boolean mReadThreadIsStart = false;
    private boolean mIsAudioArriveEndTime = false;
    private boolean mHaveAudioSendEndFlag = false;

    private final int AUDIO_BUFFER_SIZE = 4096 * 10;
    private MediaExtractor mediaExtractor;

    /**
     * 打开mp4Reader
     *
     * @param fileName  要解析的mp4文件名
     * @param startTime 从startTime时间位置开始读取mp4文件，默认值是0，表示从头开始。
     * @param endTime   到endTime时间位置结束读取mp4文件，默认值是Long.MAX_VALUE
     * @param callBack  帧数据和格式回调函数
     * @return 0表示成功，负数表示失败。
     */
    public int openReader(String fileName, long startTime, long endTime, IMediaDataCallBack callBack) {
        mStartTime = startTime;
        mEndTime = endTime;
        mMediaDataCallBack = callBack;
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(fileName);
            mediaExtractor.selectTrack(0);

            android.media.MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);
            mSampleRate = mediaFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE);
            mChannleNum = mediaFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
            mAudioDuration = mediaFormat.getLong(android.media.MediaFormat.KEY_DURATION);
            String mime = mediaFormat.getString(android.media.MediaFormat.KEY_MIME);
            mAudioTrackFormat = mediaFormat;//MediaFormat.createAudioFormat(mime, mSampleRate, mChannleNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 开始
     */
    public int start() {

        Log.d(TAG, TAG + ".start begin" + this);


        //创建音频解码器
        mAudioDecode = new HWAudioDecode();
        String audioMime = mAudioTrackFormat.getString(MediaFormat.KEY_MIME);
        mAudioDecode.setCallBack(this);
        mAudioDecode.openCodec(audioMime, mAudioTrackFormat, null, false);
        mAudioDecode.start();

        //启动音视频读取线程
        mReadThreadIsStart = false;
        mReadThread = new ReadThead();
        mReadThread.start();
        while (!mReadThreadIsStart) {
            try {
                Log.d(TAG, TAG + ".start wait" + this);
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, TAG + ".start sucess");
        return 0;
    }

    public void startSync() {
        mAudioDecode = new HWAudioDecode();
        String audioMime = mAudioTrackFormat.getString(MediaFormat.KEY_MIME);
        mAudioDecode.setCallBack(this);
        mAudioDecode.openCodec(audioMime, mAudioTrackFormat, null, false);
        mAudioDecode.start();

        int dataSize = AUDIO_BUFFER_SIZE;
        readData = ByteBuffer.allocateDirect(dataSize);
        info = new CodecBufferInfo();
    }

    ByteBuffer readData;
    CodecBufferInfo info;

    public long getReadTime() {
        return info.presentationTimeUs;
    }

    public boolean readSync() {
        int sampleSize = mediaExtractor.readSampleData(readData, 0);
        Log.d(TAG, "readSample:" + sampleSize);
        if (sampleSize > 0) {
            info.flags = 0;
            info.size = sampleSize;
            info.presentationTimeUs = mediaExtractor.getSampleTime();
            Log.d(TAG, "presentationTimeUs:" + info.presentationTimeUs);
            info.offset = 0;

            mediaExtractor.advance();
        } else if (sampleSize == 0) {
            mediaExtractor.advance();
        } else {
//            info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
//            mAudioDecode.sendData(readData, info);
            return false;
        }
        mAudioDecode.sendData(readData, info);
        return true;
    }

    public int seekTo(long pos) {
        mediaExtractor.seekTo(pos, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        return 0;
    }

    /**
     * 关闭Reader，该函数正常情况下会阻塞，直到mp4文件读取完场。
     *
     * @return
     */
    public int closeReader() {
        try {
            if (mReadThread != null) {
                mReadThread.join();
            }
            mReadThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
        if (mAudioDecode != null) {
            mAudioDecode.closeCodec();
            mAudioDecode = null;
        }
        Log.d(TAG, TAG + "closeReader");
        return 0;
    }

    public int getChannelNum() {
        return mChannleNum;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getAudioBufferSize() {
        return 1024;
    }

    /**
     * @return
     */

    public long getAudioDuration() {
        return mAudioDuration;
    }

    /**
     * 不等待文件读取完成，中间发送关闭信号，调用后closeReader不再阻塞，会立马退出。
     */
    public void forceClose() {
        mRunningRead = false;
        if (mAudioDecode != null) {
            mAudioDecode.sendEndFlag();
            mAudioDecode.foreEndThread();
        }
        Log.d(TAG, TAG + ".forceClose end");
    }

    /**
     * @return
     */
    public android.media.MediaFormat getAudioTrackFormat() {
        return mAudioTrackFormat;
    }

    @Override
    public void onMediaFormatChange(MediaFormat format, int trackType) {
        if (mMediaDataCallBack != null) {
            mMediaDataCallBack.onMediaFormatChange(format, trackType);
        }
    }

    @Override
    public int onMediaData(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType) {
        return onMediaDataToParent(mediaData, info, true, trackType);
    }

    private int onMediaDataToParent(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType) {
        int ret = 0;
        CodecBufferInfo bufferInfo = new CodecBufferInfo();
        bufferInfo.presentationTimeUs = info.presentationTimeUs;
        bufferInfo.size = info.size;
        bufferInfo.flags = info.flags;
        bufferInfo.offset = info.offset;
        if (mMediaDataCallBack != null) {
            if (info.presentationTimeUs <= mEndTime && info.presentationTimeUs >= mStartTime) {
                if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
                    if (mFirstAudioFrameTime == Long.MIN_VALUE) {
                        mFirstAudioFrameTime = info.presentationTimeUs;
                    }
                    bufferInfo.presentationTimeUs = info.presentationTimeUs - mFirstAudioFrameTime;
                    bufferInfo.decodeTimeUs = bufferInfo.presentationTimeUs;
                }
                ret = mMediaDataCallBack.onMediaData(mediaData, bufferInfo, isRawData, trackType);
                // Log.d(TAG,TAG + ".mMediaDataCallBack.onMediaData" +trackType + " " + bufferInfo.presentationTimeUs + " " + bufferInfo.flags);
            } else if (info.presentationTimeUs > mEndTime) {
                if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
                    if (!mHaveAudioSendEndFlag) {
                        bufferInfo.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
                        ret = mMediaDataCallBack.onMediaData(mediaData, bufferInfo, isRawData, trackType);
                        mHaveAudioSendEndFlag = true;
                        //   Log.d(TAG,TAG + ".mMediaDataCallBack.onMediaData" +trackType + " " + bufferInfo.presentationTimeUs + " " + bufferInfo.flags);
                    }
                    mIsAudioArriveEndTime = true;
                }
            }
        }
        return ret;
    }

    class ReadThead extends Thread {
        @Override
        public void run() {

            if (mStartTime > 0) {
                mediaExtractor.seekTo(mStartTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            }

            int dataSize = AUDIO_BUFFER_SIZE;
            ByteBuffer readData = ByteBuffer.allocateDirect(dataSize);
            CodecBufferInfo info = new CodecBufferInfo();
            mRunningRead = true;
            mReadThreadIsStart = true;
            while (mRunningRead) {
                if (mIsAudioArriveEndTime) {
                    forceClose();
                    continue;
                }
                int sampleSize = mediaExtractor.readSampleData(readData, 0);
                Log.d(TAG, "readSample:" + sampleSize);
                if (sampleSize > 0) {
                    info.flags = 0;
                    info.size = sampleSize;
                    info.presentationTimeUs = mediaExtractor.getSampleTime();
                    Log.d(TAG, "presentationTimeUs:" + info.presentationTimeUs);
                    info.offset = 0;

                    mediaExtractor.advance();
                } else if (sampleSize == 0) {
                    mediaExtractor.advance();
                    continue;
                } else {
                    info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
                }
                mAudioDecode.sendData(readData, info);
                if (sampleSize < 0) {
                    Log.d(TAG, TAG + " run read mp4 end");
                    break;
                }

            }
        }
    }
}
