package com.salat.viralcam.app.activities;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;

import java.io.IOException;

public class TestSandboxActivity extends AppCompatActivity {

    private AppCompatSeekBar colorSeekBar;
    private AppCompatSeekBar lightSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sandbox);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Uri backgroundUri = Constants.getUriFromResource(getResources(), R.raw.panda);
        Bitmap background = getBitmap(backgroundUri);
        final ImageView imageView = (ImageView) findViewById(R.id.image_view);
        assert imageView != null;
        imageView.setImageBitmap(background);

        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImageColorMatrix(imageView);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        lightSeekBar = (AppCompatSeekBar) findViewById(R.id.light_seekbar);
        assert lightSeekBar != null;
        lightSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        colorSeekBar = (AppCompatSeekBar) findViewById(R.id.color_seekbar);
        assert colorSeekBar != null;
        colorSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

    }

    private void updateImageColorMatrix(ImageView imageView){
        int light = lightSeekBar.getProgress() - lightSeekBar.getMax() / 2;
        float color = (colorSeekBar.getProgress() / (float) colorSeekBar.getMax()) * 2f;

        setImageColorMatrix(imageView, light, color);
    }

    private void setImageColorMatrix(ImageView imageView, int light, float color){
//        https://docs.rainmeter.net/tips/colormatrix-guide/
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(color);

        float[] array = colorMatrix.getArray();
        array[4] = light;
        array[9] = light;
        array[14] = light;

        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        imageView.setColorFilter(colorMatrixColorFilter);

    }


    private Bitmap getBitmap(Uri imageUri) {
        Bitmap result = null;
        try {
            result = BitmapLoader.load(getContentResolver(), imageUri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
        } catch (IOException e) {

        }
        return result;
    }

}
