package com.salat.viralcam.app.activities;

import com.github.clans.fab.FloatingActionButton;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraLollipopFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.util.RealPathUtil;
import com.salat.viralcam.app.views.ImageWithMask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import fragments.CameraFragment;
import fragments.CameraOldVersionsFragment;

public class HomeScreenActivity extends Activity {
    private static final String TAG = "HomeScreenActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final String KEY_BACKGROUND_PATH = "HomeScreenActivity.KEY_BACKGROUND_PATH";
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private String backgroundImagePath;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_home_screen);

        if(savedInstanceState != null){
            backgroundImagePath = savedInstanceState.getString(KEY_BACKGROUND_PATH);

            if(isBackgroundSelected()){
                FrameLayout layout = (FrameLayout) findViewById(R.id.container);
                layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int resourceId = 0;
                        try {
                            resourceId = Integer.parseInt(backgroundImagePath);
                        }
                        catch (NumberFormatException e){}

                        if(resourceId == 0)
                            setImageViewBackground(backgroundImagePath);
                        else
                            setImageViewBackground(resourceId);
                    }
                });
            }
        }

        FragmentManager fm = getFragmentManager();
        Fragment cameraFragment = fm.findFragmentByTag(CAMERA_FRAGMENT);

        if (cameraFragment == null) {
            if(Constants.USE_ONLY_LEGACY_CAMERA_API || Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP)
                cameraFragment = CameraOldVersionsFragment.newInstance();
            else
                cameraFragment = CameraLollipopFragment.newInstance();

            cameraFragment.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit();
        }

        final FloatingActionButton takePictureButton = (FloatingActionButton) findViewById(R.id.take_picture_button);
        if(!isBackgroundSelected())
            takePictureButton.setVisibility(View.INVISIBLE);

        final Fragment finalCameraFragment = cameraFragment;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraFragment) finalCameraFragment).takePicture(new CameraLollipopFragment.OnCaptureCompleted() {

                    @Override
                    public void onCaptureComplete(String foregroundImagePath, Uri uri) {
                        Intent intent = new Intent(HomeScreenActivity.this, TrimapActivity.class);
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_PATH, foregroundImagePath);
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_PATH, backgroundImagePath);
                        startActivity(intent);
                    }
                });
            }
        });

        final FloatingActionButton selectBackgroundButton = (FloatingActionButton) findViewById(R.id.select_background_button);
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
                int resourceId = R.raw.panda;

                switch (which){
                    case 0:  resourceId = R.raw.panda; break;
                    case 1:  resourceId = R.raw.pizza_tower; break;
                    case 2:  resourceId = R.raw.game_of_thrones; break;
                    case 3:  resourceId = R.raw.star_warse; break;

                    default:{

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

                backgroundImagePath = Integer.toString(resourceId);
                setImageViewBackground(resourceId);
            }
        });


        dialog = builder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isBackgroundSelected()){
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        backgroundImagePath = RealPathUtil.getRealPathFromURI(this, uri);

        setImageViewBackground(backgroundImagePath);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if(isBackgroundSelected())
            outState.putString(KEY_BACKGROUND_PATH, backgroundImagePath);
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

    private void setImageViewBackground(String backgroundImagePath) {
        ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);
        imageView.setImage(
                BitmapLoader.load(backgroundImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT)
        );
        imageView.invalidate();
    }

    private void setImageViewBackground(int resourceId) {
        ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);
        imageView.setImage(
                BitmapLoader.load(getResources(), resourceId, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT)
        );
        imageView.invalidate();
    }

    private boolean isBackgroundSelected(){
        return backgroundImagePath != null && !backgroundImagePath.isEmpty();
    }
}
