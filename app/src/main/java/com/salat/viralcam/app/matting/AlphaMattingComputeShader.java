package com.salat.viralcam.app.matting;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.os.Build;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Marek on 08.02.2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AlphaMattingComputeShader implements ComputeShader, OnShaderArgsValidation, ShaderHelper.OnShaderError {
    private static final int IMAGE_BINDING = 0;
    private static final int TRIMAP_BINDING = 1;

    private static final int ALPHA_BUFFER_BINDING = 0;
    private static final int FOREGROUND_BOUNDARY_BINDING = 1;
    private static final int BACKGROUND_BUFFER_BINDING = 2;
    private static final int ATOMIC_BUFFER_BINDING = 3;
    private static final int SAMPLE_BUFFER_BINDING = 4;

    private static final int BOUNDARY_SIZE = 16000;
    private static final int BOUNDARY_BUFFER_CAPACITY = 2 * BOUNDARY_SIZE; // (x, y)
    private static final int WORKGROUP_SIZE = 32;
    public static final int ALPHA_PATCHMATCH_ITERATIONS = 8;

    static class SandboxVars {
        public int program;
        public int dimensionsLocation;
        public int imageLocation;
        public int trimapLocation;

        public void init() {
            dimensionsLocation = GLES31.glGetUniformLocation(program, "dimensions");
            imageLocation = GLES31.glGetUniformLocation(program, "image");
            trimapLocation = GLES31.glGetUniformLocation(program, "trimap");
        }
    }
    static class FindBoundaryVars {
        public int program;
        public int dimensionsLocation;
        public int boundarySizeLocation;
        public int trimapLocation;

        public void init() {
            dimensionsLocation = GLES31.glGetUniformLocation(program, "dimensions");
            trimapLocation = GLES31.glGetUniformLocation(program, "trimap");
            boundarySizeLocation = GLES31.glGetUniformLocation(program, "boundary_size");
        }
    }
    static class ExtendBoundaryVars extends FindBoundaryVars {}
    static class InitializeSamplesVars extends FindBoundaryVars {}
    static class AlphaPatchMatchVars extends FindBoundaryVars {
        private int imageLocation;

        @Override
        public void init() {
            super.init();
            imageLocation = GLES31.glGetUniformLocation(program, "image");
        }
    }
    static class UpdateAlphaMaskVars extends FindBoundaryVars {}

    private SandboxVars sandbox;
    private FindBoundaryVars findBoundary;
    private ExtendBoundaryVars extendBoundary;
    private InitializeSamplesVars initializeSamples;
    private AlphaPatchMatchVars alphaPatchMatch;
    private UpdateAlphaMaskVars updateAlphaMask;


    private Resources resources;
    private final IntBuffer atomicCounters = IntBuffer.allocate(2);
    private int trimapTexture;
    private int imageTexture;
    private int foregroundBoundaryBuffer;
    private int backgroundBoundaryBuffer;
    private int atomicCounterBuffer;
    private int alphaBuffer;
    private int sampleBuffer;
    private int[] textures;
    private int[] buffers;

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
//        sandbox = new SandboxVars();
//        sandbox.program = ShaderHelper.compileShader("sandbox shader", this, gl,
//                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_sandbox));
//        sandbox.init();
//        ShaderHelper.checkGlError(this, "sandbox.init");

        findBoundary = new FindBoundaryVars();
        findBoundary.program = ShaderHelper.compileShader("find boundary shader", this, gl,
                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_find_boundary));
        findBoundary.init();
        ShaderHelper.checkGlError(this, "findBoundary.init");

        extendBoundary = new ExtendBoundaryVars();
        extendBoundary.program = ShaderHelper.compileShader("extend boundary shader", this, gl,
                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_extend_boundary));
        extendBoundary.init();
        ShaderHelper.checkGlError(this, "extendBoundary.init");

        initializeSamples = new InitializeSamplesVars();
        initializeSamples.program = ShaderHelper.compileShader("initialize samples", this, gl,
                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_initialize_samples));
        initializeSamples.init();
        ShaderHelper.checkGlError(this, "extendBoundary.init");

        alphaPatchMatch = new AlphaPatchMatchVars();
        alphaPatchMatch.program = ShaderHelper.compileShader("alpha patch match", this, gl,
                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_alpha_patchmatch));
        alphaPatchMatch.init();
        ShaderHelper.checkGlError(this, "alphaPatchMatch.init");

        updateAlphaMask = new UpdateAlphaMaskVars();
        updateAlphaMask.program = ShaderHelper.compileShader("update alpha mask", this, gl,
                AssetLoader.loadFromRaw(resources, R.raw.alpha_matte_update_alpha));
        updateAlphaMask.init();
        ShaderHelper.checkGlError(this, "alphaPatchMatch.init");

        textures = new int[2];
        GLES31.glGenTextures(textures.length, textures, 0);
        ShaderHelper.checkGlError(this, "glGenTextures");

        trimapTexture = textures[0];
        imageTexture = textures[1];

        buffers = new int[5];
        GLES31.glGenBuffers(buffers.length, buffers, 0);
        ShaderHelper.checkGlError(this, "glGenBuffers");

        foregroundBoundaryBuffer = buffers[0];
        backgroundBoundaryBuffer = buffers[1];
        atomicCounterBuffer = buffers[2];
        alphaBuffer = buffers[3];
        sampleBuffer = buffers[4];
    }

    int tempIntBuffer[] = new int[1000 * 1000];
    @Override
    public void compute(ComputeShaderArgs _args) {
        Args args = (Args) _args;
        final int width = args.image.getWidth();
        final int height = args.image.getHeight();

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
        ShaderHelper.checkGlError(this, "alphaBuffer, glBindBuffer, glBufferData, glBindBufferBase, alpha");

        // bind foreground boundary
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, foregroundBoundaryBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, BOUNDARY_BUFFER_CAPACITY * Size.ofInt(), null, GLES31.GL_DYNAMIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, FOREGROUND_BOUNDARY_BINDING, foregroundBoundaryBuffer);
        ShaderHelper.checkGlError(this, "foregroundBoundaryBuffer, glBindBuffer, glBufferData, glBindBufferBase");

        // bind background boundary
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, backgroundBoundaryBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, BOUNDARY_BUFFER_CAPACITY * Size.ofInt(), null, GLES31.GL_DYNAMIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, BACKGROUND_BUFFER_BINDING, backgroundBoundaryBuffer);
        ShaderHelper.checkGlError(this, "backgroundBoundaryBuffer, glBindBuffer, glBufferData, glBindBufferBase");

        // bind background boundary
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, sampleBuffer);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, alphaPixels * Size.ofInt() * 6, null, GLES31.GL_DYNAMIC_READ);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, SAMPLE_BUFFER_BINDING, sampleBuffer);
        ShaderHelper.checkGlError(this, "sampleBuffer, glBindBuffer, glBufferData, glBindBufferBase");

