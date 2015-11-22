package com.salat.viralcam.app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.util.RectHelper;
import com.salat.viralcam.app.views.DrawTrimapView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class TrimapActivity extends Activity  {
    private static final String TAG = "TrimapActivity";

    public static final float MIN_SCALE_FACTOR = 1f;
    public static final float MAX_SCALE_FACTOR = 5f;

    public static final String INTENT_EXTRA_BACKGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_PATH";
    public static final String INTENT_EXTRA_FOREGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_PATH";
    public static final int BOUNDINGBOX_PADDING = 4;

    private ProgressDialog processDialog;
    private Bitmap foreground;
    private Bitmap background;
    private ScaleGestureDetector scaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_trimap);

        processDialog = new ProgressDialog(this);
        processDialog.setMessage(getString(R.string.loading));
        processDialog.setCancelable(false);
        processDialog.setInverseBackgroundForced(false);

        Intent intent = getIntent();
        String backgroundImagePath = intent.getStringExtra(INTENT_EXTRA_BACKGROUND_IMAGE_PATH);
        String foregroundImagePath = intent.getStringExtra(INTENT_EXTRA_FOREGROUND_IMAGE_PATH);

        foreground = getBitmap(foregroundImagePath, Constants.TEST_IMAGE_PATH);
        background = getBitmap(backgroundImagePath, Constants.TEST_IMAGE2_PATH);

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageBitmap(foreground);

        final FloatingActionButton buttonMagic = (FloatingActionButton) findViewById(R.id.button_magic);
        buttonMagic.hide(false);

        final FloatingActionMenu menuButtons = (FloatingActionMenu) findViewById(R.id.button_editing_menu);
        menuButtons.hideMenu(false);

        final DrawTrimapView drawTrimapView = (DrawTrimapView) findViewById(R.id.drawTrimapView);
        drawTrimapView.setDrawTrimapEventsListener(new DrawTrimapView.DrawTrimapEvents() {
            boolean canShowButtons = false;

            @Override
            public void onDrawStart(DrawTrimapView view) {
                if (!canShowButtons)
                    return;

                buttonMagic.hide(true);
                menuButtons.hideMenu(false);
            }

            @Override
            public void onDrawEnd(DrawTrimapView view) {
                if (!canShowButtons)
                    return;
                buttonMagic.show(true);
                menuButtons.showMenu(false);
            }

            @Override
            public void onStateChange(DrawTrimapView view, DrawTrimapView.TrimapDrawState state) {
                if (canShowButtons || state != DrawTrimapView.TrimapDrawState.TUNING) {
                    return;
                }
                canShowButtons = true;
                buttonMagic.show(true);
                menuButtons.showMenu(false);
                buttonMagic.callOnClick();
            }
        });

        final FloatingActionButton buttonShare = (FloatingActionButton) findViewById(R.id.button_share);
        buttonShare.setVisibility(View.INVISIBLE);

        buttonMagic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDialog.show();

                Thread task = new Thread() {
                    Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
                    final int orientation = getResources().getConfiguration().orientation;

                    @Override
                    public void run() {
                        final Bitmap drawnTrimapBitmap = drawTrimapView.getTrimapBitmap();
                        final float scale = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                                foreground.getHeight() / (float) drawnTrimapBitmap.getHeight() :
                                foreground.getWidth() / (float) drawnTrimapBitmap.getWidth();

                        // for better performance we use just smallest part of the image as possible.
                        final Rect drawnTrimapBoundingBox = new Rect();
                        findBoundingBox(drawnTrimapBitmap, drawnTrimapBoundingBox);
                        RectHelper.addPadding(drawnTrimapBoundingBox, BOUNDINGBOX_PADDING, foreground.getWidth(), foreground.getHeight());
                        final Rect foregroundBoundingBox = RectHelper.scale(drawnTrimapBoundingBox, scale);

                        // create bitmaps for image, trimap and final alpha
                        final Rect foregroundRect = new Rect(0, 0, foregroundBoundingBox.width(), foregroundBoundingBox.height());
                        final Bitmap image = Bitmap.createBitmap(foregroundRect.width(), foregroundRect.height(), Bitmap.Config.ARGB_8888);
                        final Bitmap trimap = Bitmap.createBitmap(foregroundRect.width(), foregroundRect.height(), Bitmap.Config.ARGB_8888);
                        final Bitmap alpha = Bitmap.createBitmap(foregroundRect.width(), foregroundRect.height(), Bitmap.Config.ALPHA_8);

                        // fill image bitmap
                        Canvas imageCanvas = new Canvas(image);
                        imageCanvas.drawBitmap(foreground, foregroundBoundingBox, foregroundRect, bitmapPaint);

                        // fill trimap bitmap
                        Canvas trimapCanvas = new Canvas(trimap);
                        trimapCanvas.drawBitmap(drawnTrimapBitmap, drawnTrimapBoundingBox, foregroundRect, bitmapPaint);

                        // calculate alpha mask
                        calculateAlphaMask(image, trimap, alpha);

                        // Update trimap
                        Canvas drawnTrimapBitmapCanvas = new Canvas(drawnTrimapBitmap);
                        drawnTrimapBitmapCanvas.drawBitmap(trimap, null, drawnTrimapBoundingBox, null);

                        // reuse trimap
                        Canvas blendCanvas = new Canvas(trimap);
                        Paint tempPaint = new Paint();
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                        blendCanvas.drawPaint(tempPaint);

                        // Draw masked foreground. Result is foreground with transparent border.
                        blendCanvas.drawBitmap(alpha, 0, 0, null);
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                        blendCanvas.drawBitmap(image, 0, 0, tempPaint);

                        // free some memory. It is not needed any more
                        image.recycle();
                        alpha.recycle();

                        // draw final image
                        final Bitmap result = background.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas resultCanvas = new Canvas(result);
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                        final float scale2 = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                                background.getHeight() / (float) foreground.getHeight() :
                                background.getWidth() / (float) foreground.getWidth();

                        Rect backgroundRect = RectHelper.scale(foregroundBoundingBox, scale2);
                        resultCanvas.drawBitmap(trimap, null, backgroundRect, tempPaint);

                        trimap.recycle();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // drawTrimapView contains clear matrix with current scale and translation
                                Matrix drawTrimapViewMatrix = drawTrimapView.getImageMatrix();
                                setImageViewBitmapWithMatrix(drawTrimapViewMatrix, imageView, result);

                                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
                                drawTrimapView.setVisibility(View.INVISIBLE);
                                buttonShare.setVisibility(View.VISIBLE);

                                processDialog.hide();
                            }
                        });
                    }
                };
                task.start();
            }
        });

        findViewById(R.id.button_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_BACKGROUND);
            }
        });


        findViewById(R.id.button_foreground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_FOREGROUND);
            }
        });

        findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_UNKNOWN);
            }
        });

        findViewById(R.id.button_clever).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
            }
        });

        menuButtons.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawTrimapView.getState() == DrawTrimapView.TrimapDrawState.DONE) {
                    setImageViewBitmapWithMatrix(drawTrimapView.getImageMatrix(), imageView, foreground);

                    drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
                    drawTrimapView.setVisibility(View.VISIBLE);
                }
                menuButtons.toggle(true);
            }
        });

        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonShareAction(imageView);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            Matrix imageViewMatrix = new Matrix();
            Matrix trimapViewMatrix = new Matrix();

            DrawTrimapView.TrimapDrawState savedState;
            float currentScale = MIN_SCALE_FACTOR;

            float prevX;
            float prevY;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                prevX = detector.getFocusX();
                prevY = detector.getFocusY();

                trimapViewMatrix.set(drawTrimapView.getImageMatrix());
                imageViewMatrix.set(imageView.getImageMatrix());
                if(imageView.getScaleType() != ImageView.ScaleType.MATRIX){
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                }

                savedState = drawTrimapView.getState();
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                final float distX = detector.getFocusX() - prevX;
                final float distY = detector.getFocusY() - prevY;

                currentScale *= detector.getScaleFactor();
                if(currentScale >= MIN_SCALE_FACTOR && currentScale <= MAX_SCALE_FACTOR){
                    imageViewMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                    trimapViewMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                }
                currentScale = Math.max(MIN_SCALE_FACTOR, Math.min(currentScale, MAX_SCALE_FACTOR));
                imageViewMatrix.postTranslate(distX, distY);
                trimapViewMatrix.postTranslate(distX, distY);

                imageView.setImageMatrix(imageViewMatrix);
                drawTrimapView.setMatrix(trimapViewMatrix);

                imageView.invalidate();
                drawTrimapView.invalidate();

                prevX = detector.getFocusX();
                prevY = detector.getFocusY();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                drawTrimapView.setState(savedState);
            }
        });
    }

    private Bitmap getBitmap(String foregroundImagePath, int testImagePath) {
        return foregroundImagePath == null || foregroundImagePath.isEmpty() ?
                BitmapLoader.load(getResources(), testImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT) :
                BitmapLoader.load(foregroundImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
    }

    private void buttonShareAction(final ImageView imageView) {
        processDialog.show();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                SimpleDateFormat date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
                String currentDateTime = date.format(new Date());

                File file = new File(Environment.
                        getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                        "PNG_" + currentDateTime + ".png");

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a loss-less format, the compression factor (100) is ignored
                } catch (Exception e) {
                    Log.e(TAG, "Something get wrong: " + e.toString());
                    Log.e(TAG, "on file: " + file.toString());
                    Toast.makeText(TrimapActivity.this, "Something get wrong: " + e.toString(), Toast.LENGTH_SHORT).show();
                } finally {
                    try {
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    processDialog.hide();

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/png");

                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    startActivity(Intent.createChooser(share, "Share Image"));
                }
            }
        });
    }

    private void setImageViewBitmapWithMatrix(Matrix drawTrimapViewMatrix, ImageView imageView, Bitmap result) {
        // init image to initial position
        imageView.setScaleType(ImageView.ScaleType.FIT_START);
        imageView.setImageBitmap(result);
        imageView.invalidate();
        // transform bitmap so it corresponds to current zoom and drag
        Matrix imageViewMatrix = imageView.getImageMatrix();
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        imageViewMatrix.postConcat(drawTrimapViewMatrix);
        imageView.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        scaleGestureDetector.onTouchEvent(ev);
        return true;
    }


    private native void calculateAlphaMask(Bitmap image, Bitmap trimap, Bitmap outAlpha);
    private native void findBoundingBox(Bitmap trimap, Rect rect);

    static {
        System.loadLibrary("nativeAlphaMatte");
    }
}
