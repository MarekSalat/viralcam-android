package com.salat.viralcam.app.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.salat.viralcam.app.AnalyticsTrackers;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraLollipopFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.views.ImageWithMask;

import java.io.IOException;

import fragments.CameraFragment;
import fragments.CameraOldVersionsFragment;

@SuppressWarnings("ConstantConditions")
public class CaptureSceneActivity extends AppCompatActivity {

    private static final String TAG = "CaptureSceneActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final String KEY_BACKGROUND_URI = "CaptureSceneActivity.KEY_BACKGROUND_URI";
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private Uri imageUri;
    private AlertDialog dialog;
    private GestureDetectorCompat mDetector;

    private static final String KEY_USE_REAR_CAMERA = "CaptureSceneActivity.KEY_USE_REAR_CAMERA";
    private boolean mUseRear = true;
    private Bitmap imageViewBitmap;
    private Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        onWindowFocusChanged(true);

        super.onCreate(savedInstanceState);

        AnalyticsTrackers.initialize(this);
        tracker = AnalyticsTrackers.tracker().get(AnalyticsTrackers.Target.APP);

        setContentView(R.layout.activity_capture_scene);

        if(savedInstanceState != null)
            mUseRear = savedInstanceState.getBoolean(KEY_USE_REAR_CAMERA, true);

