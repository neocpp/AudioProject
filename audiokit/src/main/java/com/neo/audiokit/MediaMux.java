
package com.neo.audiokit;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.codec.IMediaDataCallBack;
import com.neo.audiokit.codec.MediaFormat;
import com.qihoo.recorder.codec.NativeMediaLib;

import java.nio.ByteBuffer;

class MediaDataCallBackImpl implements IMediaDataCallBack {

    private boolean mIsSecond = false;
    private long mHandle = 0;
    private int mSampleRate;
    private int mChannelNum;
    private QHMp4Writer mMp4Writer;

    MediaDataCallBackImpl(long handle, boolean isSecond, QHMp4Writer mp4Writer) {
        mHandle = handle;
        mIsSecond = isSecond;
        mMp4Writer = mp4Writer;
    }

    public void setSampleRateAndChannelNum(int sampleRate, int channelNum) {
        mSampleRate = sampleRate;
        mChannelNum = channelNum;
    }

    @Override
    public void onMediaFormatChange(MediaFormat format, int trackType) {
        if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
            int sampleRate = 0;
            int bytePerSample = 0;
            int channelNum = 0;
            sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE) :
                    mSampleRate;
            channelNum = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) :
                    mChannelNum;
            bytePerSample = (format.containsKey("bit-width") ? format
                    .getInteger
                            ("bit-width") : 16) / 8;
            if (mIsSecond) {
                NativeMediaLib.setSecondAudioFormat(mHandle, sampleRate, bytePerSample, channelNum);
            } else {
                NativeMediaLib.setFirstAudioFormat(mHandle, sampleRate, bytePerSample, channelNum);
            }
        }
    }

    @Override
    public int onMediaData(ByteBuffer mediaData, CodecBufferInfo info, boolean isRawData, int trackType) {
        if (trackType == IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio) {
            if (isRawData) {
                byte[] bytesdata = null;
                if ((info.flags & CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM) == 0) {
                    bytesdata = new byte[info.size];
                    mediaData.position(info.offset);
                    mediaData.get(bytesdata);
                } else {
                    info.size = 0;
                }
                int ret = 0;
                while (true) {
                    if (mIsSecond) {
                        ret = NativeMediaLib.sendSecondAudioData(mHandle, bytesdata, info.size);
                    } else {
                        ret = NativeMediaLib.sendFirstAudioData(mHandle, bytesdata, info.size);
                    }
                    //RecorderLogUtils.d(MediaMux.TAG,"NativeMediaLib.send" + info.size + " " + ret + " " + mIsVideo);
                    if (ret == 0) {
                        break;
                    } else {
                        MediaMux.Sleep(1);
                    }
                }
            } else {
                mMp4Writer.sendData(mediaData, info, isRawData, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
            }

        } else {
            mMp4Writer.sendData(mediaData, info, isRawData, trackType);
        }
        return 0;
    }
}

public class MediaMux {

    public final static String TAG = "MediaMux";
    //编码参数配置
    public final static int ENC_SAMPLE_RATE = 44100;
    public final static int ENC_CHANNEL_NUM = 1;
    public final static int ENC_BYTE_PER_SAMPLE = 2;
    public final static int ENC_BLOCK_SIZE = (100 * 1024);
    public final static int ENC_BIT_RATE = 48000;

