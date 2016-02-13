package com.salat.viralcam.app.fragments;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.salat.viralcam.app.computeshader.ComputeShader;
import com.salat.viralcam.app.computeshader.ComputeShaderArgs;
import com.salat.viralcam.app.computeshader.OnShaderArgsValidation;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class ComputeShaderFragment extends Fragment {
    private static final String TAG = "ComputeShaderFragment";

    private class ComputeShaderGLSurfaceView extends GLSurfaceView {
        private final ComputeShaderGLRenderer mRenderer;

        public ComputeShaderGLSurfaceView(Context context, ComputeShader shader){
            super(context);

            setEGLContextClientVersion(3);
            mRenderer = new ComputeShaderGLRenderer(shader);
            mRenderer.doNothing();

            setRenderer(mRenderer);
        }

        public void compute(ComputeShaderArgs args) {
            if(args == null)
                throw new IllegalArgumentException("args cannot be null");

            mRenderer.compute(args);
        }
    }

    private class ComputeShaderGLRenderer implements GLSurfaceView.Renderer {
        private boolean compute = false;

        private ComputeShader shader;
        private ComputeShaderArgs args;

        public ComputeShaderGLRenderer(ComputeShader shader){
            this.shader = shader;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            shader.initialize(gl);
        }

        private boolean isRunning = false;
        @Override
        public void onDrawFrame(GL10 unused) {
            if (compute && args != null && !isRunning){
                isRunning = true;
                shader.compute(args);
            }
            doNothing();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
        }


        public void compute(ComputeShaderArgs args) {
            if(args == null)
                throw new IllegalArgumentException("args cannot be null");

            if(shader instanceof OnShaderArgsValidation){
                try{
                    ((OnShaderArgsValidation)shader).validate(args);
                }
                catch (Exception e){
                    args.getResultCallback().error(shader, args, e);
                    return;
                }
            }

            this.args = args;
            compute = true;
        }

        private void doNothing() {
            compute = false;
            args = null;
            isRunning = false;
        }
    }

    private OnFragmentEvents mListener;

    public ComputeShaderFragment() {
        // Required empty public constructor
    }

    public static ComputeShaderFragment newInstance() {
        return new ComputeShaderFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new ComputeShaderGLSurfaceView(getActivity(), mListener.createComputeShader());
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.onComputeShaderFragmentStart(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentEvents) {
            mListener = (OnFragmentEvents) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentEvents");
        }
    }

    public void compute(ComputeShaderArgs args){
        if(args == null)
            throw new IllegalArgumentException("args cannot be null");

        ComputeShaderGLSurfaceView view = (ComputeShaderGLSurfaceView) getView();

        if(view == null)
            throw new RuntimeException("Fragment has not been properly initialized. View has not been created.");

        view.compute(args);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentEvents {
        ComputeShader createComputeShader();
        void onComputeShaderFragmentStart(ComputeShaderFragment fragment);
    }
}
