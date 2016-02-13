package com.salat.viralcam.app.matting;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
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
import com.salat.viralcam.app.util.Constants;
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
    public static final int IMAGE_SAMPLER_INDEX = 0;
    public static final int TRIMAP_SAMPLER_INDEX = 1;
    private static final int WORKGROUP_SIZE = 32;
    public static final IntBuffer ATOMIC_COUNTER_1 = IntBuffer.allocate(1);
    public static final IntBuffer ATOMIC_COUNTER_2 = IntBuffer.allocate(1);

    private int program;
    private Resources resources;
    private int dimensionsLocation;
    private int imageLocation;
    private int trimapLocation;
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
    }

    private String shaderSource;
    @NonNull
    private String loadShaderFromFile() {
        if(shaderSource == null)
            shaderSource = AssetLoader.loadFromRaw(resources, R.raw.alpha_matte);
        return shaderSource;
    }

    int tempIntBuffer[] = new int[1000 * 1000];
    IntBuffer alphaBuffer = IntBuffer.allocate(1000 * 1000);
    // buffer values are points (x, y)
    IntBuffer foregroundBoundryBuffer = IntBuffer.allocate(10000);
    IntBuffer backgroundBoundaryBuffer = IntBuffer.allocate(10000);
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
        GLES31.glUniform1i(imageLocation, IMAGE_SAMPLER_INDEX);
        GLES31.glUniform1i(trimapLocation, TRIMAP_SAMPLER_INDEX);
        ShaderHelper.checkGlError(this, "glUniform1i");

        // bind textures image and trimap
        int[] samplers = new int[2];
        GLES31.glGenSamplers(samplers.length, samplers, 0);
        bindTexture(args.image, textures[0], IMAGE_SAMPLER_INDEX, samplers[0]);
        bindTexture(args.trimap, textures[1], TRIMAP_SAMPLER_INDEX, samplers[1]);
        ShaderHelper.checkGlError(this, "bindTexture");

        // create buffer for alpha
        int alphaPixels = width * height;
        int[] buffers = new int[4];
        GLES31.glGenBuffers(buffers.length, buffers, 0);
        ShaderHelper.checkGlError(this, "glGenBuffers");

        // bind atomic counters
        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, 3, buffers[2]);
        ATOMIC_COUNTER_1.put(0, 0);
        ATOMIC_COUNTER_2.put(0, 0);
        GLES31.glBufferData(GLES31.GL_ATOMIC_COUNTER_BUFFER, Size.ofInt(), ATOMIC_COUNTER_1, GLES31.GL_DYNAMIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, 4, buffers[3]);
        GLES31.glBufferData(GLES31.GL_ATOMIC_COUNTER_BUFFER, Size.ofInt(), ATOMIC_COUNTER_2, GLES31.GL_DYNAMIC_DRAW);
        ShaderHelper.checkGlError(this, "glBindBufferBase atomic");

        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffers[0]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, alphaPixels * Size.ofInt(), alphaBuffer, GLES31.GL_STATIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        ShaderHelper.checkGlError(this, "buffer0, glBindBuffer, glBufferData, glBindBufferBase");

        // run forest run
        GLES31.glDispatchCompute(
                ShaderHelper.getDispatchSize(width, WORKGROUP_SIZE),
                ShaderHelper.getDispatchSize(height, WORKGROUP_SIZE), 1);
        ShaderHelper.checkGlError(this, "glDispatchCompute");

        // wait a bit
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        // GL_COMPUTE_SHADER_BIT is the same as GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
        //GLES31.glMemoryBarrier(GLES31.GL_COMPUTE_SHADER_BIT);

        // read all stuff
        // read atomic counters
        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, 3, buffers[2]);
        int counter1 = ((ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_ATOMIC_COUNTER_BUFFER, 0, Size.ofInt(), GLES31.GL_MAP_READ_BIT))
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .get(0);

        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, 4, buffers[3]);
        int counter2 = ((ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_ATOMIC_COUNTER_BUFFER, 0, Size.ofInt(), GLES31.GL_MAP_READ_BIT))
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
                .get(0);

        Log.e("foo", String.format("counters %d, %d", counter1, counter2));

        // read alpha values
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, buffers[0]);
        ByteBuffer alphaFromGpuInBytes = (ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER, 0, alphaPixels * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
        alphaFromGpuInBytes.order(ByteOrder.nativeOrder());
        IntBuffer alphaFromGpu = alphaFromGpuInBytes.asIntBuffer();

        for (int i = 0; i < alphaPixels; i++) {
            tempIntBuffer[i] = alphaFromGpu.get(i);
        }
        args.alpha.setPixels(tempIntBuffer, 0, width, 0, 0, width, height);

        // delete all stuff
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        GLES31.glUnmapBuffer(GLES31.GL_ATOMIC_COUNTER_BUFFER);
        ShaderHelper.checkGlError(this, "glUnmapBuffer");
        GLES31.glDeleteTextures(textures.length, textures, 0);
        ShaderHelper.checkGlError(this, "glDeleteTextures");

        args.getResultCallback().success(this, args);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void  bindTexture(Bitmap image, int texture, int index, int sampler) {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + index);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        //GLES31.glBindSampler(index, sampler);
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
