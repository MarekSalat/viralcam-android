package com.salat.viralcam.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

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
@SuppressWarnings("ConstantConditions")
public class TrimapActivity extends AppCompatActivity {
    private static final String TAG = "TrimapActivity";

    public static final float MIN_SCALE_FACTOR = 1f;
    public static final float MAX_SCALE_FACTOR = 5f;

    public static final String INTENT_EXTRA_BACKGROUND_IMAGE_URI = "TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_URI";
    public static final String INTENT_EXTRA_FOREGROUND_IMAGE_URI = "TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_URI";
    public static final int BOUNDINGBOX_PADDING = 4;
    private View layout;

    private ProgressDialog processDialog;
    private Bitmap foreground;
    private Bitmap background;
    private ScaleGestureDetector scaleGestureDetector;
    private ImageView imageView;
    private Uri foregroundUri;
    private Uri backgroundUri;
    private Snackbar markEdgeSnackbar;
    private DrawTrimapView drawTrimapView;
    private View buttonDone;
    private View buttonEdit;
    private View brushGroup;
    private View editDoneGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trimap);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        processDialog = new ProgressDialog(this);
        processDialog.setMessage(getString(R.string.loading));
        processDialog.setCancelable(false);
        processDialog.setInverseBackgroundForced(false);

        Intent intent = getIntent();
        String foregroundUriString = intent.getStringExtra(INTENT_EXTRA_FOREGROUND_IMAGE_URI);
        String backgroundUriString = intent.getStringExtra(INTENT_EXTRA_BACKGROUND_IMAGE_URI);

        Uri defaultForegroundUri = Constants.getUriFromResource(getResources(), R.raw.pizza_tower);
        Uri defaultBackgroundUri = Constants.getUriFromResource(getResources(), R.raw.panda);

        foregroundUri = foregroundUriString == null || foregroundUriString.isEmpty() ? defaultForegroundUri : Uri.parse(foregroundUriString);
        backgroundUri = backgroundUriString == null || backgroundUriString.isEmpty() ? defaultBackgroundUri : Uri.parse(backgroundUriString);

        foreground = getBitmap(foregroundUri, defaultForegroundUri);
        background = getBitmap(backgroundUri, defaultBackgroundUri);

        if(foreground == null || background == null){
            Toast.makeText(this, "Cannot load images", Toast.LENGTH_SHORT).show();
            finish();
        }

        imageView = (ImageView) findViewById(R.id.image_view);
        imageView.setImageBitmap(foreground);

        buttonDone = findViewById(R.id.button_done);
        buttonDone.setVisibility(View.GONE);

        buttonEdit = findViewById(R.id.button_edit);
        buttonEdit.setVisibility(View.GONE);

        brushGroup = findViewById(R.id.brush_group);
        brushGroup.setVisibility(View.GONE);

        editDoneGroup = findViewById(R.id.edit_done_group);

        // todo: In the future the action could be "NEVER SHOW AGAIN"
        layout = findViewById(R.id.trimap_coordinator_layout);
        markEdgeSnackbar = Snackbar.make(
                layout,
                "Roughly mark object edge. You don't have to precise, it can be tuned later.",
                Snackbar.LENGTH_INDEFINITE
        ).setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        drawTrimapView = (DrawTrimapView) findViewById(R.id.drawTrimapView);
        drawTrimapView.setDrawTrimapEventsListener(new DrawTrimapView.DrawTrimapEvents() {
            boolean canShowButtons = false;

            @Override
            public void onDrawStart(DrawTrimapView view) {
                markEdgeSnackbar.dismiss();
                if (!canShowButtons)
                    return;
                toolbar.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_up_from_top));
                brushGroup.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_down_from_bottom));
                editDoneGroup.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_down_from_bottom));
            }

            @Override
            public void onDrawEnd(DrawTrimapView view) {
                if (!canShowButtons)
                    return;
                toolbar.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_down_from_top));
                brushGroup.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_up_from_bottom));
                editDoneGroup.startAnimation(AnimationUtils.loadAnimation(TrimapActivity.this, R.anim.slide_up_from_bottom));
            }

            @Override
            public void onStateChange(DrawTrimapView view, DrawTrimapView.TrimapDrawState state) {
                if (canShowButtons || state != DrawTrimapView.TrimapDrawState.TUNING) {
                    return;
                }
                canShowButtons = true;

                buttonDone.callOnClick();
            }
        });

        buttonDone.setOnClickListener(new View.OnClickListener() {
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

                        if (foregroundBoundingBox.isEmpty() ||
                                foregroundBoundingBox.width() <= 0 ||
                                foregroundBoundingBox.height() <= 0 ||
                                (foregroundBoundingBox.width() < 32 && foregroundBoundingBox.height() < 32)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // drawTrimapView contains clear matrix with current scale and translation
                                    Matrix drawTrimapViewMatrix = drawTrimapView.getImageMatrix();
                                    setImageViewBitmapWithMatrix(drawTrimapViewMatrix, imageView, background);

                                    drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
                                    drawTrimapView.setVisibility(View.INVISIBLE);
//                                    buttonShare.setVisibility(View.VISIBLE);

                                    processDialog.hide();
                                }
                            });
                            return;
                        }

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

                                prepareShowResultUI();
                            }
                        });
                    }
                };
                task.start();
            }
        });

        findViewById(R.id.button_brush_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_BACKGROUND);
            }
        });

        findViewById(R.id.button_brush_foreground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_FOREGROUND);
            }
        });

        findViewById(R.id.button_brush_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.ONLY_UNKNOWN);
            }
        });

        findViewById(R.id.button_brush_magic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
            }
        });

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawTrimapView.getState() == DrawTrimapView.TrimapDrawState.DONE) {
                    setImageViewBitmapWithMatrix(drawTrimapView.getImageMatrix(), imageView, foreground);

                    drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
                    drawTrimapView.setVisibility(View.VISIBLE);
                }

                buttonEdit.setVisibility(View.GONE);
                buttonDone.setVisibility(View.VISIBLE);
                brushGroup.setVisibility(View.VISIBLE);
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

    private void prepareShowResultUI() {
        drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
        drawTrimapView.setVisibility(View.INVISIBLE);
//      buttonShare.setVisibility(View.VISIBLE);

        processDialog.hide();

        brushGroup.clearAnimation();
        brushGroup.setVisibility(View.GONE);
        buttonEdit.setVisibility(View.VISIBLE);
        buttonDone.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        markEdgeSnackbar.show();
    }

    @Override
    protected void onPause() {
        markEdgeSnackbar.dismiss();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : 0));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Snackbar.make(layout, "Clicked at '" + item.getTitle() + "' [" + id + "]", Snackbar.LENGTH_SHORT).show();

        if (id == android.R.id.home){
            if(drawTrimapView.getState() == DrawTrimapView.TrimapDrawState.TUNING)
                super.onBackPressed();
            else
                prepareShowResultUI();
            return true;
        }

        if (id == R.id.action_share) {
            shareAction((ImageView) findViewById(R.id.image_view));
            return true;
        }

        if(id == R.id.action_swap_images){
            swapImagesAction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void swapImagesAction() {
        Intent intent = new Intent(this, TrimapActivity.class);
        intent.putExtra(INTENT_EXTRA_FOREGROUND_IMAGE_URI, backgroundUri.toString());
        intent.putExtra(INTENT_EXTRA_BACKGROUND_IMAGE_URI, foregroundUri.toString());
        startActivity(intent);
    }

    private Bitmap getBitmap(Uri imageUri, Uri defaultImageUri) {
        Bitmap result = null;
        try {
            result = BitmapLoader.load(getContentResolver(), imageUri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
        } catch (IOException e) {
            try {
                result = BitmapLoader.load(getContentResolver(), defaultImageUri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
            } catch (IOException e1) {
                Log.e(TAG, e.toString());
            }
        }
        return result;
    }

    private void shareAction(final ImageView imageView) {
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
