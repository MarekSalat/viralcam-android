package com.salat.viralcam.app.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES31;
import android.opengl.GLES31Ext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.salat.viralcam.app.R;

import java.nio.Buffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class ComputeShaderActivity extends AppCompatActivity {

    class MyGLSurfaceView extends GLSurfaceView {
        private final MyGLRenderer mRenderer;

        public MyGLSurfaceView(Context context){
            super(context);

            // Create an OpenGL ES 2.0 context
            setEGLContextClientVersion(2);

            mRenderer = new MyGLRenderer();

            // Set the Renderer for drawing on the GLSurfaceView
            setRenderer(mRenderer);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public class MyGLRenderer implements GLSurfaceView.Renderer {
        public static final int SIZE = 128;
        private int program;
        private int shader;
        private int[] buffer_out;
        private int[] buffer_in1;
        private int[] buffer_in2;

        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            program = GLES31.glCreateProgram();
            shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);

            buffer_out = new int[1];
            buffer_in1 = new int[1];
            buffer_in2 = new int[1];

            GLES31.glGenBuffers(SIZE, buffer_out, 0);
            GLES31.glGenBuffers(SIZE, buffer_in1, 0);
            GLES31.glGenBuffers(SIZE, buffer_in2, 0);

            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, buffer_in1[1]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, buffer_in2[2]);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE, IntBuffer.allocate(SIZE), 1);
            GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE, IntBuffer.allocate(SIZE), 2);

            GLES31.glShaderSource(shader,
                    "#version 310 es\n" +
                    "layout(local_size_x = 128) in;\n" +

                    "layout(std430, binding = 0) writeonly buffer Output {\n" +
                    "   vec4 elements[];\n" +
                    "} output_data;\n" +
                    "layout(std430, binding = 1) readonly buffer Input0 {\n" +
                    "   vec4 elements[];\n" +
                    "} input_data0;\n" +
                    "layout(std430, binding = 2) readonly buffer Input1 {\n" +
                    "   vec4 elements[];\n" +
                    "} input_data1;\n" +

                    "void main()\n" +
                    "{\n" +
                    "   uint ident = gl_GlobalInvocationID.x;\n" +
                    "   output_data.elements[ident] = input_data0.elements[ident] * input_data1.elements[ident];\n" +
                    "}"
            );

            GLES31.glCompileShader(shader);
            GLES31.glAttachShader(program, shader);
            GLES31.glLinkProgram(program);
        }


        public void onDrawFrame(GL10 unused) {
            GLES31.glUseProgram(program);
            GLES31.glDispatchCompute(SIZE, 1, 1);

            GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffer_out[0]);
            Buffer buffer = GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, SIZE, GLES31.GL_READ_ONLY);

            Log.e("FoFO", "DOne " + buffer.toString());
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
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
