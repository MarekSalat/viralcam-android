package com.salat.viralcam.app.computeshader;

import android.annotation.TargetApi;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.os.Build;
import android.util.Log;

import com.salat.viralcam.app.util.GLCodes;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Marek on 09.02.2016.
 */
public class ShaderHelper {

    private static final String TAG = "ShaderHelper";

    public static interface OnShaderError {
        void onShaderError(String message, int error);
    }

    public static int getDispatchSize(int workSize, int workGroupSize){
        return (workSize % workGroupSize > 0) ?  workSize / workGroupSize + 1 : workSize / workGroupSize;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    static boolean supportsGLES31(GL10 gl) {
        int[] vers = new int[2];

        gl.glGetIntegerv(GLES30.GL_MAJOR_VERSION, vers, 0);
        gl.glGetIntegerv(GLES30.GL_MINOR_VERSION, vers, 1);

        return vers[0] > 3 || (vers[0] == 3 && vers[1] >= 1);
    }

    public static int compileShader(OnShaderError callack, GL10 gl, String shaderSource) {
        return compileShader("", callack, gl, shaderSource);
    }

    public static int compileShader(String name, OnShaderError callack, GL10 gl, String shaderSource){
        if(gl == null)
            throw new IllegalArgumentException("GLES context must be initialized");

        if(!ShaderHelper.supportsGLES31(gl)){
            Log.e(TAG, "device does not supports GLES31");
            throw new IllegalArgumentException("Device does not supports GLES31");
        }

        int program = GLES31.glCreateProgram();
        ShaderHelper.checkGlError(callack, name + " glCreateProgram");

        int shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);
        ShaderHelper.checkGlError(callack, name + " glCreateShader");

        GLES31.glShaderSource(shader, shaderSource);
        ShaderHelper.checkGlError(callack, name + " glShaderSource");

        GLES31.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, name + " Could not compile shader " + ":");
            Log.e(TAG, GLES31.glGetShaderInfoLog(shader));
        }
        ShaderHelper.checkGlError(callack, name + " glCompileShader");

        GLES31.glAttachShader(program, shader);
        ShaderHelper.checkGlError(callack, name + " glAttachShader");

        GLES31.glLinkProgram(program);
        ShaderHelper.checkGlError(callack, name + " glLinkProgram");

        return program;
    }


    public static void checkGlError(OnShaderError callback, String op) {
        int error;
        int prevError = 0;
        boolean hadError = false;
        while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
            Log.e(TAG, op + String.format(": glError %d, %x. %s", error, error, GLCodes.toString(error)));
            prevError = error;
            hadError = true;
        }
        if(hadError)
            callback.onShaderError(op, prevError);
    }
}
