package com.neo.audiokit;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import com.neo.audiokit.codec.CodecBufferInfo;
import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * 音频采集封装类
 *
 * @author adi
 */
public class AudioCapture extends AudioChain {
    // 日志开头前缀
    public static final String TAG = "AudioCapture::";
    // 音频获取源
    private int m_iaudioSource = MediaRecorder.AudioSource.MIC;
    // 采样率
    private int m_isampleRateInHz = 44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_IN_MONO为单声道
    private int m_ichannelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private int m_iaudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int m_ibufferSizeInBytes = 0;
    // 音频采集对象
    private AudioRecord m_oaudioRecord;
    // 线程是否存活标志
    private boolean m_bThreadAliveFlag = true;
    //音频采集线程
    private AudioRecordThread m_oAudioThread = null;
    //视频数据缓冲区
    private byte[] m_baAudioDataBuffer = null;
    //保存回调接口
    private boolean mFirstGetBadFrame = true;

    private int m_ibufferSizeInSample = 0;

    /**
     * 音频数据回调接口
     *
     * @author adi
     */
    public interface AudioCaptureCallBack {
        /**
         * @param data
         * @param size
         */
        public void OnAuidoData(byte[] data, int size);
    }

    /**
     * 设置采集的音频格式
     *
     * @param aiSampleRate 音频采样率
     * @param aiChannel    音频声道数
     * @param aiBitWidth   音频声道数
     */
    @Deprecated
    public void setFormat(int aiSampleRate, int aiChannel, int aiBitWidth) {
        m_isampleRateInHz = aiSampleRate;
        if (aiChannel == 1) {
            m_ichannelConfig = AudioFormat.CHANNEL_IN_MONO;
        } else if (aiChannel == 2) {
            m_ichannelConfig = AudioFormat.CHANNEL_IN_STEREO;
        }

        if (aiBitWidth == 8) {
            m_iaudioFormat = AudioFormat.ENCODING_PCM_8BIT;
        } else if (aiBitWidth == 16) {
            m_iaudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        }

        Log.d(TAG, TAG + "setFormat() " + "采样率： " + aiSampleRate + "声道数:"
                + aiChannel + "字节数:" + aiBitWidth);

        m_ibufferSizeInBytes = AudioRecord.getMinBufferSize(m_isampleRateInHz,
                m_ichannelConfig, m_iaudioFormat);

        m_ibufferSizeInSample = m_ibufferSizeInBytes / aiChannel / ((aiBitWidth + 7) / 8);
    }

    public int getSampleBufferSize() {
        return m_ibufferSizeInSample;
    }

    /**
     * 开始音频采集
     *
     * @return 0表示成功，负数表示错误码
     */
    public int start() {
        // 日志前缀
        String lsLogPrexfix = TAG + "start() ";
        if (m_oaudioRecord != null) {
            Log.d(TAG, lsLogPrexfix + "AudioRecord已经创建");
            return -1;
        }
//        m_ibufferSizeInBytes = AudioRecord.getMinBufferSize(m_isampleRateInHz,
//                m_ichannelConfig, m_iaudioFormat);
        // 创建AudioRecord对象
        m_oaudioRecord = new AudioRecord(m_iaudioSource, m_isampleRateInHz,
                m_ichannelConfig, m_iaudioFormat, m_ibufferSizeInBytes);
        if (m_oaudioRecord == null) {
            Log.d(TAG, lsLogPrexfix + "创建AudioRecord失败");
            return -2;
        }

        mFirstGetBadFrame = true;
        m_baAudioDataBuffer = new byte[m_ibufferSizeInBytes];
        // 开始采集
        try {
            //部分机型无录音权限时候会在这里崩溃,保护一下。
            m_oaudioRecord.startRecording();
        } catch (Exception e) {
            m_oaudioRecord = null;
        }
        m_bThreadAliveFlag = true;
        // 开启音频采集线程
        if (m_oaudioRecord != null) {
            m_oAudioThread = new AudioRecordThread();
            m_oAudioThread.start();
        }
        Log.d(TAG, lsLogPrexfix + "成功 ." + "采样率： " + m_isampleRateInHz + "声道数:"
                + m_ichannelConfig + "字节数:" + m_iaudioFormat + "音频缓存大小:" + m_ibufferSizeInBytes);
        return 0;
    }

    /**
     * 停止音频采集
     *
     * @return 0表示成功，负数表示错误码
     */
    public int stop() {
        if (m_oAudioThread != null) {
            m_bThreadAliveFlag = false;

            try {
                Log.d(TAG, TAG + "stop() " + "开始停止音频线程");
                m_oAudioThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, TAG + "stop() " + "停止音频线程 异常：" + e.toString());
            }
            m_oAudioThread = null;
            Log.d(TAG, TAG + "stop() " + "结束停止音频线程");
        }
        if (m_oaudioRecord != null) {
            Log.d(TAG, TAG + "stop() " + "停止音频采集");
            try {
                //部分机型无录音权限时候会在这里崩溃,保护一下。
                m_oaudioRecord.stop();
                m_oaudioRecord.release();// 释放资源
            } catch (Exception e) {
                e.printStackTrace();
                ;
            }
            m_oaudioRecord = null;
        }
        m_baAudioDataBuffer = null;
        Log.d(TAG, TAG + "stop() " + "成功");
        return 0;
    }

    /**
     * 获取录音权限
     *
     * @return
     */
    public static boolean getRecordPermission() {
        boolean ret = false;
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            if (mic != null) {
                mic.startRecording();
                byte pcmBuffer[] = new byte[minBufferSize];
                int size = mic.read(pcmBuffer, 0, pcmBuffer.length);
                Log.d(TAG, TAG + ".getRecordPermission" + size);
                if (size > 0) {
                    ret = true;
                }
                mic.stop();
                mic.release();
                mic = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 音频采集线程
     *
     * @author adi
     */
    class AudioRecordThread extends Thread {

        @Override
        public void run() {
            while (m_bThreadAliveFlag) {
                int liReadSize = m_oaudioRecord.read(m_baAudioDataBuffer, 0,
                        m_ibufferSizeInBytes);
                if (liReadSize > 0) {
                    OnAuidoData(m_baAudioDataBuffer, liReadSize);
                } else {
                    if (mFirstGetBadFrame) {
                        OnAuidoData(m_baAudioDataBuffer, -100);
                        mFirstGetBadFrame = false;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, TAG + ".AudioRecordThread::run() failed " + liReadSize);
                    ;
                }
            }
        }
    }

    private void OnAuidoData(byte[] data, int size) {
        AudioFrame audioFrame = new AudioFrame();
        audioFrame.info = new CodecBufferInfo();
        audioFrame.info.flags = 0;
        audioFrame.info.presentationTimeUs = SystemClock.elapsedRealtime() * 1000;
        audioFrame.info.size = size;
        audioFrame.info.offset = 0;

        audioFrame.isRawData = true;

        if (size > 0) {
            audioFrame.buffer = ByteBuffer.allocateDirect(size);
            audioFrame.buffer.order(ByteOrder.nativeOrder());
            audioFrame.buffer.put(data, 0, size);
            audioFrame.buffer.position(0);
        }

        newDataReady(audioFrame);
    }

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        return audioFrame;
    }

    @Override
    protected boolean isActive() {
        return true;
    }

    @Override
    public void release() {

    }
}
