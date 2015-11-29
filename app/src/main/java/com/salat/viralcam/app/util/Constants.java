package com.salat.viralcam.app.util;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;

import com.salat.viralcam.app.BuildConfig;
import com.salat.viralcam.app.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {
    public static final String PACKAGE_NAME = "com.salat.viralcam.app";

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

    /**
     * If true use only camera api introduces before LOLLIPOP (camera), do not use camera2 api.
     * If false use use whatever api suits more.
     */
    public static final boolean USE_ONLY_LEGACY_CAMERA_API = false;
    public static final boolean ALWAYS_SHOW_WHATS_NEW = false;

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT =  new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);

    public static Uri getUriFromResource(Resources resources, int resourceId){
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + resources.getResourcePackageName(resourceId)
                + '/' + resources.getResourceTypeName(resourceId) + '/' + resources.getResourceEntryName(resourceId) );
    }
}
