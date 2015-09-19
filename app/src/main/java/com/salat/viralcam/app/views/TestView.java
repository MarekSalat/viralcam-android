package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Marek on 02.11.2015.
 */
public class TestView extends View {

    public static final int WIDTH = 200;
    private Bitmap foreground;
    private Bitmap background;
    private Bitmap mask;
    private Bitmap result;

    public TestView(Context context) {
        super(context);
        init();
    }

    public TestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init(){
        result = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888);

        foreground = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888);
        background = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888);
        mask = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ALPHA_8);

        Canvas maskCanvas = new Canvas(mask);
        Paint maskPaint = new Paint();
        maskPaint.setColor(Color.WHITE);
        maskPaint.setStyle(Paint.Style.FILL);
        maskCanvas.drawCircle(WIDTH / 2, WIDTH / 2, WIDTH / 2, maskPaint);

        Canvas foregroundCanvas = new Canvas(foreground);
        foregroundCanvas.drawColor(0xff673AB7); // deep purple

        Canvas backgroundCanvas = new Canvas(background);
        backgroundCanvas.drawColor(0xff9C27B0); // purple

        // more info about porter/duff magic http://ssp.impulsetrain.com/porterduff.html
        Canvas resultCanvas = new Canvas(result);
        Paint tempPaint = new Paint();
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        resultCanvas.drawBitmap(mask, 0, 0, null);
        resultCanvas.drawBitmap(foreground, 0, 0, tempPaint);

        //
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        resultCanvas.drawBitmap(background, 0, 0, tempPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xffCDDC39); // lime
        final int offset = WIDTH + 25;
        int index = 0;
        canvas.drawBitmap(background, offset * index++, 0, null);
        canvas.drawBitmap(foreground, offset * index++, 0, null);
        canvas.drawBitmap(mask, offset * index++, 0, null);
        canvas.drawBitmap(result, offset * index++, 0, null);
    }
}
