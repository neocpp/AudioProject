package com.neo.audiokit;

public class AudioRecorder implements ProgressCtrl.ProgressCtrlCallBack {
    private AudioCapture audioCapture;
    private FileWriter fileWriter;
    private String mRootPath;
    private boolean isRecording = false;
    private ProgressCtrl progressCtrl = new ProgressCtrl();
    private IRecordCallback mCallback;
    private String outFileName;

    public AudioRecorder(String rootPath, IRecordCallback callback) {
        fileWriter = new FileWriter();
        mRootPath = rootPath;
        mCallback = callback;
    }

    public void startCapture() {
        audioCapture = new AudioCapture();
        audioCapture.setFormat(44100, 1, 16);
        audioCapture.start();
    }

    public void stopCapture() {
        audioCapture.stop();
    }

    public void startRecord() {
        if (isRecording) {
            return;
        }
        progressCtrl.init(this);
        outFileName = mRootPath + "/" + System.currentTimeMillis() + ".aac";
        fileWriter.startRecord(outFileName);
        audioCapture.setAudioTarget(fileWriter);
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
        fileWriter.stopRecord();
        audioCapture.setAudioTarget(null);
        isRecording = false;

        if (mCallback != null) {
            mCallback.onRecordStop();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {

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
    }
}
