package com.salat.viralcam.app.activities;

import com.github.clans.fab.FloatingActionButton;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.util.RealPathUtil;
import com.salat.viralcam.app.views.ImageWithMask;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class HomeScreenActivity extends Activity{
    private static final String TAG = "HomeScreenActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private String foregroundImagePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_screen);

        final CameraFragment camera2RawFragment = CameraFragment.newInstance();
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, camera2RawFragment)
                    .commit();
        }

        final FloatingActionButton takePictureButton = (FloatingActionButton) findViewById(R.id.take_picture_button);
        takePictureButton.setVisibility(View.INVISIBLE);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera2RawFragment.takePicture(new CameraFragment.OnCaptureCompleted() {
                    @Override
                    public void onCaptureComplete(String backgroundImagePath, Uri uri) {
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
                takePictureButton.setVisibility(View.VISIBLE);
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        foregroundImagePath = RealPathUtil.getRealPathFromURI(this, uri);

        ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);
        imageView.setImage(
            BitmapLoader.load(foregroundImagePath, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT)
        );

        super.onActivityResult(requestCode, resultCode, data);
    }
}
