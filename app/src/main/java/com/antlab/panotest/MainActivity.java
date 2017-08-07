package com.antlab.panotest;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.content.pm.ActivityInfo;

import com.antlab.panotest.render.PanoView;
import com.antlab.panotest.render.PanoImageView;
import com.antlab.panotest.render.PanoVideoView;

public class MainActivity extends AppCompatActivity {
    private PanoView m_panoview;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        init();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    private void init() {
        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        m_panoview = PanoVideoView.build().setGLSurface(glSurfaceView).init(MainActivity.this);
        Log.i(TAG, "init");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_panoview.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_panoview.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_panoview.resume();
    }
}
