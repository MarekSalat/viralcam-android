package com.salat.viralcam.app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import com.salat.viralcam.app.views.DrawTrimapView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrimapActivity extends Activity {
    private final String TAG = "TrimapActivity";

    public static final String INTENT_EXTRA_BACKGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_PATH";
    public static final String INTENT_EXTRA_FOREGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_PATH";
    public static final int BOUNDINGBOX_PADDING = 16;

    private ProgressDialog processDialog;
    private Bitmap foreground;
    private Bitmap background;

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

        if(backgroundImagePath == null || backgroundImagePath.isEmpty())
            backgroundImagePath = Constants.TEST_IMAGE2_PATH;
        if(foregroundImagePath == null || foregroundImagePath.isEmpty())
            foregroundImagePath = Constants.TEST_IMAGE_PATH;

        foreground = BitmapLoader.load(foregroundImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
        background = BitmapLoader.load(backgroundImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(foreground);

        imageView.setImageURI(Uri.parse(foregroundImagePath));

        final FloatingActionButton buttonMagic = (FloatingActionButton) findViewById(R.id.button_magic);
        buttonMagic.hide(false);

        final FloatingActionMenu menuButtons = (FloatingActionMenu) findViewById(R.id.button_editing_menu);
        menuButtons.hideMenu(false);

        final DrawTrimapView drawTrimapView = (DrawTrimapView) findViewById(R.id.drawTrimapView);
        drawTrimapView.setListener(new DrawTrimapView.DrawTrimapEvents() {
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

                    @Override
                    public void run() {
                        final Bitmap drawnTrimapBitmap = drawTrimapView.getTrimapBitmap();
                        final float scale = foreground.getHeight() / (float) drawnTrimapBitmap.getHeight();

                        // for better performance we use just smallest part of the image as possible.
                        final Rect drawnTrimapBoundingBox = new Rect();
                        addPadding(drawnTrimapBoundingBox, BOUNDINGBOX_PADDING, foreground.getWidth(), foreground.getHeight());
                        findBoundingBox(drawnTrimapBitmap, drawnTrimapBoundingBox);
                        final Rect foregroundBoundingBox = scaleRect(drawnTrimapBoundingBox, scale);

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

                        // reuse trimap
                        Canvas blendCanvas = new Canvas(trimap);
                        Paint tempPaint = new Paint();
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                        blendCanvas.drawPaint(tempPaint);

                        // Draw masked foreground. Result is foreground with transparent border.
                        blendCanvas.drawBitmap(alpha, 0, 0, null);
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                        blendCanvas.drawBitmap(image, 0, 0, tempPaint);

                        // free some memory. It not needed any more
                        image.recycle();
                        alpha.recycle();

                        // draw final image
                        final Bitmap result = background.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas resultCanvas = new Canvas(result);
                        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                        final float scale2 = background.getHeight() / (float) foreground.getHeight();
                        Rect backgroundRect = scaleRect(foregroundBoundingBox, scale2);
                        resultCanvas.drawBitmap(trimap, null, backgroundRect, tempPaint);

                        trimap.recycle();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(result);
                                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
                                drawTrimapView.setVisibility(View.INVISIBLE);
                                buttonShare.setVisibility(View.VISIBLE);

                                processDialog.hide();
                            }
                        });
                    }

                    private Rect scaleRect(Rect boundingBox, float scale) {
                        Rect res = new Rect();

                        res.top = (int) (boundingBox.top * scale);
                        res.bottom = (int) (boundingBox.bottom * scale);
                        res.left = (int) (boundingBox.left * scale);
                        res.right = (int) (boundingBox.right * scale);

                        return res;
                    }

                    private void addPadding(Rect boundingBox, int padding, int maxWidth, int maxHeight) {
                        if (boundingBox.top - padding > 0) boundingBox.top -= padding;
                        if (boundingBox.left - padding > 0) boundingBox.left -= padding;
                        if (boundingBox.bottom + padding < maxHeight) boundingBox.bottom += padding;
                        if (boundingBox.right + padding < maxWidth) boundingBox.right += padding;
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
                    drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
                    drawTrimapView.setVisibility(View.VISIBLE);
                }
                menuButtons.toggle(true);
            }
        });

        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });
    }

    private native void calculateAlphaMask(Bitmap image, Bitmap trimap, Bitmap outAlpha);
    private native void findBoundingBox(Bitmap trimap, Rect rect);

    static {
        System.loadLibrary("nativeAlphaMatte");
    }
}
