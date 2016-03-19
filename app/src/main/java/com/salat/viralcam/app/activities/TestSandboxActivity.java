package com.salat.viralcam.app.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;

import java.io.IOException;

public class TestSandboxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sandbox);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Uri backgroundUri = Constants.getUriFromResource(getResources(), R.raw.panda);
        Bitmap background = getBitmap(backgroundUri);
        final ImageView imageView = (ImageView) findViewById(R.id.image_view);
        imageView.setImageBitmap(background);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
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
