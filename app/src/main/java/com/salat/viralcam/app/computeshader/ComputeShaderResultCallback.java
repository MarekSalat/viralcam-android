package com.salat.viralcam.app.computeshader;

/**
 * Created by Marek on 08.02.2016.
 */
public interface ComputeShaderResultCallback {
    void success(ComputeShader shader, ComputeShaderArgs args);
    void error(ComputeShader shader, ComputeShaderArgs args, Exception exception);
}
