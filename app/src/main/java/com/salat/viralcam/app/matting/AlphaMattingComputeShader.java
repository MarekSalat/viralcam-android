package com.salat.viralcam.app.matting;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.computeshader.ComputeShader;
import com.salat.viralcam.app.computeshader.ComputeShaderArgs;
import com.salat.viralcam.app.computeshader.ComputeShaderResultCallback;
import com.salat.viralcam.app.computeshader.OnShaderArgsValidation;
import com.salat.viralcam.app.computeshader.ShaderHelper;
import com.salat.viralcam.app.util.AssetLoader;
import com.salat.viralcam.app.util.GLCodes;
import com.salat.viralcam.app.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Marek on 08.02.2016.
 */
public class AlphaMattingComputeShader implements ComputeShader, OnShaderArgsValidation, ShaderHelper.OnShaderError {
    private static final int IMAGE_BINDING = 0;
    private static final int TRIMAP_BINDING = 1;

    private static final int ALPHA_BUFFER_BINDING = 0;
    private static final int FOREGROUND_BOUNDARY_BINDING = 1;
    private static final int BACKGROUND_BUFFER_BINDING = 2;
    private static final int ATOMIC_BUFFER_BINDING = 3;

    private static final int BOUNDARY_CAPACITY = 1000 * 2; // (x, y)
    private static final int WORKGROUP_SIZE = 32;

    private int program;
    private int dimensionsLocation;
    private int imageLocation;
    private int trimapLocation;
    private Resources resources;
    private final IntBuffer atomicCounters = IntBuffer.allocate(2);
    private int trimapTexture;
    private int imageTexture;
    private int foregroundBoundaryBuffer;
    private int backgroundBoundaryBuffer;
    private int atomicCounterBuffer;
    private int alphaBuffer;
    private int[] textures;

    public static class Args implements ComputeShaderArgs {
        public Bitmap image;
        public Bitmap trimap;
        public Bitmap alpha;
        private ComputeShaderResultCallback callback;

        public Args(Bitmap image, Bitmap trimap, Bitmap alpha, ComputeShaderResultCallback callback){
            this.image = image;
            this.trimap = trimap;
            this.alpha = alpha;

            this.callback = callback;
        }

        @Override
        public ComputeShaderResultCallback getResultCallback() {
            return callback;
        }
    }

    public AlphaMattingComputeShader(Resources resources){
        this.resources = resources;
    }

    @Override
    public void initialize(GL10 gl) {
        program = ShaderHelper.compileShader(this, gl, loadShaderFromFile());

        dimensionsLocation = GLES31.glGetUniformLocation(program, "dimensions");
        imageLocation = GLES31.glGetUniformLocation(program, "image");
        trimapLocation = GLES31.glGetUniformLocation(program, "trimap");

        ShaderHelper.checkGlError(this, "glGetUniformLocation");

        textures = new int[2];
        GLES31.glGenTextures(textures.length, textures, 0);
        ShaderHelper.checkGlError(this, "glGenTextures");

        trimapTexture = textures[0];
        imageTexture = textures[1];

        int[] buffers = new int[4];
        GLES31.glGenBuffers(buffers.length, buffers, 0);
        ShaderHelper.checkGlError(this, "glGenBuffers");

        foregroundBoundaryBuffer = buffers[0];
        backgroundBoundaryBuffer = buffers[1];
        atomicCounterBuffer = buffers[2];
        alphaBuffer = buffers[3];
    }

