package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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

    public static final int PAINT_STROKE_WIDTH = 9;
    public static final int PAINT_FINAL_TUNING_STROKE_WIDTH = PAINT_STROKE_WIDTH * 6;

    public enum TrimapDrawState {
        INIT,
        TUNING,

        ONLY_FOREGROUND,
        ONLY_BACKGROUND,
        ONLY_UNKNOWN,

        DONE
    }

    private TrimapDrawState state = TrimapDrawState.INIT;

    private Bitmap trimapBitmap;
    private Canvas trimapCanvas;

    private Bitmap pathBitmap;
    private Canvas pathCanvas;

    private Path path;

    private Paint pathPaint;
    private Paint clearPaint;
    private Paint backgroundPaint;
    private Paint foregroundPaint;


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
        path = new Path();

        pathPaint = new Paint();
        pathPaint.setColor(0xFF72549A);
        pathPaint.setAntiAlias(false);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAntiAlias(false);
        backgroundPaint.setDither(true);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        foregroundPaint = new Paint();
        foregroundPaint.setColor(Color.WHITE);
        foregroundPaint.setAntiAlias(false);
        foregroundPaint.setDither(true);
        foregroundPaint.setStyle(Paint.Style.STROKE);
        foregroundPaint.setStrokeJoin(Paint.Join.ROUND);
        foregroundPaint.setStrokeCap(Paint.Cap.ROUND);
        foregroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        clearPaint = new Paint();
        clearPaint.setColor(Color.BLACK);
        clearPaint.setAntiAlias(false);
        clearPaint.setDither(true);
        clearPaint.setStyle(Paint.Style.STROKE);
        clearPaint.setStrokeJoin(Paint.Join.ROUND);
        clearPaint.setStrokeCap(Paint.Cap.ROUND);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(trimapBitmap != null){
            trimapBitmap.recycle();
            trimapBitmap = null;
            trimapCanvas = null;
        }

        trimapBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        trimapCanvas = new Canvas(trimapBitmap);

        if(pathBitmap != null){
            pathBitmap.recycle();
            pathBitmap = null;
            pathCanvas = null;
        }

        pathBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        pathCanvas = new Canvas(pathBitmap);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(trimapBitmap, 0, 0, null);
        canvas.drawBitmap(pathBitmap, 0, 0, null);
    }


    private float pathOldX, pathOldY;
    private int pathInitX, pathInitY;
    private static final float TOUCH_TOLERANCE = 5;

    private void touch_start(float x, float y) {
        path.reset();
        path.moveTo(x, y);
        pathOldX = x;
        pathOldY = y;
        pathInitX = (int) x;
        pathInitY = (int) y;

        listener.onDrawStart(this);
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - pathOldX);
        float dy = Math.abs(y - pathOldY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            pathCanvas.drawLine(pathOldX, pathOldY, x, y, pathPaint);
            path.lineTo(pathOldX, pathOldY);
            pathOldX = x;
            pathOldY = y;
        }
    }

    private void touch_up() {
        pathCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        path.lineTo(pathOldX, pathOldY);
        TrimapDrawState prevState = state;

        switch (state){
            case INIT: {
                path.close();
                foregroundPaint.setStyle(Paint.Style.FILL);

                trimapCanvas.drawColor(backgroundPaint.getColor());
                trimapCanvas.drawPath(path, foregroundPaint);
                trimapCanvas.drawPath(path, clearPaint);

                foregroundPaint.setStyle(Paint.Style.STROKE);
                state = TrimapDrawState.TUNING;

            } break;
            case ONLY_FOREGROUND:
            case ONLY_BACKGROUND:
            case ONLY_UNKNOWN:
            case TUNING:
                int initPixel = 0;

                if(state == TrimapDrawState.TUNING)
                    initPixel = trimapBitmap.getPixel(pathInitX, pathInitY);
                if(state == TrimapDrawState.ONLY_BACKGROUND)
                    initPixel = backgroundPaint.getColor();
                else if(state == TrimapDrawState.ONLY_FOREGROUND)
                    initPixel = foregroundPaint.getColor();
                else if(state == TrimapDrawState.ONLY_UNKNOWN)
                    initPixel = 0;

                // Clear pixel if start was on empty pixel
                if(initPixel == 0)
                    trimapCanvas.drawPath(path, clearPaint);
                // Draw foreground if start was on foreground pixel
                else if(initPixel == foregroundPaint.getColor())
                    trimapCanvas.drawPath(path, foregroundPaint);
                // Draw background other wise
                else if(initPixel == backgroundPaint.getColor())
                    trimapCanvas.drawPath(path, backgroundPaint);
                break;
        }
        // kill this so we don't double draw
        path.reset();
        listener.onDrawEnd(this);
        if(prevState != state)
            listener.onStateChange(this, state);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if(trimapCanvas == null)
            return true;

        if (state == TrimapDrawState.DONE)
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

    public Bitmap getTrimapBitmap() {
        return trimapBitmap;
    }

    public void setState(TrimapDrawState newState){
        if(state == TrimapDrawState.INIT) {
            throw new IllegalArgumentException("You are setting state to early. State cannot be before final tuning stage.");
        }
        if(newState == TrimapDrawState.INIT){
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
