package com.salat.viralcam.app.activities;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.ComputeShaderFragment;

import java.nio.IntBuffer;

public class ComputeShaderActivity extends AppCompatActivity implements ComputeShaderFragment.OnFragmentInteractionListener {
    private static final String FRAGMENT = "FRAGMENT";
    private String TAG = "ComputeShaderActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_compute_shader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            boolean fragmentHidden = true;

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                Snackbar.make(view, fragmentHidden ? "Fragment shown" : "Fragment hidden" , Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                FragmentManager fm = getFragmentManager();
                Fragment fragment = fm.findFragmentByTag(FRAGMENT);

                if (fragmentHidden) {
                    if(fragment == null)
                        fragment = ComputeShaderFragment.newInstance();

                    fragment.setRetainInstance(true);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment, FRAGMENT)
                            .commit();

                    fragmentHidden = false;
                }
                else{
                    if(fragment != null)
                        getFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commit();
                    fragmentHidden = true;
                }
            }
        });
    }

    private static int invocationAttempt = 1;
    @Override
    public void onStart(ComputeShaderFragment fragment) {
        final int SIZE = 1024 * invocationAttempt*invocationAttempt;
        invocationAttempt++;
        IntBuffer input1buffer = IntBuffer.allocate(SIZE);
        for (int i = 0; i < SIZE; i++) {
            input1buffer.put(i, 1027);
        }

        IntBuffer input2Buffer = IntBuffer.allocate(SIZE);
        for (int i = 0; i < SIZE; i++) {
            input2Buffer.put(i, 3039);
        }

        final long start = System.currentTimeMillis();
        fragment.compute(input1buffer, input2Buffer, new ComputeShaderFragment.ComputeResult() {
            @Override
            public void onResult(IntBuffer c) {
                long end = System.currentTimeMillis();
                Log.e(TAG, String.format("Result (%d) [%d, ... %d, %d, ... %d] in %d [ms]", SIZE, c.get(0), c.get(SIZE / 2), c.get(SIZE / 2 + 1), c.get(SIZE - 1), end - start));
            }
        });
    }
}
