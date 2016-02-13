package com.salat.viralcam.app.fragments;

import android.test.ActivityInstrumentationTestCase2;

import com.salat.viralcam.app.computeshader.AddVectorsComputeShader;
import com.salat.viralcam.app.computeshader.ComputeShader;

import org.junit.Before;
import org.junit.Test;

public class ComputeShaderFragmentTest extends ActivityInstrumentationTestCase2<FragmentUtilActivity> implements ComputeShaderFragment.OnFragmentEvents {

    private ComputeShaderFragment fragment;

    public ComputeShaderFragmentTest(){
        super(FragmentUtilActivity.class);
    }

    @Before
    public void setup() {
        fragment = ComputeShaderFragment.newInstance();
        getActivity().setComputeShaderFragmentOnFragmentEvents(this);
        getActivity().getFragmentManager()
                .beginTransaction()
                .add(1, fragment, null)
                .commit();
    }


    @Override
    public ComputeShader createComputeShader() {
        return new AddVectorsComputeShader(getActivity().getResources());
    }

    @Override
    public void onComputeShaderFragmentStart(ComputeShaderFragment fragment) {
        ;
    }

    @Test
    public void testFoo() {
        int foo = 42;
        fragment.compute(null);
    }
}