package com.salat.viralcam.app.util;


import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapLoader {
    private static final String TAG = "BitmapLoader";

    public static int calculateInSampleSize(  BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap load(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        Log.e(TAG, path +
                " [" + options.outWidth / options.inSampleSize + ", " + options.outHeight / options.inSampleSize + "]" +
                " (" + options.inSampleSize + ")");


        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }


    public static Bitmap load(Resources resources, int resourceId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resourceId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        Log.e(TAG, resourceId +
                " [" + options.outWidth / options.inSampleSize + ", " + options.outHeight / options.inSampleSize + "]" +
                " (" + options.inSampleSize + ")");


        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resourceId, options);
    }


    public static Bitmap load(ContentResolver resolver, Uri imageUri, int reqWidth, int reqHeight) throws IOException {
        InputStream imageStream = resolver.openInputStream(imageUri);
        if(imageStream == null)
            throw new IOException("Cannot open stream for " + imageUri.toString());

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(imageStream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        Log.e(TAG, imageUri.toString() +
                " [" + options.outWidth / options.inSampleSize + ", " + options.outHeight / options.inSampleSize + "]" +
                " (" + options.inSampleSize + ")");

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream.close();
        imageStream = resolver.openInputStream(imageUri);
        if(imageStream == null)
            throw new IOException("Cannot open stream for " + imageUri.toString());

        Bitmap result = BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        if(result == null)
            throw new FileNotFoundException("Cannot found " + imageUri.toString() + ". Result has been null.");

        return result;
    }
}
