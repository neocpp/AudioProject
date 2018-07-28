package com.neo.audiokit;

import android.util.Log;

import com.neo.audiokit.io.WavWriter;
import com.neo.audiokit.tarsor.TarsorDispatcher;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class AudioRecorder implements ProgressCtrl.ProgressCtrlCallBack {
    private static final String TAG = "AudioRecorder";
    private AudioCapture audioCapture;
    private WavWriter fileWriter;
    private String mRootPath;
    private boolean isRecording = false;
    private ProgressCtrl progressCtrl = new ProgressCtrl();
    private IRecordCallback mCallback;
    private String outFileName;
    private TarsorDispatcher tarsorDispatcher;

    public AudioRecorder(String rootPath, IRecordCallback callback) {
        fileWriter = new WavWriter();
        mRootPath = rootPath;
        mCallback = callback;

        startCapture();
    }

    private void startCapture() {
        audioCapture = new AudioCapture();
        audioCapture.setFormat(44100, 1, 16);
        TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, false);
        audioCapture.start();
        tarsorDispatcher = new TarsorDispatcher(audioFormat, audioCapture.getSampleBufferSize(), 0);
        tarsorDispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 44100,
                audioCapture.getSampleBufferSize(), new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                float pitchInHz = pitchDetectionResult.getPitch();
                Log.e(TAG, "pitch:" + pitchInHz);
                if (mCallback != null) {
                    mCallback.onDetectPitch(pitchInHz);
                }

            }
        }));
        tarsorDispatcher.setAudioTarget(fileWriter);
    }

    private void stopCapture() {
        audioCapture.stop();
    }

    public void startRecord() {
        if (isRecording) {
            return;
        }
        progressCtrl.init(this);
        outFileName = mRootPath + "/" + System.currentTimeMillis() + ".wav";
        fileWriter.setAudioParma(44100, 1);
        fileWriter.startRecord(outFileName);
        audioCapture.setAudioTarget(tarsorDispatcher);
        isRecording = true;

        if (mCallback != null) {
            mCallback.onRecordStart();
        }
    }

    public String getFilePath() {
        return outFileName;
    }

    public void stopRecord() {
        if (!isRecording) {
            return;
        }
        progressCtrl.unInit();
        audioCapture.setAudioTarget(null);
        fileWriter.stopRecord();
        isRecording = false;

        if (mCallback != null) {
            mCallback.onRecordStop();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {
        stopCapture();
    }

    @Override
    public int timerCall() {
        long time = fileWriter.getCurrentDuration();
        if (mCallback != null) {
            mCallback.onRecordProgress(time);
        }
        return 0;
    }

    public interface IRecordCallback {
        void onRecordStart();

        void onRecordProgress(long timeMs);

        void onRecordStop();

        void onDetectPitch(float pitchInHz);
    }
}
