package com.salat.viralcam.app.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraLollipopFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.views.ImageWithMask;

import java.io.IOException;

import fragments.CameraFragment;
import fragments.CameraOldVersionsFragment;

public class CaptureSceneActivity extends AppCompatActivity {

    private static final String TAG = "CaptureSceneActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final String KEY_BACKGROUND_URI = "CaptureSceneActivity.KEY_BACKGROUND_URI";
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private Uri imageUri;
    private AlertDialog dialog;
    private GestureDetectorCompat mDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        onWindowFocusChanged(true);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_capture_scene);

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

        FragmentManager fm = getFragmentManager();
        Fragment cameraFragment = fm.findFragmentByTag(CAMERA_FRAGMENT);

        if (cameraFragment == null) {
            if(Constants.USE_ONLY_LEGACY_CAMERA_API || Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP)
                cameraFragment = CameraOldVersionsFragment.newInstance();
            else
                cameraFragment = CameraLollipopFragment.newInstance();

            cameraFragment.setRetainInstance(true);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, cameraFragment, CAMERA_FRAGMENT)
                    .commit();
        }

        final ImageButton takePictureButton = (ImageButton) findViewById(R.id.capture_scene_button);
        if(!isImageSelected())
            takePictureButton.setVisibility(View.INVISIBLE);

        final Fragment finalCameraFragment = cameraFragment;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraFragment) finalCameraFragment).takePicture(new CameraLollipopFragment.OnCaptureCompleted() {

                    @Override
                    public void onCaptureComplete(String foregroundImagePath, Uri uri) {
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

        final ImageButton selectBackgroundButton = (ImageButton) findViewById(R.id.select_image_button);
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

        final ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);
        mDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float alpha = imageView.getAlpha();
                float distance = Math.abs(distanceX) > Math.abs(distanceY) ? distanceX : distanceY;
                double deltaInPx = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
                double deltaInDps = deltaInPx / getResources().getDisplayMetrics().density;

                alpha += (distance < 0 ? -1 : 1) * deltaInDps / 1000;
                imageView.setAlpha(Math.max(0.2f, Math.min(0.95f, alpha)));
                return true;
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
                final Bitmap bitmap;
                try {
                    bitmap = BitmapLoader.load(getContentResolver(), uri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
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

    private boolean hasImage(@NonNull ImageView view) {
        Drawable drawable = view.getDrawable();
        boolean hasImage = (drawable != null);

        if (hasImage && (drawable instanceof BitmapDrawable)) {
            hasImage = ((BitmapDrawable)drawable).getBitmap() != null;
        }

        return hasImage;
    }


}
