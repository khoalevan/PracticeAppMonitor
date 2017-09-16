package com.example.khoalevan.practiceappmonitor;

import android.content.ContentProvider;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by khoalevan on 6/21/17.
 */

public class ViewGraphic extends TextureView {

    private final Resources res;
    private final String readIntervalText, updateIntervalText, graphicIntervaWidthlText;
    private int thickGrid, thickParam, thickEdges, textSize, textSizeLegend, yTopSeparation, graphicMode, processesMode, yTop, yBottom, xLeft,
        xRight, graphicHeight, graphicWidth, intervalTotalNumber, minutes, seconds, tempVar;
    private ServiceReader mSR;
    private Canvas canvas;
    private boolean graphicInitialised;
    private Thread mThread;
    private Rect bgRect;
    private Paint bgPaint, circlePaint, linesEdgePaint, linesGridPaint, cpuTotalPaint, cpuAMPaint, memUsedPaint, memAvailablePaint, memFreePaint,
        cachedPaint, thresholdPaint, textPaintRecording, textPaintInside, textPaintLegend, textPaintLegendV;

    public ViewGraphic(Context context, AttributeSet attrs) {
        super(context, attrs);

        res = getResources();
        float sD = res.getDisplayMetrics().density;

        readIntervalText = res.getString(R.string.interval_read);
        updateIntervalText = res.getString(R.string.interval_update);
        graphicIntervaWidthlText = res.getString(R.string.interval_width);

        thickGrid = (int) Math.ceil(1*sD);
        thickParam = (int) Math.ceil(1*sD);
        thickEdges = (int) Math.ceil(2*sD);

        textSize = (int) Math.ceil(10*sD);
        textSizeLegend = (int) Math.ceil(10*sD);

        yTopSeparation = (int) Math.ceil(13*sD);
    }

    protected void onDrawCustomised(Canvas canvas, Thread thread) {
        if (mSR == null || canvas == null) return;

        if (!graphicInitialised) initializeGraphic();
        mThread = thread;

        if (mThread == null || mThread.isInterrupted()) {
            return;
        }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (mThread.isInterrupted()) return;
        canvas.drawRect(bgRect, bgPaint);

        /* Horizontal lines*/
        for (float n = 0.1f;n < 1.0f; n += 0.2f) {
            if (mThread.isInterrupted()) return;
            canvas.drawLine(xLeft, yTop+graphicHeight*n, xRight, yTop*graphicHeight*n, linesGridPaint);
        }

        /* Vertical lines*/
        for (int n = 1; n < minutes; ++n) {
            tempVar = xRight - n*mSR.getIntervalWidth()*(int)(60/((float) mSR.getIntervalRead()/1000));
            if (mThread.isInterrupted()) return;
            canvas.drawLine(tempVar, yTop, tempVar, yBottom, linesGridPaint);
        }
    }

    private void initializeGraphic() {
        yTop = (int) (getHeight()*0.1);
        yBottom = (int) (getHeight()*0.88);
        xLeft = (int) (getWidth()*0.14);
        xRight = (int) (getWidth()*0.94);

        graphicWidth = xRight - xLeft;
        graphicHeight = yBottom - yTop;

        bgRect = new Rect(xLeft, yTop, xRight, yBottom);

        calculateInnerVariable();

        bgPaint = getPaint(Color.LTGRAY, Paint.Align.CENTER, 12, false, 0);
        circlePaint = getPaint(Color.RED, Paint.Align.CENTER, 12, false, 0);

        linesEdgePaint = getPaint(res.getColor(R.color.shadow), Paint.Align.CENTER, 12, false, thickEdges);
        linesGridPaint = getPaint(res.getColor(R.color.shadow), Paint.Align.CENTER, 12, false, thickGrid);
        linesGridPaint.setStyle(Paint.Style.STROKE);
        linesGridPaint.setPathEffect(new DashPathEffect(new float[]{8, 8}, 0));

        cpuTotalPaint = getPaint(res.getColor(R.color.process1), Paint.Align.CENTER, 12, false, thickParam);
        cpuAMPaint = getPaint(res.getColor(R.color.process2), Paint.Align.CENTER, 12, false, thickParam);

        memUsedPaint = getPaint(res.getColor(R.color.Orange), Paint.Align.CENTER, 12, false, thickParam);
        memAvailablePaint = getPaint(Color.MAGENTA, Paint.Align.CENTER, 12, false, thickParam);
        memFreePaint = getPaint(Color.parseColor("#804000"), Paint.Align.CENTER, 12, false, thickParam);
        cachedPaint = getPaint(Color.BLUE, Paint.Align.CENTER, 12, false, thickParam);
        thresholdPaint = getPaint(Color.GREEN, Paint.Align.CENTER, 12, false, thickParam);

        textPaintRecording = getPaint(Color.BLACK, Paint.Align.RIGHT, textSize, true, 0);
        textPaintInside = getPaint(Color.BLACK, Paint.Align.LEFT, textSize, true, 0);
        textPaintLegend = getPaint(Color.DKGRAY, Paint.Align.CENTER, textSizeLegend, true, 0);
        textPaintLegendV = getPaint(Color.DKGRAY, Paint.Align.RIGHT, textSizeLegend, true, 0);

        graphicInitialised = true;
    }

    private void calculateInnerVariable() {
        intervalTotalNumber = (int) Math.ceil(graphicWidth/mSR.getIntervalWidth());
        minutes = (int) Math.floor(intervalTotalNumber*mSR.getIntervalRead()/1000/60);
        seconds = (int) Math.floor(intervalTotalNumber*mSR.getIntervalRead()/1000);
    }

    private Paint getPaint(int color, Paint.Align textAlign, int textSize, boolean antiAlias, float strokeWidth) {
        Paint p = new Paint();
        p.setColor(color);
        p.setTextSize(textSize);
        p.setTextAlign(textAlign);
        p.setAntiAlias(antiAlias);
        p.setStrokeWidth(strokeWidth);
        return p;
    }

    public void setGraphicMode(int graphicMode) {
        this.graphicMode = graphicMode;
    }

    public void setProcessesMode(int processesMode) {
        this.processesMode = processesMode;
    }

    public void setService(ServiceReader sr) {
        mSR = sr;
    }
}
