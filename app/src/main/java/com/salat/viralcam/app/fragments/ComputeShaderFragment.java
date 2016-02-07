package com.salat.viralcam.app.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public static class MyGLSurfaceView extends GLSurfaceView {
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static class MyGLRenderer implements GLSurfaceView.Renderer {
        private static final int WORKGROUP_SIZE_X = 32;

        private boolean compute = false;
        private int program;
        private int shader;
        private int[] buffers;
        private String shaderSource = "#version 310 es\n" +
                "uniform uint elements; \n" +
                "layout(local_size_x = " + WORKGROUP_SIZE_X + ") in;\n" +
                "layout(std430) buffer;\n" +
                "layout(binding = 0) writeonly buffer Output {\n" +
                "   int elements[];\n" +
                "} output_data;\n" +
                "layout(binding = 1) readonly buffer Input0 {\n" +
                "   int elements[];\n" +
                "} input_data0;\n" +
                "layout(binding = 2) readonly buffer Input1 {\n" +
                "   int elements[];\n" +
                "} input_data1;\n" +

                "void main()\n" +
                "{\n" +
                "   uint ident = gl_GlobalInvocationID.x;\n" +
                "   if(ident >= elements) return; \n" +
                "   output_data.elements[ident] = input_data0.elements[ident] + input_data1.elements[ident];\n" +
                "}";
        private IntBuffer input1buffer;
        private IntBuffer input2Buffer;
        private ComputeResult resultCallback;


        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
//            // fixme: could be replaced with following code but unfortunately its throws exception: not yet implemented :(
//            program = GLES31.glCreateShaderProgramv(GLES31.GL_COMPUTE_SHADER, new String[]{ shaderSource });
//            checkGlError(TAG, "glCreateShaderProgramv");

            program = GLES31.glCreateProgram();
            checkGlError(TAG, "glCreateProgram");

            shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);
            checkGlError(TAG, "glCreateShader");

            GLES31.glShaderSource(shader, shaderSource);
            checkGlError(TAG, "glShaderSource");

            GLES31.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + ":");
                Log.e(TAG, GLES31.glGetShaderInfoLog(shader));
            }
            checkGlError(TAG, "glCompileShader");

            GLES31.glAttachShader(program, shader);
            checkGlError(TAG, "glAttachShader");

            GLES31.glLinkProgram(program);
            checkGlError(TAG, "glLinkProgram");
        }

        public void onDrawFrame(GL10 unused) {
            if(!compute || input1buffer == null || input2Buffer == null)
                return;

            final int SIZE = input1buffer.capacity();

            GLES31.glUseProgram(program);
            checkGlError(TAG, "glUseProgram");

            // setup constants - elements count
            int elementsLocation = GLES31.glGetUniformLocation(program, "elements");
            GLES31.glUniform1ui(elementsLocation, SIZE);
            checkGlError(TAG, "glUniform1i");

            // setup buffers
            buffers = new int[3];
            GLES31.glGenBuffers(3, buffers, 0);
            checkGlError(TAG, "glGenBuffers");

            // out buffer = 0
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[0]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), IntBuffer.allocate(SIZE), GLES31.GL_STATIC_READ);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
            checkGlError(TAG, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

            // in buffer = 1
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[1]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), input1buffer, GLES31.GL_STATIC_DRAW);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, buffers[1]);
            checkGlError(TAG, "buffer1, glBindBuffer, glBufferData, glBindBufferBase");

            // in buffer = 2
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[2]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), input2Buffer, GLES31.GL_STATIC_DRAW);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, buffers[2]);
            checkGlError(TAG, "buffer2, glBindBuffer, glBufferData, glBindBufferBase");

            // run forest run
            GLES31.glDispatchCompute(getDispatchSize(SIZE, WORKGROUP_SIZE_X), 1, 1);
            checkGlError(TAG, "glDispatchCompute");

            // wait a bit
            GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

            // read all stuff
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
            ByteBuffer buffer = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, SIZE * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
            GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
            checkGlError(TAG, "glUnmapBuffer");

            buffer.order(ByteOrder.nativeOrder());
            IntBuffer resultIntBuffer = buffer.asIntBuffer();

//            int start = resultIntBuffer.get(0);
//            int end = resultIntBuffer.get(SIZE-1);
//
//            Log.e(TAG, String.format("Result [%d, ... %d, %d, ... %d]", start, resultIntBuffer.get(SIZE/2), resultIntBuffer.get(SIZE/2+1), end));

            doNothing();
            resultCallback.onResult(resultIntBuffer);
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
        }

        public void doNothing() {
            compute = false;
            input1buffer = null;
            input2Buffer = null;
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

        private int getDispatchSize(int workSize, int workGroupSize){
            return (workSize % workGroupSize > 0) ?  workSize / workGroupSize + 1 : workSize / workGroupSize;
        }

        protected void checkGlError(String TAG, String op) {
            int error;
            int prevError = 0;
            boolean hadError = false;
            while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
                Log.e(TAG, op + String.format(": glError %d, %x. %s", error,error, GLCodes.toString(error)));
                prevError = error;
                hadError = true;
            }
            if(hadError)
                throw new RuntimeException(op + ": glError " + prevError + ", " + GLCodes.toString(prevError));
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