    private static MediaFormat createAudioEncodeFormat() {
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, ENC_SAMPLE_RATE, ENC_CHANNEL_NUM);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, ENC_BIT_RATE);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, CodecBufferInfo.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, ENC_BLOCK_SIZE);
        return encodeFormat;
    }

    private static long processMixAudioData(byte[] mixData, int size, QHMp4Writer mp4Writer, long timeStamp) {
        ByteBuffer data = ByteBuffer.allocateDirect(size);
        data.put(mixData, 0, size);
        data.position(0);
        CodecBufferInfo info = new CodecBufferInfo();
        info.flags = 0;
        info.offset = 0;
        info.size = size;
        info.presentationTimeUs = timeStamp;
        mp4Writer.sendData(data, info, true, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        return (long) ((size * 1000000.0f) / (ENC_SAMPLE_RATE * ENC_CHANNEL_NUM * ENC_BYTE_PER_SAMPLE));
    }

    public static void Sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean needConvert(MediaFormat mediaFormat) {
        boolean needConvert = true;
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("audio/mp4a")) {
            needConvert = false;
        }
        return needConvert;
    }

    static class SendMuteThread extends Thread {
        long mHandle;
        int mSampleRate;
        int mChannelNum;
        int mBytePerSample;
        long mVideoSkipTimeMs;
        QHMp4Reader mMp4Reader;

        SendMuteThread(QHMp4Reader qhMp4Reader, long handle, int sampleRate, int channelNum, int bytePerSample, long videoSkipTimeMs) {
            mMp4Reader = qhMp4Reader;
            mHandle = handle;
            mSampleRate = sampleRate;
            mChannelNum = channelNum;
            mBytePerSample = bytePerSample;
            mVideoSkipTimeMs = videoSkipTimeMs;
        }

        @Override
        public void run() {
            long muteTimeUs = 0;
            byte[] muteData = null;
            muteData = new byte[4096];
            for (int i = 0; i < 4096; i++) {
                muteData[i] = 0x00;
            }
            while (true) {
                if (muteTimeUs < (mVideoSkipTimeMs * 1000)) {
                    while (true) {
                        int sendRet = NativeMediaLib.sendFirstAudioData(mHandle, muteData, 4096);
                        ;
                        if (sendRet == 0) {
                            long step = (long) ((4096 * 1000000.0f) / (mSampleRate * mChannelNum * mBytePerSample));
                            muteTimeUs += step;
                            break;
                        } else {
                            MediaMux.Sleep(1);
                        }
                    }
                } else {
                    mMp4Reader.start(null);
                    break;
                }
            }


        }
    }

    public static boolean mux(String audioFile, long audioSkipTimeMs, String videoFile, long videoSkipTimeMs, String mergeFilePath, float audioFileAudioWeight, float videoFileAudioWeight) {

        final AudioFileReader videoReader = new AudioFileReader();
        AudioFileReader audioFileReader = null;
        QHMp4Writer mp4Writer = new QHMp4Writer();
        if (audioFile != null) {
            audioFileReader = new AudioFileReader();
        }
//        RecorderLogUtils.d(TAG,"mux start" + audioFile + " " + videoFile + " " + audioFileAudioWeight + " " + videoFileAudioWeight + " " + mergeFilePath + " " + audioSkipTimeMs);
        long handle = 0;
        if (audioFileReader != null) {
            handle = NativeMediaLib.createMixAudio(ENC_SAMPLE_RATE, ENC_BYTE_PER_SAMPLE, ENC_CHANNEL_NUM, audioFileAudioWeight, videoFileAudioWeight);
        } else {
            handle = NativeMediaLib.createMixAudio(ENC_SAMPLE_RATE, ENC_BYTE_PER_SAMPLE, ENC_CHANNEL_NUM, videoFileAudioWeight, audioFileAudioWeight);
        }


        boolean needEncodeAudio = true;
        /**读取视频文件*/
        MediaDataCallBackImpl videoReaderCallBack = new MediaDataCallBackImpl(handle, audioFile != null ? true : false, mp4Writer);
        videoReader.openReader(videoFile, Long.MIN_VALUE, Long.MAX_VALUE, videoReaderCallBack);
        videoReaderCallBack.setSampleRateAndChannelNum(videoReader.getSampleRate(), videoReader.getChannelNum());
        final boolean videoFileAudioDecFlag = (needEncodeAudio && videoReader.getAudioTrackFormat() != null);
        long startTime = audioSkipTimeMs > 0 ? (audioSkipTimeMs * 1000) : Long.MIN_VALUE;
        long endTime = videoReader.getAudioDuration() + (audioSkipTimeMs * 1000);

        /**读取音频文件*/
        if (audioFileReader != null) {
            MediaDataCallBackImpl audioCallBack = new MediaDataCallBackImpl(handle, false, mp4Writer);
            audioFileReader.openReader(audioFile, startTime, endTime, audioCallBack);
            audioCallBack.setSampleRateAndChannelNum(audioFileReader.getSampleRate(), audioFileReader.getChannelNum());
        }

        //写音视频
        mp4Writer.openWriter(mergeFilePath, false, null, false,
                needEncodeAudio ? createAudioEncodeFormat() : audioFileReader.getAudioTrackFormat(), needEncodeAudio, null);

        /************************************特效音乐合成到视频某个位置的逻辑start************************************************/

        if (audioFileReader != null) {
            if (videoSkipTimeMs == 0) {
                audioFileReader.start();
            }
        }
        /************************************特效音乐合成到视频某个位置的逻辑end************************************************/
        videoReader.start();
        mp4Writer.start();
        if (needEncodeAudio) {
            byte[] mixData = new byte[ENC_BLOCK_SIZE];
            long mAudioTS = 0;
            if (!videoFileAudioDecFlag || audioFile == null) {
                NativeMediaLib.sendSecondAudioData(handle, null, 0);
            }
            //处理音频编码
            while (true) {
                int ret = NativeMediaLib.getResultAuidoData(handle, mixData, 0);
                if (ret == -100) {
                    break;
                } else if (ret <= 0) {
                    Sleep(1);
                } else {
                    //RecorderLogUtils.d(TAG,"muxNew " + ret);
                    mAudioTS += processMixAudioData(mixData, ret, mp4Writer, mAudioTS);
                }
            }
            CodecBufferInfo info = new CodecBufferInfo();
            info.flags = CodecBufferInfo.BUFFER_FLAG_END_OF_STREAM;
            mp4Writer.sendData(null, info, true, IMediaDataCallBack.IMediaDataCallBackTrackTypeAudio);
        }

        NativeMediaLib.closeMixAudio(handle);
        if (audioFileReader != null) {
            audioFileReader.closeReader();
        }
        videoReader.closeReader();
        mp4Writer.closeWriter();
//        RecorderLogUtils.d(TAG,"muxNew sucess end");
        return true;
    }
}
