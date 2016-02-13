package com.salat.viralcam.app.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.salat.viralcam.app.computeshader.ComputeShader;

/**
 * Created by Marek on 09.02.2016.
 */
class FragmentUtilActivity extends Activity implements ComputeShaderFragment.OnFragmentEvents {
    ComputeShaderFragment.OnFragmentEvents callback;

    public void setComputeShaderFragmentOnFragmentEvents(ComputeShaderFragment.OnFragmentEvents callback){
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        LinearLayout view = new LinearLayout(this);
        view.setId(1);

        setContentView(view);
    }

    @Override
    public ComputeShader createComputeShader() {
        return callback.createComputeShader();
    }

    @Override
    public void onComputeShaderFragmentStart(ComputeShaderFragment fragment) {
        callback.onComputeShaderFragmentStart(fragment);
    }
}
