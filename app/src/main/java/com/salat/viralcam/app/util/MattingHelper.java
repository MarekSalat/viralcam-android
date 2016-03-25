package com.salat.viralcam.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Marek on 22.03.2016.
 */
public class MattingHelper {
    private static final String TAG = "MattingHelper";

    public static Bitmap convertToAlpha8(Bitmap image) {
        Bitmap alpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int value = Color.green(image.getPixel(x, y));
                alpha.setPixel(x, y, Color.argb(value, value, value, value));
            }
        }

        image.recycle();
        image = null;

        return alpha;
    }

    public static Bitmap changeTrimapColors(Bitmap trimap, int originalUnknown, int currentUnknown) {
        Bitmap newTrimap = trimap.copy(Bitmap.Config.ARGB_8888, true);
        trimap.recycle();
        trimap = null;
        newTrimap.setHasAlpha(true);

        for (int x = 0; x < newTrimap.getWidth(); x++) {
            for (int y = 0; y < newTrimap.getHeight(); y++) {
                if(newTrimap.getPixel(x, y) == originalUnknown)
                    newTrimap.setPixel(x, y, currentUnknown);
            }
        }
        return newTrimap;
    }

    public static Bitmap read(String imagePath, Bitmap.Config inPreferredConfig){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        options.inPreferredConfig = inPreferredConfig;

        return BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + imagePath, options);
    }

    public static int calculatePixels(Bitmap trimap, int color) {
        int count = 0;
        for (int x = 0; x < trimap.getWidth(); x++) {
            for (int y = 0; y < trimap.getHeight(); y++) {
                if(trimap.getPixel(x, y) == color)
                    count++;
            }
        }

        return count;
    }

    public static long calculateSE(Bitmap trueAlpha, Bitmap calculatedAlpha) {
        long mse = 0;
        for (int x = 0; x < trueAlpha.getWidth(); x++) {
            for (int y = 0; y < trueAlpha.getHeight(); y++) {
                mse += Math.pow(getDelta(
                        trueAlpha.getPixel(x, y),
                        calculatedAlpha.getPixel(x, y)), 2);
            }
        }

        return mse;
    }

    public static long calculateSAD(Bitmap trueAlpha, Bitmap calculatedAlpha) {
        long sad = 0;
        for (int x = 0; x < trueAlpha.getWidth(); x++) {
            for (int y = 0; y < trueAlpha.getHeight(); y++) {
                sad += Math.abs(getDelta(
                        trueAlpha.getPixel(x, y),
                        calculatedAlpha.getPixel(x, y)));
            }
        }

        return sad;
    }

    public static int getDelta(int x, int y) {
        return (Color.alpha(x)) - Color.alpha(y);
    }

    public static void drawResultImage(Bitmap canvasBitmap, Bitmap image, Bitmap calculatedAlpha) {
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(Color.GREEN);

        Paint tempPaint = new Paint();
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(tempPaint);

        // Draw masked foreground. Result is foreground with transparent border.
        canvas.drawBitmap(calculatedAlpha, 0, 0, null);
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(image, 0, 0, tempPaint);
    }

    public static void saveImage(Bitmap image, String path) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path);

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            (image.getConfig() == Bitmap.Config.ALPHA_8 ? image.copy(Bitmap.Config.ARGB_8888, false) : image)
                    .compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.e(TAG, "Something get wrong: " + e.toString());
            Log.e(TAG, "on file: " + file.toString());
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
