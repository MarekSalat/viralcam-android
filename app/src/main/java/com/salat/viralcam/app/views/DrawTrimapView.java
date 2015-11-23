package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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

import com.salat.viralcam.app.util.RectHelper;


public class DrawTrimapView extends View {

    public interface DrawTrimapEvents {
        void onDrawStart(DrawTrimapView view);
        void onDrawEnd(DrawTrimapView view);
        void onStateChange(DrawTrimapView view, TrimapDrawState state);
    }

    public DrawTrimapEvents drawTrimapEventsListener;

    public static final int PAINT_STROKE_WIDTH = 8;
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
    private Paint alphaPaint;

    private Matrix matrix = new Matrix();

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
        setDrawTrimapEventsListener(null);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        path = new Path();

        alphaPaint = new Paint();
        alphaPaint.setAlpha(127);

        pathPaint = new Paint();
        pathPaint.setColor(0xFF72549A);
        pathPaint.setAntiAlias(false);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAntiAlias(false);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        foregroundPaint = new Paint();
        foregroundPaint.setColor(Color.WHITE);
        foregroundPaint.setAntiAlias(false);
        foregroundPaint.setStyle(Paint.Style.STROKE);
        foregroundPaint.setStrokeJoin(Paint.Join.ROUND);
        foregroundPaint.setStrokeCap(Paint.Cap.ROUND);
        foregroundPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);

        clearPaint = new Paint();
        clearPaint.setColor(Color.BLACK);
        clearPaint.setAntiAlias(false);
        clearPaint.setDither(false);
        clearPaint.setStyle(Paint.Style.STROKE);
        clearPaint.setStrokeJoin(Paint.Join.ROUND);
        clearPaint.setStrokeCap(Paint.Cap.ROUND);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setStrokeWidth(PAINT_FINAL_TUNING_STROKE_WIDTH);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
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
        pathCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    Rect tmpDrawRect = new Rect();
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.setMatrix(matrix);

        //if(state != TrimapDrawState.INIT)
        canvas.drawBitmap(trimapBitmap, 0, 0, alphaPaint);

        if (drawingPath) {
            tmpDrawRect.set(pathBoundingBox);
            RectHelper.addPadding(tmpDrawRect, (int) pathPaint.getStrokeWidth(), canvas.getWidth(), canvas.getHeight());
            canvas.clipRect(tmpDrawRect);
            canvas.drawBitmap(pathBitmap, 0, 0, alphaPaint);
        }
        canvas.restore();
    }


    Matrix inverseMatrix = new Matrix();
    private boolean drawingPath = false;
    private Rect pathBoundingBox = new Rect();
    private float pathOldX, pathOldY;
    private int pathInitX, pathInitY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        matrix.invert(inverseMatrix);

        pathBoundingBox.left = pathBitmap.getWidth();
        pathBoundingBox.top = pathBitmap.getHeight();
        pathBoundingBox.right = 0;
        pathBoundingBox.bottom = 0;

        path.reset();
        path.moveTo(x, y);
        pathOldX = x;
        pathOldY = y;
        pathInitX = (int) x;
        pathInitY = (int) y;

        drawTrimapEventsListener.onDrawStart(this);
    }

    Rect touchMoveRect = new Rect();
    float []touchMovePoint = {0f, 0f};
    private void touch_move(float x, float y) {
        drawingPath = true;

        if(touchMovePoint[0] < pathBoundingBox.left)
            pathBoundingBox.left = (int) touchMovePoint[0];
        if(touchMovePoint[0] > pathBoundingBox.right)
            pathBoundingBox.right = (int) touchMovePoint[0];
        if(touchMovePoint[1] < pathBoundingBox.top)
            pathBoundingBox.top = (int) touchMovePoint[1];
        if(touchMovePoint[1] > pathBoundingBox.bottom)
            pathBoundingBox.bottom = (int) touchMovePoint[1];

        float dx = Math.abs(x - pathOldX);
        float dy = Math.abs(y - pathOldY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            touchMovePoint[0] = x;
            touchMovePoint[1] = y;
            inverseMatrix.mapPoints(touchMovePoint);

            touchMoveRect.set(
                    (int)Math.min(pathOldX, x),
                    (int)Math.min(pathOldY, y),
                    (int)Math.max(pathOldX, x),
                    (int)Math.max(pathOldY, y)
            );
            RectHelper.addPadding(touchMoveRect, (int) pathPaint.getStrokeWidth(), pathCanvas.getWidth(), pathCanvas.getHeight());

            pathCanvas.save();

            pathCanvas.setMatrix(inverseMatrix);
            pathCanvas.clipRect(touchMoveRect, Region.Op.REPLACE);
            pathCanvas.drawLine(pathOldX, pathOldY, x, y, pathPaint);
            pathCanvas.restore();

            path.lineTo(x, y);
            pathOldX = x;
            pathOldY = y;
        }
    }

    private void touch_up() {
        drawingPath = false;

        pathCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        path.lineTo(pathOldX, pathOldY);
        TrimapDrawState prevState = state;

        trimapCanvas.save();
        trimapCanvas.setMatrix(inverseMatrix);
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

                if(state == TrimapDrawState.TUNING){
                    float []point = {pathInitX, pathInitY};
                    inverseMatrix.mapPoints(point);
                    initPixel = trimapBitmap.getPixel((int)point[0], (int)point[1]);
                }
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
        trimapCanvas.restore();

        // kill this so we don't double draw
        path.reset();
        drawTrimapEventsListener.onDrawEnd(this);
        if(prevState != state)
            drawTrimapEventsListener.onStateChange(this, state);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        if(trimapCanvas == null || state == TrimapDrawState.DONE)
            return false;

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

    public TrimapDrawState getState() {
        return state;
    }

    public Bitmap getTrimapBitmap() {
        return trimapBitmap;
    }

    public void setState(TrimapDrawState newState){
        state = newState;
    }

    public void setDrawTrimapEventsListener(DrawTrimapEvents drawTrimapEventsListener){
        if(drawTrimapEventsListener == null){
            drawTrimapEventsListener = new DrawTrimapEvents() {
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

        this.drawTrimapEventsListener = drawTrimapEventsListener;
    }

    public void setMatrix(Matrix newMatrix){
        if(newMatrix == null)
            throw new NullPointerException();

        this.matrix = newMatrix;
    }

    public Matrix getImageMatrix(){
        return matrix;
    }
}
