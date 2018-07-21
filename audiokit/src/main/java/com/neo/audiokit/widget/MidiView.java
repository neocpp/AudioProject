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
import java.util.List;

/**
 * Created by boyo on 18/7/21.
 */

public class MidiView extends View {
    private List<MidiNoteInfo> processedSamples;
    private Handler mHandler = new Handler();

    private Paint paint;
    int startOffset = 180;

    int mPitchHeight = 0;
    int mPitchRectWidth = 5;

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
    }

    public void loadMid(String path) {
        try {
            MidiParser parser = new MidiParser(new File(path));
            processedSamples = parser.generateNoteInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO

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
        canvas.drawLine(startOffset, getTop(), startOffset, getBottom(), paint);

        // 画人声音高
        canvas.drawRect(startOffset - mPitchRectWidth, mPitchHeight - mPitchRectWidth,
                startOffset + mPitchRectWidth, mPitchHeight + mPitchRectWidth, paint);
    }

    public void setPitch(final float pitch) {
        if (pitch > 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    mPitchHeight = getHeight() - (int) (pitch % getHeight());
                    invalidate();
                }
            });

        }
    }
}
