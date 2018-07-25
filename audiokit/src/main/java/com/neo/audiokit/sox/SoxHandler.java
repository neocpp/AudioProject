package com.neo.audiokit.sox;

import com.lib.sox.SoxJni;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoxHandler extends AudioChain {
    private long mSampleRate;
    private int mChannels;
    private SoxReverbBean reverbBean;
    private long byteRate;

    public void init(long sampleRate, int channels) {
        mSampleRate = sampleRate;
        mChannels = channels;

        byteRate = mSampleRate * mChannels * 16 / 8;
    }

    private void setWavHeader(byte[] header, long totalAudioLen, long totalDataLen) {
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
        header[22] = (byte) mChannels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (mSampleRate & 0xff);
        header[25] = (byte) ((mSampleRate >> 8) & 0xff);
        header[26] = (byte) ((mSampleRate >> 16) & 0xff);
        header[27] = (byte) ((mSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (mChannels * 16 / 8);
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
    }

    @Override
    protected boolean isActive() {
        return reverbBean == null ? false : reverbBean.isUnset();
    }

    public void setParam(SoxReverbBean bean) {
        reverbBean = bean;
    }

    private ByteBuffer cacheBuffer;

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        byte[] bytedata = new byte[audioFrame.info.size + 44];
        setWavHeader(bytedata, audioFrame.info.size + 36, audioFrame.info.size);
        audioFrame.buffer.position(audioFrame.info.offset);
        audioFrame.buffer.get(bytedata, 44, audioFrame.info.size);

        byte[] outBuffer = new byte[bytedata.length * 2];
        int outSize = SoxJni.processBuffer(bytedata, bytedata.length, outBuffer, (int) mSampleRate, mChannels,
                reverbBean.reverberance, reverbBean.hFDamping, reverbBean.roomScale, reverbBean.stereoDepth,
                reverbBean.preDelay, reverbBean.wetGain);
        if (cacheBuffer == null || cacheBuffer.capacity() < outSize) {
            cacheBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder());
        }

        cacheBuffer.clear();
        cacheBuffer.put(outBuffer, 44, outSize - 44);
        cacheBuffer.position(0);
        cacheBuffer.limit(outSize);
        audioFrame.buffer = cacheBuffer;
        audioFrame.info.offset = 0;
        audioFrame.info.size = outSize;

        return audioFrame;
    }

    @Override
    public void release() {

    }
}