//        // use program sandbox
//        GLES31.glUseProgram(sandbox.program);
//        ShaderHelper.checkGlError(this, "glUseProgram");{
//            // set dimensions
//            GLES31.glUniform2i(sandbox.dimensionsLocation, width, height);
//            GLES31.glUniform1i(sandbox.imageLocation, IMAGE_BINDING);
//            GLES31.glUniform1i(sandbox.trimapLocation, TRIMAP_BINDING);
//            ShaderHelper.checkGlError(this, "glUniform1i");
//            dispatchComputeAndWait(width, height);
//
//            // read all stuff
//            // read atomic counters
//            GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, ATOMIC_BUFFER_BINDING, atomicCounterBuffer);
//            IntBuffer atomicCountersResult = ((ByteBuffer) GLES31.glMapBufferRange(
//                    GLES31.GL_ATOMIC_COUNTER_BUFFER, 0, atomicCounters.capacity() * Size.ofInt(), GLES31.GL_MAP_READ_BIT))
//                    .order(ByteOrder.nativeOrder())
//                    .asIntBuffer();
//
//            Log.e("foo", String.format("counters %d, %d", atomicCountersResult.get(0), atomicCountersResult.get(1)));
//
//            // read alpha values
//            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, ALPHA_BUFFER_BINDING, alphaBuffer);
//            ByteBuffer alphaFromGpuInBytes = (ByteBuffer) GLES31.glMapBufferRange(
//                    GLES31.GL_SHADER_STORAGE_BUFFER, 0, alphaPixels * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
//            alphaFromGpuInBytes.order(ByteOrder.nativeOrder());
//            IntBuffer alphaFromGpu = alphaFromGpuInBytes.asIntBuffer();
//
//            final long start = System.currentTimeMillis(); {
//                if(tempIntBuffer.length < alphaPixels)
//                    tempIntBuffer = new int[alphaPixels];
//
//                alphaFromGpu.get(tempIntBuffer, 0, alphaPixels);
//                args.alpha.setPixels(tempIntBuffer, 0, width, 0, 0, width, height);
//
//                // FIXME: 16.02.2016 performance improvements
//                // following code should work but I not able to make it work even for RGBA_8888 bitmap.
//                // If I would optimize read and writes, here is the good place where to start.
//                // Copying right to the bitmap takes 1ms while copying to array and then back to bitmap
//                // takes 7ms. Also I would save memory for tempIntBuffer.
//            /*/
//            alphaFromGpuInBytes.rewind();
//            args.alpha.copyPixelsFromBuffer(alphaFromGpuInBytes);
//            /**/
//            }
//            final long end = System.currentTimeMillis();
//            Log.e("foo", String.format("alpha copying %d [ms]", end - start));
//        }

        // use program findBoundary
        GLES31.glUseProgram(findBoundary.program);
        ShaderHelper.checkGlError(this, "glUseProgram"); {
            // set dimensions
            GLES31.glUniform2i(findBoundary.dimensionsLocation, width, height);
            GLES31.glUniform1ui(findBoundary.boundarySizeLocation, BOUNDARY_SIZE);
            GLES31.glUniform1i(findBoundary.trimapLocation, TRIMAP_BINDING);
            ShaderHelper.checkGlError(this, "glUniform1i");

            dispatchComputeAndWait(width, height);
        }

        // use program extendBoundary
        GLES31.glUseProgram(extendBoundary.program);
        ShaderHelper.checkGlError(this, "glUseProgram"); {
            // set dimensions
            GLES31.glUniform2i(extendBoundary.dimensionsLocation, width, height);
            GLES31.glUniform1ui(extendBoundary.boundarySizeLocation, BOUNDARY_SIZE);
            GLES31.glUniform1i(extendBoundary.trimapLocation, TRIMAP_BINDING);
            ShaderHelper.checkGlError(this, "glUniform1i");

            dispatchComputeAndWait(width, height);
        }
        
        // TODO: 16.02.2016 sort boundary pixels by luminescence

        GLES31.glBindBufferBase(GLES31.GL_ATOMIC_COUNTER_BUFFER, ATOMIC_BUFFER_BINDING, atomicCounterBuffer);
        IntBuffer atomicCountersResult = ((ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_ATOMIC_COUNTER_BUFFER, 0, atomicCounters.capacity() * Size.ofInt(), GLES31.GL_MAP_READ_BIT))
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();

        final int minBoundarySize = Math.min(BOUNDARY_SIZE, Math.min(atomicCountersResult.get(0), atomicCountersResult.get(1)));

        // use program initializeSamples
        GLES31.glUseProgram(initializeSamples.program);
        ShaderHelper.checkGlError(this, "glUseProgram"); {
            // set dimensions
            GLES31.glUniform2i(initializeSamples.dimensionsLocation, width, height);
            GLES31.glUniform1ui(initializeSamples.boundarySizeLocation, minBoundarySize);
            GLES31.glUniform1i(initializeSamples.trimapLocation, TRIMAP_BINDING);
            ShaderHelper.checkGlError(this, "glUniform1i");

            dispatchComputeAndWait(width, height);
        }

        // use program alphaPatchMatch
        GLES31.glUseProgram(alphaPatchMatch.program);
        ShaderHelper.checkGlError(this, "glUseProgram"); {
            // set dimensions
            GLES31.glUniform2i(alphaPatchMatch.dimensionsLocation, width, height);
            GLES31.glUniform1ui(alphaPatchMatch.boundarySizeLocation, minBoundarySize);
            GLES31.glUniform1i(alphaPatchMatch.trimapLocation, TRIMAP_BINDING);
            GLES31.glUniform1i(alphaPatchMatch.imageLocation, IMAGE_BINDING);
            ShaderHelper.checkGlError(this, "glUniform1i");

            for (int i = 0; i < ALPHA_PATCHMATCH_ITERATIONS; i++) {
                dispatchComputeAndWait(width, height);
            }
        }

        // use program updateAlphaMask
        GLES31.glUseProgram(updateAlphaMask.program);
        ShaderHelper.checkGlError(this, "glUseProgram"); {
            // set dimensions
            GLES31.glUniform2i(updateAlphaMask.dimensionsLocation, width, height);
            GLES31.glUniform1ui(updateAlphaMask.boundarySizeLocation, BOUNDARY_SIZE);
            GLES31.glUniform1i(updateAlphaMask.trimapLocation, TRIMAP_BINDING);
            ShaderHelper.checkGlError(this, "glUniform1i");

            dispatchComputeAndWait(width, height);
        }

        setShaderResult(args, width, height, alphaPixels);

        // delete all mapped stuff
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
//        GLES31.glUnmapBuffer(GLES31.GL_ATOMIC_COUNTER_BUFFER);
        ShaderHelper.checkGlError(this, "glUnmapBuffer");

        args.getResultCallback().success(this, args);
    }

    private void setShaderResult(Args args, int width, int height, int alphaPixels) {
        // read alpha values
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, ALPHA_BUFFER_BINDING, alphaBuffer);
        ByteBuffer alphaFromGpuInBytes = (ByteBuffer) GLES31.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER, 0, alphaPixels * Size.ofInt(), GLES31.GL_MAP_READ_BIT );
        alphaFromGpuInBytes.order(ByteOrder.nativeOrder());
        IntBuffer alphaFromGpu = alphaFromGpuInBytes.asIntBuffer();

        final long start = System.currentTimeMillis();
        {
            if (tempIntBuffer.length < alphaPixels)
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
    }

    private void dispatchComputeAndWait(int width, int height) {
        // run forest run
        GLES31.glDispatchCompute(
                ShaderHelper.getDispatchSize(width, WORKGROUP_SIZE),
                ShaderHelper.getDispatchSize(height, WORKGROUP_SIZE), 1);
        ShaderHelper.checkGlError(this, "glDispatchCompute");

        // wait a bit
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
    }

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
        throw new RuntimeException(message + ": glError " + GLCodes.toString(error));
    }
}
