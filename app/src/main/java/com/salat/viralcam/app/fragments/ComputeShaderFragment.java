package com.salat.viralcam.app.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.salat.viralcam.app.ComputeShaderTest;
import com.salat.viralcam.app.util.AssetLoader;
import com.salat.viralcam.app.util.GLCodes;
import com.salat.viralcam.app.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
*  http://www.slideshare.net/Khronos_Group/how-to-use-and-teach-opengl-compute-shaders
*  http://malideveloper.arm.com/resources/sample-code/introduction-compute-shaders-2/
*
*  http://developer.android.com/guide/topics/graphics/opengl.html#version-check
*  http://developer.android.com/reference/android/opengl/GLES31.html
*  developer.android.com/reference/android/opengl/GLES31Ext.html
*/
public class ComputeShaderFragment extends Fragment {
    private static final String TAG = "ComputeShaderFragment";

    public interface ComputeResult {
        void onResult(IntBuffer c);
    }

    public class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;

        public MyGLSurfaceView(Context context){
            super(context);

            setEGLContextClientVersion(3);
            mRenderer = new MyGLRenderer();
            mRenderer.doNothing();

            setRenderer(mRenderer);
        }

        public void doNothing() {
            mRenderer.doNothing();
        }

        public void compute(IntBuffer a, IntBuffer b, ComputeResult result) {
            mRenderer.compute(a, b, result);
        }
    }


    public class MyGLRenderer implements GLSurfaceView.Renderer {
        private ComputeResult resultCallback;
        private boolean compute = false;
        private IntBuffer input1buffer;
        private IntBuffer input2Buffer;

        private ComputeShaderTest shader;

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//            // fixme: could be replaced with following code but unfortunately its throws exception: not yet implemented :(
//            program = GLES31.glCreateShaderProgramv(GLES31.GL_COMPUTE_SHADER, new String[]{ shaderSource });
//            checkGlError(TAG, "glCreateShaderProgramv");



            shader = new ComputeShaderTest(getActivity().getResources());
            shader.init(gl);
        }

        public void onDrawFrame(GL10 unused) {
            if (compute && input1buffer != null && input2Buffer != null)
                resultCallback.onResult(shader.compute(input1buffer, input2Buffer));

            doNothing();
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
        }


        public void compute(IntBuffer a, IntBuffer b, ComputeResult result) {
            if(a == null)
                throw new IllegalArgumentException("a");
            if(b == null)
                throw new IllegalArgumentException("b");
            if(a.capacity() != b.capacity())
                throw new IllegalArgumentException("a and b must have same dimension.");
            if(a.capacity() == 0)
                throw new IllegalArgumentException("buffers cannot be empty");

            input1buffer = a;
            input2Buffer = b;
            resultCallback = result;

            compute = true;
        }

        public void doNothing() {
            compute = false;
            input1buffer = null;
            input2Buffer = null;
        }
    }

    private OnFragmentInteractionListener mListener;

    public ComputeShaderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ComputeShaderFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ComputeShaderFragment newInstance() {
        return new ComputeShaderFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new MyGLSurfaceView(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();

        mListener.onStart(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public void compute(IntBuffer a, IntBuffer b, ComputeResult result){
        MyGLSurfaceView view = (MyGLSurfaceView) getView();
        view.compute(a, b, result);
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
    public interface OnFragmentInteractionListener {
        void onStart(ComputeShaderFragment fragment);
    }
}
