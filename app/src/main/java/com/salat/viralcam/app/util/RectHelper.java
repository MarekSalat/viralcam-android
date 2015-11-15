package com.salat.viralcam.app.util;

import android.graphics.Rect;

/**
 * Created by Marek on 15.11.2015.
 */
public class RectHelper {

    public static Rect scale(Rect boundingBox, float scale) {
        Rect res = new Rect();

        res.top = (int) (boundingBox.top * scale);
        res.bottom = (int) (boundingBox.bottom * scale);
        res.left = (int) (boundingBox.left * scale);
        res.right = (int) (boundingBox.right * scale);

        return res;
    }

    public static void addPadding(Rect boundingBox, int padding, int maxWidth, int maxHeight) {
        if (boundingBox.top - padding > 0) boundingBox.top -= padding;
        if (boundingBox.left - padding > 0) boundingBox.left -= padding;
        if (boundingBox.bottom + padding < maxHeight) boundingBox.bottom += padding;
        if (boundingBox.right + padding < maxWidth) boundingBox.right += padding;
    }
}
