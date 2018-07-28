package com.neo.audiokit.io;

import com.neo.audiokit.framework.AudioFrame;
import com.neo.audiokit.framework.IAudioTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

public class WavWriter implements IAudioTarget {
    private int mSampleRate = 44100;
    private int mChannelNum = 1;
    private int mAudioBitsrate = 48000;
    private long mAudioEncodeTimestamp;
    private FileOutputStream fileOutputStream;
    private byte[] mPcmData;
    private long totalCount;
    private String outPath;

    public WavWriter() {

    }

    public void setAudioParma(int sampleRate, int channel) {
        mSampleRate = sampleRate;
        mChannelNum = channel;
        mAudioBitsrate = mSampleRate * mChannelNum * 2;
    }

    private byte[] getWavHeader(long totalAudioLen) {
        byte[] header = new byte[44];
        long totalDataLen = totalAudioLen + 36;
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) mChannelNum;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (mSampleRate & 0xff);
        header[25] = (byte) ((mSampleRate >> 8) & 0xff);
        header[26] = (byte) ((mSampleRate >> 16) & 0xff);
        header[27] = (byte) ((mSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (mAudioBitsrate & 0xff);
        header[29] = (byte) ((mAudioBitsrate >> 8) & 0xff);
        header[30] = (byte) ((mAudioBitsrate >> 16) & 0xff);
        header[31] = (byte) ((mAudioBitsrate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (mChannelNum * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }


    @Override
    public void newDataReady(AudioFrame audioFrame) {
        int nSample = audioFrame.info.size / 2 / mChannelNum;
        long step = (long) ((nSample * 1000000.0f) / (mSampleRate));
        audioFrame.info.presentationTimeUs = mAudioEncodeTimestamp;
        mAudioEncodeTimestamp += step;

        int byteSize = audioFrame.info.size;
        if (mPcmData == null || mPcmData.length < byteSize) {
            mPcmData = new byte[byteSize];
        }
        audioFrame.buffer.position(audioFrame.info.offset);
        audioFrame.buffer.get(mPcmData, 0, byteSize);

        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(mPcmData, 0, byteSize);
                totalCount += byteSize;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void newDataReady(byte[] data, int offset, int length) {
        int nSample = length / 2 / mChannelNum;
        long step = (long) ((nSample * 1000000.0f) / (mSampleRate));
        mAudioEncodeTimestamp += step;

        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(data, 0, length);
                totalCount += length;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startRecord(String outPath) {
        totalCount = 0;
        this.outPath = outPath;
        mAudioEncodeTimestamp = 0;
        try {
            fileOutputStream = new FileOutputStream(new File(outPath));
            byte[] header = getWavHeader(0);
            fileOutputStream.write(header);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        try {
            fileOutputStream.close();
            byte[] header = getWavHeader(totalCount);
            RandomAccessFile randomAccessFile = new RandomAccessFile(outPath, "rw");
            randomAccessFile.seek(0);
            randomAccessFile.write(header);
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public long getCurrentDuration() {
        return mAudioEncodeTimestamp / 1000;
    }
}
