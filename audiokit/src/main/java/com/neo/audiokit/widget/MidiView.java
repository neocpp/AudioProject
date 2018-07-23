package com.neo.audiokit.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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

    float mPitchHeight = 0;
    int mPitchRectWidth = 5;

    private int widthTimeMs = 5000;
    private float timePerPixel;
    private float inViewStartTime;
    private float inViewEndTime;

    private float midiInterval;

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

    private void init() {
        paint = new Paint();
        needDrawInfoList = new ArrayList<>();

        float screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        timePerPixel = widthTimeMs / screenWidth;

        inViewStartTime = -timePerPixel * startLineOffset;
        inViewEndTime = widthTimeMs + inViewStartTime;

        midiInterval = (getHeight() - mPitchRectWidth * 2) / 7f;
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
        canvas.drawColor(Color.BLACK);

        //  画横线
        paint.setColor(Color.LTGRAY);
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
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawLine(startLineOffset, getTop(), startLineOffset, getBottom(), paint);

        // 画人声音高
        canvas.drawRect(startLineOffset - mPitchRectWidth, mPitchHeight - mPitchRectWidth,
                startLineOffset + mPitchRectWidth, mPitchHeight + mPitchRectWidth, paint);

        // 画midi信息
        needDrawInfoList.clear();
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
        }

        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(mPitchRectWidth);
        if (midiInterval <= 0) {
            midiInterval = (getHeight() - mPitchRectWidth * 2) / 7f;
        }
        for (MidiNoteInfo info : needDrawInfoList) {
            float startx = (info.getStart() - inViewStartTime) / timePerPixel;
            float endx = startx + (float) info.getDuration() / timePerPixel;

            float lineHeight = midiNoteToHeight(info.getMidiNote());
            canvas.drawLine(startx, lineHeight, endx, lineHeight, paint);
        }

    }

    private static double hertzToMidiNote(double hertz) {
        return 69 + 12 * Math.log(hertz / 440) / Math.log(2);
    }

    public void setPitch(float pitch) {
        if (pitch > 0) {
            final double midinote = hertzToMidiNote(pitch);
            post(new Runnable() {
                @Override
                public void run() {
                    mPitchHeight = midiNoteToHeight((float) midinote);
                }
            });

        }
    }

    private float midiNoteToHeight(float midiNote) {
        return getHeight() - (midiNote % 7 * midiInterval) + mPitchRectWidth;
    }
}
