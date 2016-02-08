package com.salat.viralcam.app.computeshader;

import android.annotation.TargetApi;
import android.support.annotation.NonNull;
import android.content.res.Resources;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.os.Build;
import android.util.Log;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.AssetLoader;
import com.salat.viralcam.app.util.GLCodes;
import com.salat.viralcam.app.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 *  http://www.slideshare.net/Khronos_Group/how-to-use-and-teach-opengl-compute-shaders
 *  http://malideveloper.arm.com/resources/sample-code/introduction-compute-shaders-2/
 *
 *  http://developer.android.com/guide/topics/graphics/opengl.html#version-check
 *  http://developer.android.com/reference/android/opengl/GLES31.html
 *  developer.android.com/reference/android/opengl/GLES31Ext.html
 */
public class AddVectorsComputeShader implements ComputeShader, OnShaderArgsValidation {
    private static class Args implements ComputeShaderArgs {
        public IntBuffer a;
        public IntBuffer b;
        public IntBuffer result;
        private ComputeShaderResultCallback callback;

        public Args(IntBuffer a, IntBuffer b, ComputeShaderResultCallback callback) {

            this.a = a;
            this.b = b;
            this.callback = callback;
        }

        @Override
        public ComputeShaderResultCallback getResultCallback() {
            return callback;
        }
    }

    private static final String TAG = "AddVectorsComputeShader";
    private static final int WORKGROUP_SIZE_X = 32; // see shader layout
    public static final int TEST_SHADER_COMP_FILE_NAME =  R.raw.test_shader;

    private final Resources resources;
    private int program;

    public AddVectorsComputeShader(Resources resources){
        this.resources = resources;
    }

    public static ComputeShaderArgs createArgs(IntBuffer a, IntBuffer b, ComputeShaderResultCallback callback){
        return new Args(a, b, callback);
    }

    public static IntBuffer getResultFromArgs(ComputeShaderArgs args){
        return ((Args)args).result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void initialize(GL10 unused){
//            // fixme: could be replaced with following code but unfortunately its throws exception: not yet implemented :(
//            program = GLES31.glCreateShaderProgramv(GLES31.GL_COMPUTE_SHADER, new String[]{ shaderSource });
//            checkGlError(TAG, "glCreateShaderProgramv");

        if(unused == null)
            throw new IllegalArgumentException("GLES context must be initialized");

        if(!supportsGLES31(unused)){
            Log.e(TAG, "device does not supports GLES31");
            throw new IllegalArgumentException("Device does not supports GLES31");
        }

        program = GLES31.glCreateProgram();
        checkGlError(TAG, "glCreateProgram");

        int shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);
        checkGlError(TAG, "glCreateShader");

        GLES31.glShaderSource(shader, loadShaderFromFile());
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

    @Override
    public void validate(ComputeShaderArgs args) {
        if (!(args instanceof Args))
            throw new IllegalArgumentException("args must be instance of this.Args. Use createArgs method.");

        Args myArgs = (Args) args;
        if(myArgs.a == null)
            throw new IllegalArgumentException("a");
        if(myArgs.b == null)
            throw new IllegalArgumentException("b");
        if(myArgs.a.capacity() != myArgs.b.capacity())
            throw new IllegalArgumentException("a and b must have same dimension.");
        if(myArgs.a.capacity() == 0)
            throw new IllegalArgumentException("buffers cannot be empty");
    }

    @Override
    public void compute(ComputeShaderArgs args) {
        Args myArgs = (Args) args;

        try {
            myArgs.result = compute(myArgs.a, myArgs.b);
        } catch (Exception e) {
            args.getResultCallback().error(this, args, e);
            return;
        }

        args.getResultCallback().success(this, args);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private IntBuffer compute(IntBuffer a, IntBuffer b){
        final int SIZE = a.capacity();

        GLES31.glUseProgram(program);
        checkGlError(TAG, "glUseProgram");

        // setup constants - elements count
        int elementsLocation = GLES31.glGetUniformLocation(program, "elements");
        GLES31.glUniform1ui(elementsLocation, SIZE);
        checkGlError(TAG, "glUniform1i");

        // setup buffers
        int[] buffers = new int[3];
        GLES31.glGenBuffers(3, buffers, 0);
        checkGlError(TAG, "glGenBuffers");

        // out buffer = 0
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[0]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), IntBuffer.allocate(SIZE), GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        checkGlError(TAG, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

        // in buffer = 1
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[1]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), a, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, buffers[1]);
        checkGlError(TAG, "buffer1, glBindBuffer, glBufferData, glBindBufferBase");

        // in buffer = 2
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[2]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), b, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, buffers[2]);
        checkGlError(TAG, "buffer2, glBindBuffer, glBufferData, glBindBufferBase");

        // run forest run
        GLES31.glDispatchCompute(getDispatchSize(SIZE, WORKGROUP_SIZE_X), 1, 1);
        checkGlError(TAG, "glDispatchCompute");

        // wait a bit
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

        // read all stuff
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        ByteBuffer buffer = (ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER, 0, SIZE * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        checkGlError(TAG, "glUnmapBuffer");

        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }


    private int getDispatchSize(int workSize, int workGroupSize){
        return (workSize % workGroupSize > 0) ?  workSize / workGroupSize + 1 : workSize / workGroupSize;
    }

    private String shaderSource;
    @NonNull private String loadShaderFromFile() {
        if(shaderSource == null)
            shaderSource = AssetLoader.loadFromRaw(resources, TEST_SHADER_COMP_FILE_NAME);
        return shaderSource;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean supportsGLES31(GL10 gl) {
        int[] vers = new int[2];

        gl.glGetIntegerv(GLES30.GL_MAJOR_VERSION, vers, 0);
        gl.glGetIntegerv(GLES30.GL_MINOR_VERSION, vers, 1);

        return vers[0] > 3 || (vers[0] == 3 && vers[1] >= 1);
    }

    private void checkGlError(String TAG, String op) {
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
