package com.salat.viralcam.app.activities;

import com.github.clans.fab.FloatingActionButton;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraLollipopFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.views.ImageWithMask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;

import fragments.CameraFragment;
import fragments.CameraOldVersionsFragment;

public class HomeScreenActivity extends Activity {
    private static final String TAG = "HomeScreenActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final String KEY_BACKGROUND_URI = "HomeScreenActivity.KEY_BACKGROUND_URI";
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private Uri imageUri;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_home_screen);

        if(savedInstanceState != null && savedInstanceState.getString(KEY_BACKGROUND_URI) != null && !savedInstanceState.getString(KEY_BACKGROUND_URI).isEmpty()){
            imageUri =  Uri.parse(savedInstanceState.getString(KEY_BACKGROUND_URI));

            if(isImageSelected()){
                FrameLayout layout = (FrameLayout) findViewById(R.id.container);
                layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        setImageViewBackground(imageUri);
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
        if(!isImageSelected())
            takePictureButton.setVisibility(View.INVISIBLE);

        final Fragment finalCameraFragment = cameraFragment;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureButton.setEnabled(false);
                ((CameraFragment) finalCameraFragment).takePicture(new CameraLollipopFragment.OnCaptureCompleted() {

                    @Override
                    public void onCaptureComplete(String foregroundImagePath, Uri uri) {
                        if (foregroundImagePath == null || uri == null) {
                            return;
                        }

                        Intent intent = new Intent(HomeScreenActivity.this, TrimapActivity.class);
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_FOREGROUND_IMAGE_URI, uri.toString());
                        intent.putExtra(TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_URI, imageUri.toString());
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

                imageUri = Constants.getUriFromResource(getResources(), resourceId);
                setImageViewBackground(imageUri);
            }
        });


        dialog = builder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isImageSelected()){
            dialog.show();
        }
        findViewById(R.id.take_picture_button).setEnabled(true);
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

        final ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            public ProgressDialog processDialog;

            @Override
            protected void onPreExecute() {
                processDialog = new ProgressDialog(HomeScreenActivity.this);
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
                final Bitmap bitmap;
                try {
                    bitmap = BitmapLoader.load(getContentResolver(), uri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
                } catch (IOException e) {
                    Log.e(TAG, "Image cannot be loader. " + e.toString());
                    dialog.show();
                    return null;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImage(bitmap);
                        imageView.invalidate();
                    }
                });

                return null;
            }
        };

        task.execute();
    }

    private boolean isImageSelected(){
        return imageUri != null;
    }
}
