package com.salat.viralcam.app.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;

import com.salat.viralcam.app.R;

public class RippleTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        onWindowFocusChanged(true);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ripple_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //noinspection ConstantConditions
        findViewById(R.id.fragment).setOnTouchListener(new View.OnTouchListener() {
            boolean show = true;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_UP && event.getAction() != MotionEvent.ACTION_DOWN)
                    return false;
                show = !show;

                final View toolbar = findViewById(R.id.appbar);
                assert toolbar != null;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    toolbar.startAnimation(AnimationUtils.loadAnimation(RippleTestActivity.this, R.anim.slide_up_from_top));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    toolbar.startAnimation(AnimationUtils.loadAnimation(RippleTestActivity.this, R.anim.slide_down_from_top));
                }

                final View trimapBar = findViewById(R.id.trimap_action_bar);
                assert trimapBar != null;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    trimapBar.startAnimation(AnimationUtils.loadAnimation(RippleTestActivity.this, R.anim.slide_down_from_bottom));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    trimapBar.startAnimation(AnimationUtils.loadAnimation(RippleTestActivity.this, R.anim.slide_up_from_bottom));
                }

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ripple_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
