package com.neo.audiokit.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.neo.audiokit.midi.MidiNoteInfo;
import com.neo.audiokit.midi.MidiParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boyo on 18/7/21.
 */

public class MidiView extends View {
    private List<MidiNoteInfo> processedSamples;
    private List<MidiNoteInfo> needDrawInfoList;

    private Paint paint;
    int startLineOffset = 180;
    float startTimeOffset;
    float mPitchHeight = -1;
    int mPitchRectWidth = 5;

    private int widthTimeMs = 5000;
    private float timePerPixel;
    private float inViewStartTime;
    private float inViewEndTime;
    private float midiInterval;

    private int rowColor;
    private int columeColor;
    private int midiColor;

    private int inTargetCount;
    private int totalCount;

    private final static int MOD_VALUE = 14;

    public MidiView(Context context) {
        super(context);
        init();
    }

    public MidiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MidiView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void reset() {
        inTargetCount = 0;
        totalCount = 0;
    }

    private void init() {
        paint = new Paint();
        needDrawInfoList = new ArrayList<>();

        float screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        timePerPixel = widthTimeMs / screenWidth;

        startTimeOffset = timePerPixel * startLineOffset;
        inViewStartTime = -timePerPixel * startLineOffset;
        inViewEndTime = widthTimeMs + inViewStartTime;

        midiInterval = (getHeight() - mPitchRectWidth * 2f) / MOD_VALUE;

        setBackgroundColor(Color.parseColor("#0a0f0f"));
        rowColor = Color.parseColor("#3f4343");
        columeColor = Color.parseColor("#babcbc");
        midiColor = Color.parseColor("#31e98f");

        mPitchRectWidth = dp2px(getContext(), 3);
    }

    int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }


    public void loadMid(String path) {
        try {
            MidiParser parser = new MidiParser(new File(path));
            processedSamples = parser.generateNoteInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setProgress(final float timeMs) {
        post(new Runnable() {
            @Override
            public void run() {
                inViewStartTime = timeMs - startLineOffset * timePerPixel;
                inViewEndTime = widthTimeMs + inViewStartTime;
                invalidate();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //  画背景
//        canvas.drawColor(Color.BLACK);

        //  画横线
        paint.setColor(rowColor);
        int lineWidth = 2;
        float interval = (getHeight() - lineWidth) / 5f;
        int liney = getTop() + lineWidth;
        paint.setStrokeWidth(lineWidth);
        canvas.drawLine(getLeft(), liney, getRight(), liney, paint);
        for (int i = 0; i < 5; i++) {
            liney += interval;
            canvas.drawLine(getLeft(), liney, getRight(), liney, paint);
        }

        // 画竖线
        paint.setColor(columeColor);
        paint.setStrokeWidth(3);
        canvas.drawLine(startLineOffset, getTop(), startLineOffset, getBottom(), paint);

        // 画midi信息
        needDrawInfoList.clear();
        if (processedSamples != null && !processedSamples.isEmpty()) {
            float curTime = inViewStartTime + startTimeOffset;
            for (MidiNoteInfo info : processedSamples) {
                int startMs = info.getStart();
                if (startMs > inViewEndTime) {
                    break;
                }
                int endMs = startMs + info.getDuration();
                if (endMs < inViewStartTime) {
                    continue;
                }
                needDrawInfoList.add(info);

                // TODO 评分
                if (startMs <= curTime && endMs >= curTime) {
                    if (Math.abs(mPitch - info.getMidiNote()) < 5) {
                        inTargetCount++;
                    }
                    totalCount++;
                }
            }
            if (!needDrawInfoList.isEmpty()) {
                paint.setColor(midiColor);
                paint.setStrokeWidth(mPitchRectWidth);
                if (midiInterval <= 0) {
                    midiInterval = (getHeight() - mPitchRectWidth * 2f) / MOD_VALUE;
                }
                for (MidiNoteInfo info : needDrawInfoList) {
                    float startx = (info.getStart() - inViewStartTime) / timePerPixel;
                    float endx = startx + (float) info.getDuration() / timePerPixel;

                    float lineHeight = midiNoteToHeight(info.getMidiNote());
                    canvas.drawLine(startx, lineHeight, endx, lineHeight, paint);
                }
            }
        }

        // 画人声音高
        if (mPitchHeight < 0) {
            mPitchHeight = getHeight();
        }
        canvas.save();
        canvas.translate(startLineOffset, mPitchHeight);
        canvas.rotate(45);
        paint.setColor(Color.WHITE);
        canvas.drawRect(0 - mPitchRectWidth, 0 - mPitchRectWidth,
                0 + mPitchRectWidth, 0 + mPitchRectWidth, paint);
        canvas.restore();
    }

    private static double hertzToMidiNote(double hertz) {
        return 69 + 12 * Math.log(hertz / 440) / Math.log(2);
    }

    private double mPitch;

    public void setPitch(float pitch) {
        if (pitch > 0) {
            final double midinote = hertzToMidiNote(pitch);
            post(new Runnable() {
                @Override
                public void run() {
                    mPitch = midinote;
                    mPitchHeight = midiNoteToHeight((float) midinote);
                }
            });

        }
    }

    public int getScore() {
        Log.e("test", "inTargetCount:" + inTargetCount + ", totalCount:" + totalCount);
        return totalCount == 0 ? 0 : (int) (inTargetCount / (float) totalCount * 100);
    }

    private float midiNoteToHeight(float midiNote) {
        return getHeight() - (midiNote % MOD_VALUE * midiInterval) + mPitchRectWidth;
    }
}
