package wiki.hike.neo.hikecamera.gl;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;
import java.util.List;

/**
 * Created by Neo on 09/10/17.
 */

//Encapsulate android camera here.

public class CameraManager {
    private Camera mCamera;
    int mCameraWidth;
    int mCameraHeight;

    //Code to be changed for getting best preview size.
    void initPreviewSize(int width,int height)
    {
        //set camera para-----------------------------------
        mCameraWidth =0;
        mCameraHeight =0;

        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if(psize.size() > 0 ){
            int i;
            for (i = 0; i < psize.size(); i++){
                if(psize.get(i).width < width || psize.get(i).height < height)
                    break;
            }
            if(i>0)
                i--;
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);

            mCameraWidth = psize.get(i).width;
            mCameraHeight= psize.get(i).height;
        }
        mCamera.setParameters(param);
    }

    public int getWidth()
    {
        return mCameraWidth;
    }

    public int getHeight()
    {
        return mCameraHeight;
    }

    public void open()
    {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    public Camera getCamera()
    {
        return mCamera;
    }

    void setSurfaceTexture(SurfaceTexture texture,int width,int height)
    {
        try{
            if(mCamera != null){
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            try{
                mCamera.setPreviewTexture(texture);
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
            initPreviewSize(width,height);
            mCamera.startPreview();

        }catch(final Exception ex){

        }
    }


}
