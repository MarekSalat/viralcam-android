package com.salat.viralcam.app.computeshader;

import android.annotation.TargetApi;
import android.support.annotation.NonNull;
import android.content.res.Resources;
import android.opengl.GLES31;
import android.os.Build;
import android.util.Log;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.computeshader.ShaderHelper.OnShaderError;
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
public class AddVectorsComputeShader implements ComputeShader, OnShaderArgsValidation, OnShaderError {
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

        program = ShaderHelper.compileShader(this, unused, loadShaderFromFile());
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
        ShaderHelper.checkGlError(this, "glUseProgram");

        // setup constants - elements count
        int elementsLocation = GLES31.glGetUniformLocation(program, "elements");
        GLES31.glUniform1ui(elementsLocation, SIZE);
        ShaderHelper.checkGlError(this, "glUniform1i");

        // setup buffers
        int[] buffers = new int[3];
        GLES31.glGenBuffers(3, buffers, 0);
        ShaderHelper.checkGlError(this, "glGenBuffers");

        // out buffer = 0
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[0]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), IntBuffer.allocate(SIZE), GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        ShaderHelper.checkGlError(this, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

        // in buffer = 1
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[1]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), a, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 1, buffers[1]);
        ShaderHelper.checkGlError(this, "buffer1, glBindBuffer, glBufferData, glBindBufferBase");

        // in buffer = 2
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[2]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, SIZE * Size.ofInt(), b, GLES31.GL_STATIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, buffers[2]);
        ShaderHelper.checkGlError(this, "buffer2, glBindBuffer, glBufferData, glBindBufferBase");

        // run forest run
        GLES31.glDispatchCompute(ShaderHelper.getDispatchSize(SIZE, WORKGROUP_SIZE_X), 1, 1);
        ShaderHelper.checkGlError(this, "glDispatchCompute");

        // wait a bit
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

        // read all stuff
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        ByteBuffer buffer = (ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER, 0, SIZE * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        ShaderHelper.checkGlError(this, "glUnmapBuffer");

        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }

    @Override
    public void onShaderError(String message, int error) {
        throw new RuntimeException(message + ": glError" + GLCodes.toString(error));
    }

    private String shaderSource;
    @NonNull private String loadShaderFromFile() {
        if(shaderSource == null)
            shaderSource = AssetLoader.loadFromRaw(resources, R.raw.test_shader);
        return shaderSource;
    }
}
