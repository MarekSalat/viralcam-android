package com.salat.viralcam.app.activities;

import com.github.clans.fab.FloatingActionButton;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.Camera2RawFragment;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.util.RealPathUtil;
import com.salat.viralcam.app.util.SystemUiHider;
import com.salat.viralcam.app.views.ImageWithMask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class HomeScreenActivity extends Activity{
    private static final String TAG = "HomeScreenActivity";
    private static final int PICK_IMAGE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_screen);

        final Camera2RawFragment camera2RawFragment = Camera2RawFragment.newInstance();
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, camera2RawFragment)
                    .commit();
        }


        final FloatingActionButton selectBackgroundButton = (FloatingActionButton) findViewById(R.id.select_background_button);
        selectBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        final FloatingActionButton takePictureButton = (FloatingActionButton) findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //camera2RawFragment.takePicture();
                Intent intent = new Intent(HomeScreenActivity.this, TrimapActivity.class);
                intent.putExtra(TrimapActivity.INTENT_EXTRA_BACKGROUND_IMAGE_PATH, Constants.TEST_IMAGE_PATH);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String realPath = RealPathUtil.getRealPathFromURI(this, uri);

            ImageWithMask imageView = (ImageWithMask) findViewById(R.id.imageView);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(realPath, options);
            options.inSampleSize = calculateInSampleSize(options, Constants.IMAGE_ALLOWED_WIDTH,  Constants.IMAGE_ALLOWED_HEIGHT);

            Log.e(TAG, realPath +
                            " [" + options.outWidth + ", " + options.outHeight + "]" +
                            " [" + imageView.getWidth() + ", " + imageView.getHeight() + "]" +
                            " (" + options.inSampleSize + ")");

            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(realPath, options);
            imageView.setImage(bitmap);

            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
