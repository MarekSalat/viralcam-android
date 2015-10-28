package com.salat.viralcam.app.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Size;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Constants {
    // 9gag image dimension
    public static final int IMAGE_ALLOWED_WIDTH = 500;
    public static final int IMAGE_ALLOWED_HEIGHT = 500;

    public static final String TEST_IMAGE_PATH = "/storage/emulated/0/Download/cb55df3e9a79d71214e8e0c4c35f565f.1000x750x1.jpg";
    public static final Size NEXUS_7_OPTIMAL_PREVIEW = new Size(800, 600);
}
