package com.neo.audiokit;

import java.util.Timer;
import java.util.TimerTask;

public class ProgressCtrl {
    interface ProgressCtrlCallBack{
        int timerCall();
    }
    private Timer mTimer;
    private TimerTask mTimerTask;
    private ProgressCtrl.ProgressCtrlCallBack mCallback;
    public void init(ProgressCtrl.ProgressCtrlCallBack callBack){
        mCallback = callBack;
        initTimer();
    }
    public void unInit(){
        unInitTimer();
        mCallback = null;
    }
    private void initTimer() {
        if (mTimer == null)
            mTimer = new Timer();
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                public void run() {
                    mCallback.timerCall();
                }
            };
            mTimer.scheduleAtFixedRate(mTimerTask, 0, 30);
        }
    }

    private void unInitTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }
}