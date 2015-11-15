package com.salat.viralcam.app.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Size;

import com.salat.viralcam.app.BuildConfig;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {
    public static enum Flavor {
        Production,
        AlphaBeta,
    }
    public static enum BuildType {
        Release,
        Debug,
    }
    public static final Flavor FLAVOR = BuildConfig.VERSION_NAME.contains("alpha") ||
            BuildConfig.VERSION_NAME.contains("beta") ? Flavor.AlphaBeta : Flavor.Production;

    public static BuildType BUILD_TYPE = BuildConfig.BUILD_TYPE.contains("debug") ?
            BuildType.Debug : BuildType.Release;

    // 9gag image dimension
    public static final int IMAGE_OPTIMAL_WIDTH = 400;
    public static final int IMAGE_OPTIMAL_HEIGHT = 400;

    public static final String TEST_IMAGE_PATH = "/storage/emulated/0/Download/cb55df3e9a79d71214e8e0c4c35f565f.1000x750x1.jpg";
    public static final String TEST_IMAGE2_PATH = "/storage/emulated/0/Download/JPEG_2015_11_01_17_55_45_865.jpg";

    /**
     * If true use only camera api introduces before LOLLIPOP (camera), do not use camera2 api.
     * If false use use whatever api suits more.
     */
    public static final boolean USE_ONLY_LEGACY_CAMERA_API = false;

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT =  new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
}
