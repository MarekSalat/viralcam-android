package com.salat.viralcam.app.computeshader;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Marek on 08.02.2016.
 */
public interface ComputeShader {
    void initialize(GL10 gl);
    void compute(ComputeShaderArgs args);
}
