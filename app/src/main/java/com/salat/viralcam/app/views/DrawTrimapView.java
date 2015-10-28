package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;


public class DrawTrimapView extends View implements ViewTreeObserver.OnPreDrawListener {

    public interface DrawTrimapEvents {
        void onDrawStart(DrawTrimapView view);
        void onDrawEnd(DrawTrimapView view);
        void onStateChange(DrawTrimapView view, TrimapDrawState state);
    }
    public DrawTrimapEvents listener;

    public static final int PAINT_STROKE_WIDTH = 12;
    public static final int PAINT_FINAL_TUNING_STROKE_WIDTH = PAINT_STROKE_WIDTH * 6;

    public enum TrimapDrawState {
        RAW_BACKGROUND,
        RAW_FOREGROUND,
        FINAL_TUNING,

        DRAW_FOREGROUND,
        DRAW_BACKGROUND,
        DRAW_UNKNOWN,
    }

    private TrimapDrawState state = TrimapDrawState.RAW_BACKGROUND;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Path mPath;

    private Paint mBitmapPaint;
    private Paint mPaint;
    private Paint mClearPaint;
    private Paint mBackgroundPaint;
    private Paint mForegroundPaint;


    public DrawTrimapView(Context c) {
        super(c);
    }

    public DrawTrimapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawTrimapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setListener(null);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mBitmapPaint.setAlpha(127);

        mPaint = new Paint();
        mPaint.setColor(0xFF72549A);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(PAINT_STROKE_WIDTH);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setDither(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mBackgroundPaint.setStrokeJoin(Paint.Join.ROUND);
        mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        mBackgroundPaint.setStrokeWidth(PAINT_STROKE_WIDTH);

        mForegroundPaint = new Paint();
        mForegroundPaint.setColor(Color.WHITE);
        mForegroundPaint.setAntiAlias(true);
        mForegroundPaint.setDither(true);
        mForegroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mForegroundPaint.setStrokeJoin(Paint.Join.ROUND);
        mForegroundPaint.setStrokeCap(Paint.Cap.ROUND);
        mForegroundPaint.setStrokeWidth(PAINT_STROKE_WIDTH);

        mClearPaint= new Paint();
        mClearPaint.setColor(Color.BLACK);
        mClearPaint.setAntiAlias(true);
        mClearPaint.setDither(true);
        mClearPaint.setStyle(Paint.Style.STROKE);
        mClearPaint.setStrokeJoin(Paint.Join.ROUND);
        mClearPaint.setStrokeCap(Paint.Cap.ROUND);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        //BlurMaskFilter blur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        //mPaint.setMaskFilter(blur);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(mBitmap != null){
            mBitmap.recycle();
            mBitmap = null;
            mCanvas = null;
        }

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void setTrimapDimensions(int width, int height) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    private float mX, mY;
    private int mInitX, mInitY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        mInitX = (int) x;
        mInitY = (int) y;

        listener.onDrawStart(this);
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
        TrimapDrawState prevState = state;

        switch (state){
            case RAW_BACKGROUND: {
                mPath.close();

                // Fill path outside by clipping (difference) an filling rectangle.
                mCanvas.clipPath(mPath, Region.Op.DIFFERENCE);
                mCanvas.drawColor(mBackgroundPaint.getColor());
                mCanvas.clipRect(new Rect(0, 0, mCanvas.getWidth(), mCanvas.getHeight()), Region.Op.REPLACE);

                state = TrimapDrawState.RAW_FOREGROUND;
            } break;
            case RAW_FOREGROUND:
                mPath.close();
                mCanvas.drawPath(mPath, mForegroundPaint);

                mPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);
                mClearPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);
                mBackgroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);
                mForegroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

                mBackgroundPaint.setStyle(Paint.Style.STROKE);
                mForegroundPaint.setStyle(Paint.Style.STROKE);

                state = TrimapDrawState.FINAL_TUNING;
                break;
            case DRAW_FOREGROUND:
            case DRAW_BACKGROUND:
            case DRAW_UNKNOWN:
            case FINAL_TUNING:
                int initPixel = 0;

                if(state == TrimapDrawState.FINAL_TUNING)
                    initPixel = mBitmap.getPixel(mInitX, mInitY);
                if(state == TrimapDrawState.DRAW_BACKGROUND)
                    initPixel = mBackgroundPaint.getColor();
                else if(state == TrimapDrawState.DRAW_FOREGROUND)
                    initPixel = mForegroundPaint.getColor();
                else if(state == TrimapDrawState.DRAW_UNKNOWN)
                    initPixel = 0;

                // Clear pixel if start was on empty pixel
                if(Color.alpha(initPixel) == 0)
                    mCanvas.drawPath(mPath, mClearPaint);
                // Draw foreground if start was on foreground pixel
                else if(Color.alpha(initPixel) > 0 && Color.red(initPixel) > 127)
                    mCanvas.drawPath(mPath, mForegroundPaint);
                // Draw background other wise
                else if(Color.alpha(initPixel) > 0 && Color.red(initPixel) < 127)
                    mCanvas.drawPath(mPath, mBackgroundPaint);
                break;
        }
        // kill this so we don't double draw
        mPath.reset();
        listener.onDrawEnd(this);
        if(prevState != state)
            listener.onStateChange(this, state);
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

    public TrimapDrawState getState() {
        return state;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setState(TrimapDrawState newState){
        if(state == TrimapDrawState.RAW_BACKGROUND || state == TrimapDrawState.RAW_FOREGROUND) {
            throw new IllegalArgumentException("You are setting state to early. State cannot be before final tuning stage.");
        }
        if(newState == TrimapDrawState.RAW_BACKGROUND || newState == TrimapDrawState.RAW_FOREGROUND){
            throw new IllegalArgumentException("State is not allowed to be set");
        }

        state = newState;
    }

    public void setListener(DrawTrimapEvents listener){
        if(listener == null){
            listener = new DrawTrimapEvents() {
                @Override
                public void onDrawStart(DrawTrimapView view) {

                }

                @Override
                public void onDrawEnd(DrawTrimapView view) {

                }

                @Override
                public void onStateChange(DrawTrimapView view, TrimapDrawState state) {

                }
            };
        }

        this.listener = listener;
    }
}
