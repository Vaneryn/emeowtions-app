package com.example.emeowtions.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class OverlayView extends View {

    private List<BoundingBox> results;
    private Paint boxPaint;
    private Paint textBackgroundPaint;
    private Paint textPaint;
    private Rect bounds;

    private static final int BOUNDING_RECT_TEXT_PADDING = 8;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints(context);
        results = new java.util.LinkedList<>();
    }

    private void initPaints(Context context) {
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.BLACK);
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setTextSize(50f);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50f);

        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStrokeWidth(8F);
        boxPaint.setStyle(Paint.Style.STROKE);

        bounds = new Rect();
    }

    public void clear() {
        results.clear();
        textPaint.reset();
        textBackgroundPaint.reset();
        boxPaint.reset();
        invalidate();
        initPaints(getContext());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (BoundingBox box : results) {
            float left = box.getX1() * getWidth();
            float top = box.getY1() * getHeight();
            float right = box.getX2() * getWidth();
            float bottom = box.getY2() * getHeight();

            canvas.drawRect(left, top, right, bottom, boxPaint);

            String drawableText = box.getClsName();
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length(), bounds);
            int textWidth = bounds.width();
            int textHeight = bounds.height();

            canvas.drawRect(left, top, left + textWidth + BOUNDING_RECT_TEXT_PADDING, top + textHeight + BOUNDING_RECT_TEXT_PADDING, textBackgroundPaint);
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint);
        }
    }

    public void setResults(List<BoundingBox> boundingBoxes) {
        this.results = boundingBoxes;
        invalidate();
    }
}
