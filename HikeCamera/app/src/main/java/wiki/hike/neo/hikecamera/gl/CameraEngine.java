package wiki.hike.neo.hikecamera.gl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.content.Context;
import java.io.IOException;

import android.util.Log;

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

    //Commands are passed to camera engine and is should be handled camera.
    //Should help in recognising camera calls when it spreads in a big code base.
    //Any client using this engine can only interacts using commands.
    //So bug fixing and code maintainance will be easy.


    public static final int NO_COMMAND = 0;
    public static final int COMMAND_FLIP = 1;
    public static final int COMMAND_SNAPSHOT = 2;
    public static final int COMMAND_RECORD_ON = 3;
    public static final int COMMAND_RECORD_STOP = 4;


    public static int RECORDING_STATE_ON = 0;
    public static int RECORDING_STATE_OFF = 1;

    CameraManager mCameraManager;
    GLSurfaceView mSurfaceView;
    CameraRenderer mCameraRenderer;
    Context mContext;

    int mRecordingState;

    int mCurrentCommand = NO_COMMAND;

    //Create a filter factory;
    Filter mFilter;

    private final RendererObserver mObserverRenderer = new RendererObserver();

    public CameraEngine(GLSurfaceView surfaceView, Context context) {
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

    public void setGLSurfaceView(GLSurfaceView surfaceView) {
    }

    public void onDestroy() {
    }

    public void flip() {
        mCameraManager.flipit();
        mCameraRenderer.flip(mCameraManager.getCameraFacing());
        Log.e("CAMERA_ENGINE", "Camera facing : " + mCameraManager.getCameraFacing());
    }

    public void startRecording()
    {
        setRecordingState(RECORDING_STATE_ON);
    }

    public void stopRecording()
    {
        setRecordingState(RECORDING_STATE_OFF);
    }

    public void onResume()
    {
        mSurfaceView.onResume();
        //Check if we need to start the camera.
    }

    public  void onPause()
    {
        if(getmRecordingState() == RECORDING_STATE_ON)
            stopRecording();

        mSurfaceView.onPause();
        mCameraManager.onPause();
    }

    public void processCommand(int command)
    {
        mCurrentCommand = command;
        switch (command)
        {
            case COMMAND_FLIP:
                flip();
                Log.e("ENGINE","Command" + command);
                break;
            case COMMAND_SNAPSHOT:
                Log.e("ENGINE","Command" + command);
                break;
            default:
                Log.e("ENGINE","Unregocnisable command" + command);
                break;
        }

    }

    void setRecordingState(int state)
    {
        mRecordingState = state;
    }

    int getmRecordingState()
    {
        return mRecordingState;
    }

    private class RendererObserver implements CameraRenderer.Observer{
        @Override
        public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture){
            mCameraManager.setSurfaceTexture(surfaceTexture);
            mCameraRenderer.initPreviewFrameRenderer(mCameraManager.getPreviewWidth(),mCameraManager.getPrevieHeight());
        }
    }
}
