#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include <vector>
#include "globalmatting.h"

using namespace std;

#define  LOG_TAG    "matte"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,__VA_ARGS__)

/* Set to 1 to enable debug log traces. */
#define DEBUG 0


extern "C" {
JNIEXPORT void JNICALL Java_com_salat_viralcam_app_activities_TrimapActivity_calculateAlphaMask(
                                         JNIEnv *env,
                                         jobject instance,
                                         jobject image,
                                         jobject trimap,
                                         jobject outAlpha) {
        int returned;
        AndroidBitmapInfo imageInfo;
        AndroidBitmapInfo trimapInfo;
        AndroidBitmapInfo outAlphaInfo;

        if ((returned = AndroidBitmap_getInfo(env, image, &imageInfo)) < 0 ||
            (returned = AndroidBitmap_getInfo(env, trimap, &trimapInfo)) < 0 ||
            (returned = AndroidBitmap_getInfo(env, outAlpha, &outAlphaInfo)) < 0) {
            LOGE("AndroidBitmap_getInfo() failed ! error=%d", returned);
            return;
        }

        if (imageInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("[image] Bitmap format is not RGBA_8888 !");
            return;
        }

        if (trimapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("[trimap] Bitmap format is not RGBA_8888 !");
            return;
        }

        if (outAlphaInfo.format != ANDROID_BITMAP_FORMAT_A_8) {
            LOGE("[outAlpha] Bitmap format is not FORMAT_A_8 !");
            return;
        }

        if (imageInfo.height != trimapInfo.height || imageInfo.height != outAlphaInfo.height ||
                                                     imageInfo.width != trimapInfo.width || imageInfo.width != outAlphaInfo.width) {
            LOGE("Dimensions does not match");
            return;
        }

        void *imagePixels;
        void *trimapPixels;
        void *outAlphaPixels;

        if ((returned = AndroidBitmap_lockPixels(env, image, &imagePixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", returned);
            return;
        }

        if ((returned = AndroidBitmap_lockPixels(env, trimap, &trimapPixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", returned);
            AndroidBitmap_unlockPixels(env, outAlpha);
            return;
        }

        if ((returned = AndroidBitmap_lockPixels(env, outAlpha, &outAlphaPixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", returned);
            AndroidBitmap_unlockPixels(env, outAlpha);
            AndroidBitmap_unlockPixels(env, trimap);
            return;
        }

        // do magic here
        Bitmap imageBitmap(imageInfo.width, imageInfo.height, imageInfo.stride, (uint32_t *) imagePixels);
        Bitmap trimapBitmap(trimapInfo.width, trimapInfo.height, trimapInfo.stride, (uint32_t *) trimapPixels);
        MaskBitmap outAlphaMaskBitmap(outAlphaInfo.width, outAlphaInfo.height, outAlphaInfo.stride, (uint8_t *) outAlphaPixels);

        expansionOfKnownRegions(imageBitmap, trimapBitmap);
        globalMatting(imageBitmap, trimapBitmap, outAlphaMaskBitmap);

        AndroidBitmap_unlockPixels(env, outAlpha);
        AndroidBitmap_unlockPixels(env, trimap);
        AndroidBitmap_unlockPixels(env, image);
    }


    JNIEXPORT void JNICALL Java_com_salat_viralcam_app_activities_TrimapActivity_findBoundingBox(JNIEnv *env, jobject instance,
                                                                          jobject trimap,
                                                                          jobject rect) {

        int returned;
        AndroidBitmapInfo trimapInfo;
        if ((returned = AndroidBitmap_getInfo(env, trimap, &trimapInfo))) {
            LOGE("AndroidBitmap_getInfo() failed ! error=%d", returned);
            return;
        }

        if (trimapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("[image] Bitmap format is not RGBA_8888 !");
            return;
        }

        void *trimapPixels;
        if ((returned = AndroidBitmap_lockPixels(env, trimap, &trimapPixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", returned);
            return;
        }

        Bitmap trimapBitmap(trimapInfo.width, trimapInfo.height, trimapInfo.stride, (uint32_t *) trimapPixels);

        int left = trimapBitmap.width;
        int top = trimapBitmap.height;
        int right = 0;
        int bottom = 0;

        // fixme: something is wrong here
        for (int y = 0; y < trimapBitmap.height; ++y) {
            for (int x = 0; x < trimapBitmap.width; ++x) {
                if(trimapBitmap(y, x) == TRIMAP_UNKNOWN || trimapBitmap(y, x) == TRIMAP_FOREGROUND){
                    if(x < left)
                        left = x;
                    if(x > right)
                        right = x;
                    if(y < top)
                        top = y;
                    if(y > bottom)
                        bottom = y;
                }
            }
        }
        AndroidBitmap_unlockPixels(env, trimap);

        jclass Rect = env->GetObjectClass(rect);
        jfieldID Rect_top_id = env->GetFieldID(Rect, "top", "I");
        jfieldID Rect_bottom_id = env->GetFieldID(Rect, "bottom", "I");
        jfieldID Rect_left_id = env->GetFieldID(Rect, "left", "I");
        jfieldID Rect_right_id = env->GetFieldID(Rect, "right", "I");

        env->SetIntField(rect, Rect_top_id, top);
        env->SetIntField(rect, Rect_bottom_id, bottom);
        env->SetIntField(rect, Rect_left_id, left);
        env->SetIntField(rect, Rect_right_id, right);
    }
}