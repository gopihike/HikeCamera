package wiki.hike.neo.hikecamera.gl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.content.Context;

import java.io.IOException;

/**
 * Created by Neo on 09/10/17.
 */

//Programming practices to be followed
//Please try to right only pure functions.
//Less or no use of booleans.
//Use state machine instead of bools.


//Order of init :
    //Create manager
    //Create renderer
    //Create filter from factory and set it in renderer
    //Set renderer in surfaceview.


public class CameraEngine {
    CameraManager mCameraManager;
    GLSurfaceView mSurfaceView;
    CameraRenderer mCameraRenderer;
    Context mContext;

    //Create a filter factory;
    Filter mFilter;

    private final RendererObserver mObserverRenderer = new RendererObserver();

    public CameraEngine(GLSurfaceView surfaceView,Context context)
    {
        mSurfaceView = surfaceView;
        mContext = context;

        //Open the camera.
        mCameraManager = new CameraManager();

        //Initiliase the GLSurfaceView.
        mCameraRenderer = new CameraRenderer(mContext);
        mCameraRenderer.setCameraManager(mCameraManager);
        mCameraRenderer.setObserver(mObserverRenderer);

        //mFilter = new Filter();
        //mCameraRenderer.setFilter(mFilter);

        mCameraRenderer.setSurfaceView(mSurfaceView);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);

        //Call back generated.
        mSurfaceView.setRenderer(mCameraRenderer);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setGLSurfaceView(GLSurfaceView surfaceView)
    {
    }

    public void onDestroy()
    {
    }

    public void onResume()
    {
        mSurfaceView.onResume();
    }

    public  void onPause()
    {
        mSurfaceView.onPause();
    }


    private class RendererObserver implements CameraRenderer.Observer{
        @Override
        public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture,int width,int height){
            mCameraManager.setSurfaceTexture(surfaceTexture,width,height);
            mCameraRenderer.initRenderer();
        }
    }
}
