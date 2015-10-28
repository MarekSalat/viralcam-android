package com.salat.viralcam.app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.views.DrawTrimapView;

public class TrimapActivity extends Activity {

    public static final String INTENT_EXTRA_BACKGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_PATH";
    public static final String INTENT_EXTRA_FOREGROUND_IMAGE_PATH = "TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_PATH";

    private ProgressDialog processDialog;

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

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageURI(Uri.parse(backgroundImagePath));

        final FloatingActionButton buttonMagic = (FloatingActionButton) findViewById(R.id.button_magic);
        buttonMagic.hide(false);

        final FloatingActionMenu menuButtons = (FloatingActionMenu) findViewById(R.id.menu3);
        menuButtons.hideMenu(false);

        final DrawTrimapView drawTrimapView = (DrawTrimapView) findViewById(R.id.drawTrimapView);
        drawTrimapView.setListener(new DrawTrimapView.DrawTrimapEvents() {
            boolean canShow = false;

            @Override
            public void onDrawStart(DrawTrimapView view) {
                if (!canShow)
                    return;

                buttonMagic.hide(true);
                menuButtons.hideMenu(false);
            }

            @Override
            public void onDrawEnd(DrawTrimapView view) {
                if (!canShow)
                    return;
                buttonMagic.show(true);
                menuButtons.showMenu(false);
            }

            @Override
            public void onStateChange(DrawTrimapView view, DrawTrimapView.TrimapDrawState state) {
                if (canShow || state != DrawTrimapView.TrimapDrawState.FINAL_TUNING) {
                    return;
                }
                canShow = true;
                buttonMagic.show(true);
                menuButtons.showMenu(false);
            }
        });

        buttonMagic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                processDialog.show();

                Thread task = new Thread(){
                    @Override
                    public void run()
                    {
                        Rect boundingBox = new Rect();

                        Bitmap drawnTrimapBitmap =  drawTrimapView.getBitmap();
                        Bitmap imageBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                        final Bitmap trimapBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                        Bitmap alpha = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ALPHA_8);

                        findBoundingBox(trimapBitmap, boundingBox);

                        Canvas canvas = new Canvas(trimapBitmap);
                        double scale = trimapBitmap.getHeight() / (double) drawnTrimapBitmap.getHeight();
                        int width =  (int)(drawnTrimapBitmap.getWidth()*scale);
                        int height =  (int)(drawnTrimapBitmap.getHeight()*scale);
                        Paint paint = new Paint();
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                        canvas.drawBitmap(drawnTrimapBitmap, null, new Rect(0, 0, width, height), paint);

                        calculateAlphaMask(imageBitmap, trimapBitmap, alpha);

                        Paint maskPaint = new Paint();
                        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

                        Paint imagePaint = new Paint();
                        imagePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

                        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                        canvas.drawBitmap(imageBitmap, 0, 0, imagePaint);
                        canvas.drawBitmap(alpha, 0, 0, maskPaint);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(trimapBitmap);
                                drawTrimapView.setVisibility(View.INVISIBLE);

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
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DRAW_BACKGROUND);
            }
        });


        findViewById(R.id.button_foreground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DRAW_FOREGROUND);
            }
        });

        findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DRAW_UNKNOWN);
            }
        });

        findViewById(R.id.button_clever).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawTrimapView.setState(DrawTrimapView.TrimapDrawState.FINAL_TUNING);
            }
        });
    }

    private native void calculateAlphaMask(Bitmap image, Bitmap trimap, Bitmap outAlpha);
    private native void findBoundingBox(Bitmap trimap, Rect rect);

    static {
        System.loadLibrary("nativeAlphaMatte");
    }
}