    private String shaderSource;
    @NonNull
    private String loadShaderFromFile() {
        if(shaderSource == null)
            shaderSource = AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_sandbox);
        return shaderSource;
    }

    int tempIntBuffer[] = new int[1000 * 1000];
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void compute(ComputeShaderArgs _args) {
        Args args = (Args) _args;
        final int width = args.image.getWidth();
        final int height = args.image.getHeight();

        GLES31.glUseProgram(program);
        ShaderHelper.checkGlError(this, "glUseProgram");

        // set dimensions
        GLES31.glUniform2i(dimensionsLocation, width, height);
        GLES31.glUniform1i(imageLocation, IMAGE_BINDING);
        GLES31.glUniform1i(trimapLocation, TRIMAP_BINDING);
        ShaderHelper.checkGlError(this, "glUniform1i");

        // bind textures image and trimap
        bindTexture(args.image, imageTexture, IMAGE_BINDING);
        bindTexture(args.trimap, trimapTexture, TRIMAP_BINDING);
        ShaderHelper.checkGlError(this, "bindTexture");

        // create buffer for alpha
        int alphaPixels = width * height;

        // bind atomic counters
        atomicCounters.put(0, 0);
        atomicCounters.put(1, 0);
        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, ATOMIC_BUFFER_BINDING, atomicCounterBuffer);
        GLES31.glBufferData(GLES31.GL_ATOMIC_COUNTER_BUFFER, atomicCounters.capacity() * Size.ofInt(), atomicCounters, GLES31.GL_DYNAMIC_DRAW);
        ShaderHelper.checkGlError(this, "glBindBufferBase atomic");

        // bind alpha
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, alphaBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, alphaPixels * Size.ofInt(), null, GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, ALPHA_BUFFER_BINDING, alphaBuffer);
        ShaderHelper.checkGlError(this, "glBindBuffer, glBufferData, glBindBufferBase, alpha");

        // bind foreground boundary
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, foregroundBoundaryBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, BOUNDARY_CAPACITY * Size.ofInt(), null, GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, FOREGROUND_BOUNDARY_BINDING, foregroundBoundaryBuffer);
        ShaderHelper.checkGlError(this, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

        // bind background boundary
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, backgroundBoundaryBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, BOUNDARY_CAPACITY * Size.ofInt(), null, GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, BACKGROUND_BUFFER_BINDING, backgroundBoundaryBuffer);
        ShaderHelper.checkGlError(this, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

        // run forest run
        GLES31.glDispatchCompute(
                ShaderHelper.getDispatchSize(width, WORKGROUP_SIZE),
                ShaderHelper.getDispatchSize(height, WORKGROUP_SIZE), 1);
        ShaderHelper.checkGlError(this, "glDispatchCompute");

        // wait a bit
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);

        // read all stuff
        // read atomic counters
        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, ATOMIC_BUFFER_BINDING, atomicCounterBuffer);
        IntBuffer atomicCountersResult = ((ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_ATOMIC_COUNTER_BUFFER, 0, atomicCounters.capacity() * Size.ofInt(), GLES31.GL_MAP_READ_BIT))
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();

        Log.e("foo", String.format("counters %d, %d", atomicCountersResult.get(0), atomicCountersResult.get(1)));

        // read alpha values
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, ALPHA_BUFFER_BINDING, alphaBuffer);
        ByteBuffer alphaFromGpuInBytes = (ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER, 0, alphaPixels * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
        alphaFromGpuInBytes.order(ByteOrder.nativeOrder());
        IntBuffer alphaFromGpu = alphaFromGpuInBytes.asIntBuffer();

        final long start = System.currentTimeMillis(); {
            if(tempIntBuffer.length < alphaPixels)
                tempIntBuffer = new int[alphaPixels];

            alphaFromGpu.get(tempIntBuffer, 0, alphaPixels);
            args.alpha.setPixels(tempIntBuffer, 0, width, 0, 0, width, height);

            // FIXME: 16.02.2016 performance improvements
            // following code should work but I not able to make it work even for RGBA_8888 bitmap.
            // If I would optimize read and writes, here is the good place where to start.
            // Copying right to the bitmap takes 1ms while copying to array and then back to bitmap
            // takes 7ms. Also I would save memory for tempIntBuffer.
            /*/
            alphaFromGpuInBytes.rewind();
            args.alpha.copyPixelsFromBuffer(alphaFromGpuInBytes);
            /**/
        }
        final long end = System.currentTimeMillis();
        Log.e("foo", String.format("alpha copying %d [ms]", end - start));

        // delete all stuff
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        GLES31.glUnmapBuffer(GLES31.GL_ATOMIC_COUNTER_BUFFER);
        ShaderHelper.checkGlError(this, "glUnmapBuffer");
        GLES31.glDeleteTextures(textures.length, textures, 0);
        ShaderHelper.checkGlError(this, "glDeleteTextures");

        args.getResultCallback().success(this, args);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void  bindTexture(Bitmap image, int texture, int index) {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + index);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, image, 0);
    }

    @Override
    public void validate(ComputeShaderArgs args) {

    }

    @Override
    public void onShaderError(String message, int error) {
        throw new RuntimeException(message + ": glError" + GLCodes.toString(error));
    }
}
