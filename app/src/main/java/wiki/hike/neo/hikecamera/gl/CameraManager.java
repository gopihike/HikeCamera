package wiki.hike.neo.hikecamera.gl;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wiki.hike.neo.hikecamera.utils.CameraEngineBackgroundHandler;


/**
 * Created by Neo on 09/10/17.
 */

//Encapsulate android camera here.

public class CameraManager {
    private Camera mCamera;
    static int mCameraWidth = 0;
    static int mCameraHeight = 0;

    static int mOrientation = 0;
    static int mPreviewFormat = 1;

    public static int CAMERA_ACTION_TYPE_OPEN = 0;
    public static int CAMERA_ACTION_TYPE_FLIP = 1;

    static int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK; //By Default camera is facing front.
    SurfaceTexture mSurfaceTexture;
    private List<CameraManager.CameraDescriptor> mDescriptors = null;

    public CameraManager() {
        //Required for front and back camera.
        //Check for right preview size,picture size and fps.
        initializeBestPreviewSize();
    }

    public void onPause() {
        //mSurfaceTexture = null;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();
        }
        mCamera = null;
    }

    public void onResume() {
        openCamera(getCameraFacing(),CAMERA_ACTION_TYPE_OPEN);
    }

    public void onRecreateCameraOnResume()
    {


    }

    public void onDestroy()
    {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void startCamera(final int cameraFacing)
    {
        openCamera(cameraFacing,CAMERA_ACTION_TYPE_OPEN);
        /*CameraEngineBackgroundHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                openCamera(cameraFacing,CAMERA_ACTION_TYPE_OPEN);//TODO : Have separate callback for camera open.
            }
        });*/
    }

    public void openCamera(int cameraFace,int actionType) {
        mCameraFacing = cameraFace;
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(mCameraFacing);
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraFacing, info);
            mOrientation = info.orientation;
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            Camera.Parameters param = mCamera.getParameters();
            CameraManager.mPreviewFormat =  param.getPreviewFormat();
            param.setPreviewSize(getPreviewWidth(), getPrevieHeight());
            mCamera.setParameters(param);
            mCamera.startPreview();
        } catch (final Exception ex) {
        }
    }

    /*public void onResume() {
        openCamera(getCameraFacing());
    }*/

    //Code to be changed for getting best preview size.
    void initPreviewSize(int width, int height) {
        //set camera para-----------------------------------
        //mCameraWidth =0;
        //mCameraHeight =0;
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if (psize.size() > 0) {
            int i;
            for (i = 0; i < psize.size(); i++) {
                if (psize.get(i).width < width || psize.get(i).height < height)
                    break;
            }
            if (i > 0)
                i--;
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);

            mCameraWidth = psize.get(i).width;
            mCameraHeight = psize.get(i).height;
        }
        mCamera.setParameters(param);
    }

    public int getWidth() {
        return mCameraWidth;
    }

    public int getHeight() {
        return mCameraHeight;
    }

    boolean isCameraFacingFront() {
        return mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? true : false;
    }

    public void openCamera(int cameraFace) {
        mCameraFacing = cameraFace;
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(mCameraFacing);
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraFacing, info);
            mOrientation = info.orientation;
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            Camera.Parameters param = mCamera.getParameters();
            CameraManager.mPreviewFormat =  param.getPreviewFormat();
            param.setPreviewSize(getPreviewWidth(), getPrevieHeight()/*640,480*/);
            mCamera.setParameters(param);
            mCamera.startPreview();
        } catch (final Exception ex) {
        }
    }

    public void setCameraPreview(int width,int height)
    {
        Camera.Parameters param = mCamera.getParameters();
        param.setPreviewSize(width,height);
        mCamera.stopPreview();
        mCamera.setParameters(param);
        mCamera.startPreview();
    }

    public void flipit()
    {
        synchronized(this) {
            //myCamera is the Camera object
            if (Camera.getNumberOfCameras()>=2) {
                //"which" is just an integer flag
                if(getCameraFacing() == Camera.CameraInfo.CAMERA_FACING_FRONT)
                {
                    openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                else
                {
                    openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
            }
        }

    }


    public Camera getCamera()
    {
        return mCamera;
    }

    void setSurfaceTexture(SurfaceTexture texture)
    {
        //We need this handle because.
        mSurfaceTexture = texture;
        startCamera(getCameraFacing());
        //openCamera(getCameraFacing());
    }

    void setPreviewWidth(int width)
    {
        mCameraWidth = /*640*/width;
    }

    void setPreviewHeight(int height)
    {
        mCameraHeight = /*480*/height;
    }

    static int getPreviewWidth()
    {
        return mCameraWidth/*640*/;
    }

    static int getPrevieHeight()
    {
        return mCameraHeight/*480*/;
    }

    static int getCameraOrientation()
    {
        return mOrientation;
    }

    void setCameraFacing(int cameraFace)
    {
        mCameraFacing = cameraFace;
    }

    static int getCameraFacing()
    {
        return mCameraFacing;
    }

    static int getPreviewFormat()
    {
        return mPreviewFormat;
    }

    //Finds out best preview size and sends a call back.
    public void initializeBestPreviewSize() {
        /*captureRunnable = new Runnable() {
            @Override
            public void run() {*/
        try {
            if (mDescriptors == null) {
                SizeP returnSize;
                int numberOfCameras = Camera.getNumberOfCameras();
                List<CameraManager.CameraDescriptor> result = new ArrayList<CameraManager.CameraDescriptor>();
                Camera.CameraInfo info = new Camera.CameraInfo();

                for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
                    Camera.getCameraInfo(cameraId, info);
                    CameraManager.CameraDescriptor descriptor = new CameraManager.CameraDescriptor(cameraId, info);

                    Camera camera = Camera.open(descriptor.getCameraId());
                    Camera.Parameters params = camera.getParameters();

                    if (params != null) {
                        ArrayList<SizeP> sizes = new ArrayList<SizeP>();

                        for (Camera.Size size : params.getSupportedPreviewSizes()) {
                            if (size.height < 2160 && size.width < 2160) {
                                sizes.add(new SizeP(size.width, size.height));
                            }
                        }
                        descriptor.setPreviewSizes(sizes);

                        sizes = new ArrayList<SizeP>();

                        for (Camera.Size size : params.getSupportedPictureSizes()) {
                            if (!"samsung".equals(Build.MANUFACTURER) ||
                                    !"jflteuc".equals(Build.PRODUCT) ||
                                    size.width < 2048) {

                                //Size(size.width, size.height);
                                //size.width;
                                //size.height;
                                sizes.add(new SizeP(size.width, size.height));
                            }
                        }
                        descriptor.setPictureSizes(sizes);
                        result.add(descriptor);
                    }
                    camera.release();
                }
                mDescriptors = result;

                //Find the best preview size.
                ArrayList<SizeP> previewSizesBackCamera = null;
                ArrayList<SizeP> previewSizesFrontCamera = null;

                List<SizeP> usableChoices = new ArrayList();
                SizeP idealSize = new SizeP(1280, 720);

                if(numberOfCameras == 2) {
                    previewSizesBackCamera = mDescriptors.get(0).getCameraPreviewSizes();
                    previewSizesFrontCamera = mDescriptors.get(1).getCameraPreviewSizes();
                }
                else
                {
                    previewSizesBackCamera = mDescriptors.get(0).getCameraPreviewSizes();
                    previewSizesFrontCamera = mDescriptors.get(0).getCameraPreviewSizes();
                }

                for (int i = 0; i < previewSizesFrontCamera.size(); i++) {
                    for (int j = 0; j < previewSizesBackCamera.size(); j++) {

                       int height =    previewSizesFrontCamera.get(i).getHeight();

                        if (previewSizesFrontCamera.get(i).getHeight() == previewSizesBackCamera.get(j).getHeight() && previewSizesFrontCamera.get(i).getWidth() == previewSizesBackCamera.get(j).getWidth()) {
                            usableChoices.add(previewSizesFrontCamera.get(i));
                        }
                    }
                }
                List<SizeP> idealChoices = new ArrayList();
                for (int i = 0; i < usableChoices.size(); i++) {
                    if (Math.abs(usableChoices.get(i).getWidth() - idealSize.getWidth()) <= 100 && Math.abs(usableChoices.get(i).getHeight() - idealSize.getHeight()) <= 100) {
                        idealChoices.add(usableChoices.get(i));
                    }
                }

                if (idealChoices.size() != 0 && idealChoices.get(0).getWidth() >= idealChoices.get((idealChoices.size() - 1)).getWidth()) {
                    returnSize = idealChoices.get(0);
                }
                else{
                    returnSize = usableChoices.get((usableChoices.size()-1));
                }

                setPreviewWidth(returnSize.getWidth());
                setPreviewHeight(returnSize.getHeight());//mObserver.onPreviewSizeSet(returnSize);
                //Check
                //EventBus.getDefault().post(new CameraPreviewSizeEvent(returnSize));
                //bus.post( new CameraPreviewSizeEvent(returnSize));
            }

        } catch (Exception e) {
        }
    }

    public class SizeP
    {
        private int mWidth;
        private int mHeight;
        public SizeP(int width,int height)
        {
            mWidth = width;
            mHeight = height;
        }

        public int getWidth()
        {
            return mWidth;
        }

        public int getHeight()
        {
            return mHeight;
        }
    }

    public static class CameraDescriptor  {
        private int cameraId;
        private Camera camera;
        private ArrayList<SizeP> pictureSizes;
        private ArrayList<SizeP> previewSizes;
        private final int facing;

        private CameraDescriptor(int cameraId, Camera.CameraInfo info) {
            this.cameraId = cameraId;
            this.facing = info.facing;
        }

        public int getCameraId() {
            return (cameraId);
        }

        private void setCamera(Camera camera) {
            this.camera = camera;
        }

        private Camera getCamera() {
            return (camera);
        }

        public ArrayList<SizeP> getCameraPreviewSizes() {
            return (previewSizes);
        }

        private void setPreviewSizes(ArrayList<SizeP> sizes) {
            previewSizes = sizes;
        }


        public ArrayList<SizeP> getCameraPictureSizes() {
            return (pictureSizes);
        }

        public boolean isPictureFormatSupported(int format) {
            return (ImageFormat.JPEG == format);
        }

        private void setPictureSizes(ArrayList<SizeP> sizes) {
            pictureSizes = sizes;
        }

    }


}
