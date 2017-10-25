package wiki.hike.neo.hikecamera.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import wiki.hike.neo.hikecamera.encoder.MediaAudioEncoder;
import wiki.hike.neo.hikecamera.encoder.MediaEncoder;
import wiki.hike.neo.hikecamera.encoder.MediaMuxerWrapper;
import wiki.hike.neo.hikecamera.encoder.MediaVideoEncoder;


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

    public interface VideoCallBack{
        void onVideoDownloadCallback(String downloadState);
    }

    //Commands are passed to camera engine and is should be handled by camera.
    //Should help in recognising camera calls when it spreads in a big code base.
    //Any client using this engine can only interact using commands.
    //So bug fixing and code maintainance will be easy.
    public static final int NO_COMMAND = 0;
    public static final int COMMAND_FLIP = 1;
    public static final int COMMAND_SNAPSHOT = 2;
    public static final int COMMAND_RECORD = 3;

    public static final int RECORDING_STATE_ON = 0;
    public static final int RECORDING_STATE_OFF = 1;

    public static int ENGINE_STATE_CREATE = 0;
    public static int ENGINE_STATE_RESUME = 1;
    public static int ENGINE_STATE_PAUSE = 2;
    public static int ENGINE_STATE_NOT_DEFINED = 3;

    CameraManager mCameraManager;
    GLSurfaceView mSurfaceView;
    CameraRenderer mCameraRenderer;

    //Video .Code for video recording.
    private MediaMuxerWrapper mMuxer;
    private File mVideoFile;
    Context mContext;

    int mRecordingState = RECORDING_STATE_OFF;

    int mCurrentCommand = NO_COMMAND;

    int mEngineState = ENGINE_STATE_NOT_DEFINED;

    //Create a filter factory;
    Filter mFilter;

    private final RendererObserver mObserverRenderer = new RendererObserver();

    public CameraEngine(GLSurfaceView surfaceView, Context context) {
        mSurfaceView = surfaceView;
        mContext = context;

        //Open the camera.
        mCameraManager = new CameraManager();

        //Initiliase the GLSurfaceView.
        mCameraRenderer = new CameraRenderer(mContext,mCameraManager.getWidth(),mCameraManager.getHeight());
        mCameraRenderer.setCameraManager(mCameraManager);
        mCameraRenderer.setObserver(mObserverRenderer);

        //mFilter = new Filter();
        //mCameraRenderer.setFilter(mFilter);
        mCameraRenderer.setSurfaceView(mSurfaceView);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);

        mFilter = new FilterPreviewBuffer();
        mCameraRenderer.setFilter(mFilter);

        //Call back generated.
        mSurfaceView.setRenderer(mCameraRenderer);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setEngineState(ENGINE_STATE_CREATE);
    }

    public void setGLSurfaceView(GLSurfaceView surfaceView) {
    }

    public void onDestroy() {

    }

    //Inflip operation camera is destoryed and created again so previewBuffer needs to be created
    public void flip() {
        mCameraManager.flipit();
        mCameraRenderer.flipWithCallBackReset(mCameraManager.getCameraFacing());

        Log.e("ENGINE", "Camera facing : " + mCameraManager.getCameraFacing());
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
        if(getEngineState() == ENGINE_STATE_PAUSE)
        {
            //Green frame might be seen when Filter type is preview buffer because for one frame cycle there will be no camera frame available for renderer to draw
            //but draw call will be active so draw will be called and because of YUV to RGB conversion logic green frame might be seen.
            //Fix would be not to draw until valid camera frame is available.
            mCameraManager.onResume();
            mCameraRenderer.setCallBackBasedOnFilter();
        }
        mSurfaceView.onResume();

        setEngineState(ENGINE_STATE_RESUME);
    }

    public  void onPause()
    {
        if(getmRecordingState() == RECORDING_STATE_ON)
            stopRecording();

        mSurfaceView.onPause();
        mCameraManager.onPause();
        setEngineState(ENGINE_STATE_PAUSE);
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
                takePicture();
                Log.e("ENGINE","Command" + command);
                break;
            case COMMAND_RECORD:
                int recordingState =  getmRecordingState() == CameraEngine.RECORDING_STATE_ON ? CameraEngine.RECORDING_STATE_OFF : CameraEngine.RECORDING_STATE_ON;
                setRecordingState(recordingState);
                break;
            default:
                Log.e("ENGINE","Unregocnisable command" + command);
                break;
        }
    }

    void setRecordingState(int state) {
        mRecordingState = state;
        switch (mRecordingState) {
            case RECORDING_STATE_ON:
                try {
                    String fileName = "TestVideo" + System.currentTimeMillis() + ".mp4";
                    //File cacheDir = mContext.getCacheDir();
                    //cacheDir.mkdirs();
                    String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/Hike";
                    File dir = new File(file_path);
                    if(!dir.exists())
                        dir.mkdirs();

                    mVideoFile = new File(dir, fileName);
                    mMuxer = new MediaMuxerWrapper(".mp4", mVideoFile.getAbsolutePath());    // for audio change to m4a
                    new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraRenderer.getSurfaceWidth(), mCameraRenderer.getSurfaceHeight());
                    new MediaAudioEncoder(mMuxer, mMediaEncoderListener, null);
                    mMuxer.prepare();
                    mMuxer.startRecording();
                } catch (final IOException e)
                {

                }
                break;
            case RECORDING_STATE_OFF:
                if (mMuxer != null) {
                    mMuxer.stopRecording();
                    mMuxer = null;
                }
                break;
        }
    }

    public int getmRecordingState()
    {
        return mRecordingState;
    }

    void setEngineState(int state)
    {
        mEngineState = state;
    }

    int getEngineState()
    {
        return mEngineState;
    }

    private class RendererObserver implements CameraRenderer.Observer{
        @Override
        public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture){
            mCameraManager.setSurfaceTexture(surfaceTexture);
            mCameraRenderer.initPreviewFrameRenderer(/*mCameraManager.getWidth(),mCameraManager.getHeight()*/);
        }
    }

    private void takePicture(){

        if(mCameraRenderer != null){
            mCameraRenderer.takePicture(new TakePicture());
        }
    }

    private class TakePicture implements Filter.TakePictureListener{

        @Override
        public void onPictureTaken(Bitmap bitmap) {

            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Hike";
            File dir = new File(file_path);
            if(!dir.exists())
                dir.mkdirs();
            File file = new File(dir, "snapshot.jpg");
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            try {
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder)
                mCameraRenderer.setVideoEncoder((MediaVideoEncoder) encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder)
                mCameraRenderer.setVideoEncoder(null);
        }
    };
}