        if(savedInstanceState != null && savedInstanceState.getString(KEY_BACKGROUND_URI) != null && !savedInstanceState.getString(KEY_BACKGROUND_URI).isEmpty()){
            imageUri =  Uri.parse(savedInstanceState.getString(KEY_BACKGROUND_URI));

            if(isImageSelected()){
                FrameLayout layout = (FrameLayout) findViewById(R.id.container);
                layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    boolean isImageSet = false;
                    @Override
                    public void onGlobalLayout() {
                        if(!isImageSet)
                            setImageViewBackground(imageUri);

                        isImageSet = true;
                    }
                });
            }
        }

        createAndReplaceCameraFragment(mUseRear);

        final ImageWithMask imageView = (ImageWithMask) findViewById(R.id.image_view);

        final View takePictureButton = findViewById(R.id.take_picture_button);
        if(!isImageSelected())
            takePictureButton.setVisibility(View.INVISIBLE);

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logTakePictureAction();

                FragmentManager fm = getFragmentManager();
                Fragment cameraFragment = fm.findFragmentByTag(CAMERA_FRAGMENT);

                Animation rotation = AnimationUtils.loadAnimation(CaptureSceneActivity.this, R.anim.clockwise_rotation);
                rotation.setRepeatCount(Animation.INFINITE);
                takePictureButton.startAnimation(rotation);

                ((CameraFragment) cameraFragment).takePicture(new CameraLollipopFragment.OnCaptureCompleted() {

                    @Override
                    public void onCaptureComplete(String foregroundImagePath, Uri uri) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                takePictureButton.clearAnimation();
                            }
                        });
                        if (foregroundImagePath == null || uri == null) {
                            return;
                        }

                        Intent intent = new Intent(CaptureSceneActivity.this, TrimapActivity.class);
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_URI, uri.toString());
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_URI, imageUri.toString());
                        startActivity(intent);
                    }
                });
            }
        });

        final View selectBackgroundButton = findViewById(R.id.select_image_button);
        selectBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setCancelable(true);
                dialog.show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_background);
        builder.setCancelable(false);
        builder.setItems(R.array.images, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                takePictureButton.setVisibility(View.VISIBLE);
                int resourceId = R.raw.eiffel_tower_port;

                switch (which){
                    case 0:  resourceId = R.raw.eiffel_tower_port; break;
//                    case 1:  resourceId = R.raw.great_wall_of_china_land; break;
                    case 1:  resourceId = R.raw.great_wall_of_china_port; break;
                    case 2:  resourceId = R.raw.machu_picchu_land; break;
//                    case 4:  resourceId = R.raw.machu_picchu_port; break;
                    case 3:  resourceId = R.raw.pisa_land; break;
                    case 4:  resourceId = R.raw.pisa_port; break;

                    default:{
                        logSelectBackground(AnalyticsTrackers.Label.CustomBackground);

                        Intent intent = new Intent();
                        // Show only images, no videos or anything else
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        // Always show the chooser (if there are multiple options available)
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                        return;
                    }
                }
                logSelectBackground(AnalyticsTrackers.Label.PredefinedBackground);

                imageUri = Constants.getUriFromResource(getResources(), resourceId);
                setImageViewBackground(imageUri);
            }
        });

        mDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float alpha = imageView.getAlpha();
                alpha += distanceX / 100;
                imageView.setAlpha(Math.max(0.2f, Math.min(0.95f, alpha)));
                return true;
            }
        });


        final View swapCamera = findViewById(R.id.swap_camera);
        final ImageView swapCameraImageView = (ImageView) findViewById(R.id.swap_camera_image);

        if(!mUseRear)
            swapCameraImageView.setImageResource(R.drawable.ic_camera_rear_white_48dp);

        if(Constants.USE_ONLY_LEGACY_CAMERA_API || Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP){
            swapCamera.setVisibility(View.GONE);
        } else {
            swapCamera.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mUseRear = !mUseRear;

                    logSwapCameraAction();

                    if(mUseRear){
                        swapCamera.animate().scaleX(0).setDuration(125).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                swapCameraImageView.setImageResource(R.drawable.ic_camera_front_white_48dp);
                                swapCamera.animate().scaleX(1).start();
                            }
                        }).start();
                        invertImageView(imageView, 1);
                    }
                    else{
                        swapCamera.animate().scaleX(0).setDuration(125).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                swapCameraImageView.setImageResource(R.drawable.ic_camera_rear_white_48dp);
                                swapCamera.animate().scaleX(1).start();
                            }
                        }).start();
                        invertImageView(imageView, -1);
                    }

                    createAndReplaceCameraFragment(mUseRear);
                }
            });
        }

        dialog = builder.create();
    }

    private void logSelectBackground(AnalyticsTrackers.Label label) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(AnalyticsTrackers.Category.Action.toString())
                .setAction(AnalyticsTrackers.Action.SelectBackground.toString())
                .setLabel(label.toString())
                .setValue(1)
                .build());
    }

    private void logSwapCameraAction() {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(AnalyticsTrackers.Category.Action.toString())
                .setAction(AnalyticsTrackers.Action.SwapCamera.toString())
                .setLabel((mUseRear
                        ? AnalyticsTrackers.Label.UseRearCamera
                        : AnalyticsTrackers.Label.UseFrontCamera).toString())
                .setValue(1)
                .build());
    }

    private void logTakePictureAction() {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(AnalyticsTrackers.Category.Action.toString())
                .setAction(AnalyticsTrackers.Action.TakePicture.toString())
                .setLabel((mUseRear
                        ? AnalyticsTrackers.Label.UsingRearCamera
                        : AnalyticsTrackers.Label.UsingFrontCamera).toString())
                .setValue(1)
                .build());

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(AnalyticsTrackers.Category.State.toString())
                .setAction(AnalyticsTrackers.Action.TakePicture.toString())
                .setLabel((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? AnalyticsTrackers.Label.LandscapeMode
                        : AnalyticsTrackers.Label.PortraitMode).toString())
                .setValue(1)
                .build());
    }

    @NonNull
    private void createAndReplaceCameraFragment(boolean useRearCamera) {
        FragmentManager fm = getFragmentManager();
        Fragment cameraFragment = fm.findFragmentByTag(CAMERA_FRAGMENT);

        if(cameraFragment != null){
            fm.beginTransaction()
                    .remove(cameraFragment)
                    .commit();
        }

        if(Constants.USE_ONLY_LEGACY_CAMERA_API || Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP)
            cameraFragment = CameraOldVersionsFragment.newInstance();
        else
            cameraFragment = CameraLollipopFragment.newInstance(useRearCamera);

        cameraFragment.setRetainInstance(true);
        fm.beginTransaction()
                .replace(R.id.container, cameraFragment, CAMERA_FRAGMENT)
                .commit();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isImageSelected()){
            dialog.show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        imageUri = uri;

        setImageViewBackground(uri);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if(isImageSelected())
            outState.putString(KEY_BACKGROUND_URI, imageUri.toString());
        outState.putBoolean(KEY_USE_REAR_CAMERA, mUseRear);
        super.onSaveInstanceState(outState);
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

    private void setImageViewBackground(final Uri uri){
        if(uri == null)
            throw new IllegalArgumentException("Uri cannot be null.");

        final ImageWithMask imageView = (ImageWithMask) findViewById(R.id.image_view);

        if(!mUseRear)
            invertImageView(imageView, -1);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            public ProgressDialog processDialog;

            @Override
            protected void onPreExecute() {
                processDialog = new ProgressDialog(CaptureSceneActivity.this);
                processDialog.setMessage(getString(R.string.loading));
                processDialog.setCancelable(false);
                processDialog.setInverseBackgroundForced(false);
                processDialog.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                processDialog.hide();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    imageViewBitmap = BitmapLoader.load(getContentResolver(), uri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
                } catch (IOException e) {
                    Log.e(TAG, "Image cannot be loaded. " + e.toString() + ": " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.show();
                        }
                    });
                    return null;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImage(imageViewBitmap);
                        imageView.invalidate();
                    }
                });

                return null;
            }
        };

        task.execute();
    }

    private void invertImageView(ImageWithMask imageView, int scaleX) {
        imageView.setScale(scaleX);
        imageView.invalidate();
    }

    private boolean isImageSelected(){
        return imageUri != null;
    }
}
