package com.neo.audiokit;

import android.util.Log;
import android.view.Surface;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.FFVideoDecode;
import com.neo.audiokit.codec.HWAudioDecode;
import com.neo.audiokit.codec.HWVideoDecode;
import com.neo.audiokit.codec.ICodec;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;

/**
 * mp4读取类，
 * 可以读取音视频aac和h264帧，
 * 也可以读取音视频pcm和yuv帧(Raw)。
 * 也可以直接将视频数据画到Surface上。
 * 音视频帧通过IMediaDataCallBack callBack回调给调用者。
 */
public class QHMp4Reader implements IMediaDataCallBack {

    private static final String TAG = "QHMp4Reader";
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private long mDuration;
    private long mAudioDuration;
    private int mChannleNum = 1;
    private int mSampleRate = 44100;
    private long mStartTime = Long.MIN_VALUE;
    private long mFirstAudioFrameTime = Long.MIN_VALUE;
    private long mFirstVideoFrameTime = Long.MIN_VALUE;
    private long mEndTime;
    private long mMp4ReaderHandle;
    private MediaFormat mVideoTrackFormat = null;
    private MediaFormat mAudioTrackFormat = null;
    private ICodec mVideoDecode;
    private ICodec mAudioDecode;
    private boolean mHasAudio = false;
    private boolean mHasVideo = false;
    private IMediaDataCallBack mMediaDataCallBack;
    private ReadThead mReadThread;
    private boolean mRunningRead = false;
    private boolean mReadThreadIsStart = false;
    private boolean mEnableReadAudioTrack = true;
    private boolean mEnableReadVideoTrack = true;
    private boolean mAudioOutputIsRaw = false;
    private boolean mVideoOutputIsRaw = false;
    private boolean mIsAudioArriveEndTime = false;
    private boolean mIsVideoArriveEndTime = false;
    private boolean mHaveAudioSendEndFlag = false;
    private boolean mHaveVideoSendEndFlag = false;
    private boolean mUseFFmpegDecodeVideo = false;

    private boolean mOpenVideoError = false;

    private final int AUDIO_BUFFER_SIZE = 4096 * 10;

