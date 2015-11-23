package com.salat.viralcam.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Marek on 12.09.2015.
 */
public class ImageWithMask extends View {
    private static final String TAG = "ImageWithMask";


    private Bitmap mBitmap;

    private Bitmap mImage;
    private Bitmap mMask;

    private Paint mMaskPaint;
    private Paint mImagePaint;
    private Rect mBitmapRect;


    public ImageWithMask(Context c) {
        super(c);
    }

    public ImageWithMask(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageWithMask(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mMaskPaint = new Paint();
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mImagePaint = new Paint();
        mImagePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
    }

    public void setImage(Bitmap image){
        if(mImage != null){
            mImage.recycle();
            mImage = null;
        }
        mImage = image;

        final int width = getWidth();
        final int height = getHeight();

        if(height / (double) width > 1)
            mBitmapRect = new Rect(0, 0, width, (int) (width * (image.getHeight() / (double) image.getWidth())));
        else
            mBitmapRect = new Rect(0, 0, (int) (height * (image.getWidth() / (double) image.getHeight())), height);

        if(mMask != null){
            mMask.recycle();
            mMask = null;
        }

        mMask = Bitmap.createBitmap(mBitmapRect.width(), mBitmapRect.height(), Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(mMask);
        //Paint paint = new Paint();
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        //paint.setMaskFilter(new BlurMaskFilter(1024, BlurMaskFilter.Blur.NORMAL));

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setShader(new RadialGradient(mBitmapRect.width() / 2, mBitmapRect.height() / 2,
                    mBitmapRect.height() / 3, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.MIRROR));

        canvas.drawCircle(mBitmapRect.width() / 2, mBitmapRect.height() / 2, mBitmapRect.height() / 3, paint);
    }

    @Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if(mBitmap != null){
            mBitmap.recycle();
            mBitmap = null;
        }

        newWidth /= 2;
        newHeight /= 2;

        mBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        Log.e(TAG, "Foo FOo [" +newWidth + ", " + newHeight + "]");

        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        if(mImage == null || mMask == null)
            return;

        canvas.save();
        canvas.drawBitmap(mImage, null, mBitmapRect, mImagePaint);
        canvas.drawBitmap(mMask, null, mBitmapRect, mMaskPaint);
        canvas.restore();
    }
}
