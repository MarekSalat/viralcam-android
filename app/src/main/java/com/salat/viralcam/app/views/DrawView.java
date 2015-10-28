package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by Marek on 12.09.2015.
 */
public class DrawView extends View implements ViewTreeObserver.OnPreDrawListener {

    private static final float MINP = 0.25f;
    private static final float MAXP = 0.75f;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;

    public DrawView(Context c) {
        super(c);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF00FF);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        BlurMaskFilter blur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        mPaint.setMaskFilter(blur);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(mCanvas == null || mBitmap == null){
            if(mBitmap != null){
                mBitmap.recycle();
                mBitmap = null;
            }

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xbb000000);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if(mCanvas == null)
            return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public boolean onPreDraw() {
        return false;
    }
}