    public void useFFmpegDecodeVideo() {
        mUseFFmpegDecodeVideo = true;
    }

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
        mMp4ReaderHandle = NativeMediaLib.mp4ReaderCreate();
        NativeMediaLib.mp4ReaderOpenFile(mMp4ReaderHandle, fileName);
        if (NativeMediaLib.mp4ReaderHasAudioTrack(mMp4ReaderHandle) != 0) {
            mSampleRate = NativeMediaLib.mp4ReaderGetSampleRate(mMp4ReaderHandle);
            mChannleNum = NativeMediaLib.mp4ReaderGetChannelNum(mMp4ReaderHandle);
            mHasAudio = true;
            String mimeType = NativeMediaLib.mp4ReaderGetAudioMimetype(mMp4ReaderHandle);
            mAudioTrackFormat = MediaFormat.createAudioFormat(mimeType, mSampleRate, mChannleNum);
            byte[] csd0Data = NativeMediaLib.mp4ReaderGetAudioConfigData(mMp4ReaderHandle);
            ByteBuffer csd0ByBu = ByteBuffer.wrap(csd0Data);
            mAudioTrackFormat.setByteBuffer("csd-0", csd0ByBu);
            mAudioDuration = NativeMediaLib.mp4ReaderGetAudioDuration(mMp4ReaderHandle);
        }
        mDuration = NativeMediaLib.mp4ReaderGetDuration(mMp4ReaderHandle);
        mRotation = NativeMediaLib.mp4ReaderGetRotation(mMp4ReaderHandle);
        if (NativeMediaLib.mp4ReaderHasVideoTrack(mMp4ReaderHandle) != 0) {
            mWidth = NativeMediaLib.mp4ReaderGetWidth(mMp4ReaderHandle);
            mHeight = NativeMediaLib.mp4ReaderGetHeight(mMp4ReaderHandle);
            mHasVideo = true;
            mVideoTrackFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
            byte[] csd0Data = NativeMediaLib.mp4ReaderGetSPSData(mMp4ReaderHandle);
            ByteBuffer csd0ByBu = ByteBuffer.wrap(csd0Data);
            byte[] csd1Data = NativeMediaLib.mp4ReaderGetPPSData(mMp4ReaderHandle);
            ByteBuffer csd1ByBu = ByteBuffer.wrap(csd1Data);
            mVideoTrackFormat.setByteBuffer("csd-0", csd0ByBu);
            mVideoTrackFormat.setByteBuffer("csd-1", csd1ByBu);
            mVideoTrackFormat.setInteger(MediaFormat.KEY_ROTATION, mRotation);
        } else {
            mOpenVideoError = true;
        }
//        Log.d(TAG, TAG + ".openReader sucess");
        return 0;
    }

    /**
     * 开始
     *
     * @param surface 视频帧如果解码到Surface上需要制定
     * @return 0表示成功，负数表示失败。
     */
    public int start(Surface surface) {

        Log.d(TAG, TAG + ".start begin" + this);

        if (mOpenVideoError && mEnableReadVideoTrack) {
            Log.d(TAG, TAG + "has no video start error");
            return -1;
        }
        //创建视频解码器
        if (mHasVideo && mEnableReadVideoTrack && mVideoOutputIsRaw) {
            if (mUseFFmpegDecodeVideo) {
                mVideoDecode = new FFVideoDecode();
            } else {
                mVideoDecode = new HWVideoDecode();
            }
            if (surface == null) {
                mVideoTrackFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecBufferInfo.COLOR_FormatYUV420Flexible);
            }
            mVideoDecode.setCallBack(this);
//            mVideoDecode.openCodec(MediaFormat.MIMETYPE_VIDEO_AVC, mVideoTrackFormat, surface, false);
            mVideoDecode.start();
        }

        //创建音频解码器
        if (mHasAudio && mEnableReadAudioTrack && mAudioOutputIsRaw) {
            mAudioDecode = new HWAudioDecode();
            String audioMime = mAudioTrackFormat.getString(MediaFormat.KEY_MIME);
            mAudioDecode.setCallBack(this);
//            mAudioDecode.openCodec(audioMime, mAudioTrackFormat, null, false);
            mAudioDecode.start();
        }

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

    public int seekTo(long pos) {
        if (mMp4ReaderHandle == 0) {
            Log.e(TAG, TAG + ".seekTo native reader has not created!!\n");
            return -1;
        }
        NativeMediaLib.mp4ReaderSeekTo(mMp4ReaderHandle, pos);
        return 0;
    }

    /**
     * 设置帧数据回调的类型和是否读取对应的track
     *
     * @param trackType
     * @param enable    是否回调trackType
     * @param isRaw     回调的数据是否是yuv或者pcm数据，即是否解码
     */
    public void setTrackConfig(int trackType, boolean enable, boolean isRaw) {
        if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
            mEnableReadAudioTrack = enable;
            mAudioOutputIsRaw = isRaw;
            if (!enable) {
                mIsAudioArriveEndTime = true;
            }
        } else {
            mEnableReadVideoTrack = enable;
            mVideoOutputIsRaw = isRaw;
            if (!enable) {
                mIsVideoArriveEndTime = true;
            }
        }
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
        if (mMp4ReaderHandle != 0) {
            NativeMediaLib.mp4ReaderCloseFile(mMp4ReaderHandle);
            NativeMediaLib.mp4ReaderDestroy(mMp4ReaderHandle);
            mMp4ReaderHandle = 0;
        }
        if (mVideoDecode != null) {
            mVideoDecode.closeCodec();
            mVideoDecode = null;
        }
        if (mAudioDecode != null) {
            mAudioDecode.closeCodec();
            mAudioDecode = null;
        }
        Log.d(TAG, TAG + "closeReader");
        return 0;
    }

    /**
     * @return
     */
    public int getWidth() {
        if (mRotation == 90 || mRotation == 270) {
            return mHeight;
        } else {
            return mWidth;
        }
    }

    /**
     * @return
     */
    public int getHeight() {
        if (mRotation == 90 || mRotation == 270) {
            return mWidth;
        } else {
            return mHeight;
        }
    }

    /**
     * @return
     */
    public int getRotation() {
        return mRotation;
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
    public long getDuration() {
        return mDuration;
    }

    public long getAudioDuration() {
        return mAudioDuration;
    }

    /**
     * 不等待文件读取完成，中间发送关闭信号，调用后closeReader不再阻塞，会立马退出。
     */
    public void forceClose() {
        mRunningRead = false;
        if (mVideoDecode != null) {
            mVideoDecode.sendEndFlag();
            mVideoDecode.foreEndThread();
        }
        if (mAudioDecode != null) {
            mAudioDecode.sendEndFlag();
            mAudioDecode.foreEndThread();
        }
        Log.d(TAG, TAG + ".forceClose end");
    }

    /**
     * @return
     */
    public MediaFormat getVideoTrackFormat() {
        return mVideoTrackFormat;
    }

    /**
     * @return
     */
    public MediaFormat getAudioTrackFormat() {
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
                if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo) {
                    if (mFirstVideoFrameTime == Long.MIN_VALUE) {
                        mFirstVideoFrameTime = info.presentationTimeUs;
                    }
                    bufferInfo.presentationTimeUs = info.presentationTimeUs - mFirstVideoFrameTime;
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
                } else {
                    if (!mHaveVideoSendEndFlag) {
                        bufferInfo.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
                        ret = mMediaDataCallBack.onMediaData(mediaData, bufferInfo, isRawData, trackType);
                        mHaveVideoSendEndFlag = true;
                        //         Log.d(TAG,TAG + ".mMediaDataCallBack.onMediaData" +trackType + " " + bufferInfo.presentationTimeUs + " " + bufferInfo.flags);
                    }
                    mIsVideoArriveEndTime = true;
                }
            }
        }
        return ret;
    }

    class ReadThead extends Thread {
        @Override
        public void run() {

            if (mStartTime > 0) {
                NativeMediaLib.mp4ReaderSeekTo(mMp4ReaderHandle, mStartTime);
            }

            int dataSize;
            if (mHasVideo) {
                dataSize = mHeight * mWidth * 3;
            } else {
                dataSize = AUDIO_BUFFER_SIZE;
            }
            ByteBuffer readData = ByteBuffer.allocateDirect(dataSize);
            CodecBufferInfo info = new CodecBufferInfo();
            mRunningRead = true;
            mReadThreadIsStart = true;
            while (mRunningRead) {
                if (mIsAudioArriveEndTime && mIsVideoArriveEndTime) {
                    forceClose();
                    continue;
                }
                int sampleSize = NativeMediaLib.mp4ReaderReadPacket(mMp4ReaderHandle, readData, dataSize);
                if (sampleSize > 0) {
                    info.flags = NativeMediaLib.mp4ReaderGetFrameFlag(mMp4ReaderHandle);
                    info.size = sampleSize;
                    info.presentationTimeUs = NativeMediaLib.mp4ReaderGetFramePts(mMp4ReaderHandle);
                    info.offset = 0;
                    // Log.d(TAG,"QHMp4Reader readTrackSampleData  trackType:" + trackType + " info.presentationTimeUs:" + info.presentationTimeUs + " " + this);
                } else if (sampleSize == 0) {
                    continue;
                } else {
                    info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
                }
                if (NativeMediaLib.mp4ReaderGetFrameTrackType(mMp4ReaderHandle) == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio || info.flags == CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) {
                    if (mEnableReadAudioTrack) {
                        if (mAudioOutputIsRaw) {
                            mAudioDecode.sendData(readData, info);
                        } else {
                            onMediaDataToParent(readData, info, false, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
                        }
                    }
                }
                if (NativeMediaLib.mp4ReaderGetFrameTrackType(mMp4ReaderHandle) == IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo || info.flags == CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) {
                    if (mEnableReadVideoTrack) {
                        if (mVideoOutputIsRaw) {
                            mVideoDecode.sendData(readData, info);
                        } else {
                            onMediaDataToParent(readData, info, false, IMediaDataCallBack.IMediaDataCallBackTrackTypeVideo);
                        }
                    }
                }
                if (sampleSize < 0) {
                    Log.d(TAG, TAG + " run read mp4 end");
                    break;
                }
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

            }
        }
    }
}
