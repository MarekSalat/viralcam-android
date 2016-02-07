package com.salat.viralcam.app.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.salat.viralcam.app.util.GLCodes;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ComputeShaderActivity extends AppCompatActivity {

    private String TAG = "ComputeShaderActivity";

    class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;

        public MyGLSurfaceView(Context context){
            super(context);

            setEGLContextClientVersion(3);
            mRenderer = new MyGLRenderer();

            setRenderer(mRenderer);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public class MyGLRenderer implements GLSurfaceView.Renderer {
        public static final int SIZE = 128;
        private int program;
        private int shader;
        private int[] buffers;
        private String shaderSource = "#version 310 es\n" +
                "layout(local_size_x = 128) in;\n" +
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
                "   output_data.elements[ident] = 13 + input_data0.elements[ident] + input_data1.elements[ident];\n" +
                "}";


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
            GLES31.glUseProgram(program);
            checkGlError(TAG, "glUseProgram");

            buffers = new int[3];
            GLES31.glGenBuffers(3, buffers, 0);
            checkGlError(TAG, "glGenBuffers");

            // in buffer = 1
            IntBuffer input1buffer = IntBuffer.allocate(SIZE);
            for (int i = 0; i < SIZE; i++) {
                input1buffer.put(i, 68);
            }
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[1]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE, input1buffer, GLES31.GL_STATIC_DRAW);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, buffers[1]);
            checkGlError(TAG, "buffer1, glBindBuffer, glBufferData, glBindBufferBase");

            // in buffer = 2
            IntBuffer input2Buffer = IntBuffer.allocate(SIZE);
            for (int i = 0; i < SIZE; i++) {
                input2Buffer.put(i, 37);
            }
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[2]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE, input2Buffer, GLES31.GL_STATIC_DRAW);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, buffers[2]);
            checkGlError(TAG, "buffer2, glBindBuffer, glBufferData, glBindBufferBase");

            // out buffer = 0
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[0]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE, IntBuffer.allocate(SIZE), GLES31.GL_STATIC_READ);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
            checkGlError(TAG, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

            // run forest run
            GLES31.glDispatchCompute(SIZE / 32, 1, 1);
            checkGlError(TAG, "glDispatchCompute");

            // wait a bit
            GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

            // read all stuff
            ByteBuffer buffer = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, SIZE, GLES31.GL_MAP_READ_BIT );
            buffer.order(ByteOrder.nativeOrder());
            IntBuffer resultIntBuffer = buffer.asIntBuffer();

            int result = resultIntBuffer.get(0);

            Log.e(TAG, "onDrawFrame " + (buffer.isDirect() ? result : ""));

            GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
            checkGlError(TAG, "glUnmapBuffer");
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyGLSurfaceView mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);

//        setContentView(R.layout.activity_compute_shader);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                // http://www.slideshare.net/Khronos_Group/how-to-use-and-teach-opengl-compute-shaders
//                // http://malideveloper.arm.com/resources/sample-code/introduction-compute-shaders-2/
//
//                // http://developer.android.com/guide/topics/graphics/opengl.html#version-check
//                // http://developer.android.com/reference/android/opengl/GLES31.html
//                // developer.android.com/reference/android/opengl/GLES31Ext.html
//            }
//        });
    }

}
