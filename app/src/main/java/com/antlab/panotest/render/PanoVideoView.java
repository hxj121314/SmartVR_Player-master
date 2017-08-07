package com.antlab.panotest.render;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.antlab.panotest.PanoSE;
import com.antlab.panotest.Shade;
import com.antlab.panotest.Sphere;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class PanoVideoView extends PanoView implements GLSurfaceView.Renderer, PanoSE.updateSensorMatrix, SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnVideoSizeChangedListener {
    private static final String TAG = PanoVideoView.class.getSimpleName();
    private GLSurfaceView m_glsv;
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
    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;

    private PanoVideoView() {
    }

    public PanoVideoView setGLSurface(GLSurfaceView gl) {
        m_glsv = gl;
        m_glsv.setEGLContextClientVersion(2);
        m_glsv.setRenderer(this);
        m_glsv.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return this;
    }

    public static PanoVideoView build() {
        return new PanoVideoView();
    }

    public PanoVideoView init(Context context) {
        m_sphere = new Sphere(18, 100);
        Matrix.setIdentityM(modelMatrix, 0);
        m_pse.init((Activity) context, this);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/360Video/LY.mp4"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        return this;
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        Log.i(TAG, "onDraw");
        surfaceTexture.updateTexImage();

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

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
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d(TAG, "onVideoSizeChanged: " + width + " " + height);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        programId = Shade.createProgram(Shade.vertex, Shade.video_frag);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        textureId = Shade.loadTexture();

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        surface.release();

        try {
            mediaPlayer.prepare();
        } catch (IOException t) {
            Log.e(TAG, "media player prepare failed");
        }
        mediaPlayer.start();
    }

    private void writeSDcard(String str) {

        try {
            // if the SDcard exists 判断是否存在SD卡
            if (Environment.getExternalStorageState().equals(                //Toast.makeText(this,"已经写入",Toast.LENGTH_LONG).show();

                    Environment.MEDIA_MOUNTED)) {
                // get the directory of the SDcard 获取SD卡的目录
                File sdDire = Environment.getExternalStorageDirectory();
                //Toast.makeText(this,"开始写入",Toast.LENGTH_LONG).show();
                FileOutputStream outFileStream = new FileOutputStream(
                        sdDire.getCanonicalPath() + "/360Video/" + "mDATA" +".pcap", true);
                outFileStream.write(str.getBytes());
                outFileStream.close();
                //Toast.makeText(this,"写入成功",Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        float[] orientationValues = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        writeSDcard("azimuth: " + "\t" + String.valueOf(Math.toDegrees(orientationValues[0])) + "\t" +"pitch: " + "\t" +String.valueOf(Math.toDegrees(orientationValues[1])) + "\t" +"roll: " + "\t" +String.valueOf(Math.toDegrees(orientationValues[2])) + "\t" +"\n");
        m_glsv.requestRender();
        Log.i(TAG, "requestRender");
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        m_glsv.requestRender();
    }
}
