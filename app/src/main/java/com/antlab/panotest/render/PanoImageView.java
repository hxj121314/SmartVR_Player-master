package com.antlab.panotest.render;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.antlab.panotest.PanoSE;
import com.antlab.panotest.R;
import com.antlab.panotest.Shade;
import com.antlab.panotest.Sphere;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class PanoImageView extends PanoView implements GLSurfaceView.Renderer, PanoSE.updateSensorMatrix {
    private static final String TAG = PanoImageView.class.getSimpleName();
    private GLSurfaceView m_glsv;
    private Context m_context;
    private Sphere m_sphere;
    private int aPositionHandle;
    private int programId;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int textureId;
    private float[] modelMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private int uMatrixHandle;
    private PanoSE m_pse = new PanoSE();

    private PanoImageView() {
    }

    public PanoImageView setGLSurface(GLSurfaceView gl) {
        m_glsv = gl;
        m_glsv.setEGLContextClientVersion(2);
        m_glsv.setRenderer(this);
        m_glsv.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return this;
    }

    public static PanoImageView build() {
        return new PanoImageView();
    }

    public PanoImageView init(Context context) {
        m_sphere = new Sphere(18, 100);
        m_context = context;
        Matrix.setIdentityM(modelMatrix, 0);
        m_pse.init((Activity) context, this);
        return this;
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        Log.i(TAG, "onDraw");

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glUniform1i(uTextureSamplerHandle, 0);
        m_sphere.uploadVerticesBuffer(aPositionHandle);
        m_sphere.uploadTexCoordinateBuffer(aTextureCoordHandle);
        m_sphere.draw();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60, ratio, 1f, 500f);//视角为60度，近平面为1，远平面500

        Matrix.setLookAtM(viewMatrix, 0,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f);
        //相机所在点是原点：0.0f, 0.0f, 0.0f
        //相机的视线朝向：0.0f, 0.0f,1.0f
        //相机的正（头顶的）朝向：0.0f, 1.0f, 0.0f
        m_glsv.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        programId = Shade.createProgram(Shade.vertex, Shade.image_frag);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        textureId = Shade.loadTexture(m_context, R.drawable.texture_360_n2);
        m_glsv.requestRender();
    }

    @Override
    public void release() {
    }

    @Override
    public void pause() {
        m_glsv.onPause();
    }

    @Override
    public void resume() {
        m_glsv.onResume();
    }

    @Override
    public void update(float[] rotationMatrix) {
        modelMatrix = rotationMatrix;
        m_glsv.requestRender();
        Log.i(TAG, "requestRender");
    }
}
