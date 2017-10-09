package wiki.hike.neo.hikecamera.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;

import wiki.hike.neo.hikecamera.R;

/**
 * Created by Neo on 09/10/17.
 */
public class Filter {

    public static final int RENDER_TYPE_SURFACE_TEXTURE = 0;
    public static final int RENDER_TYPE_PREVIEW_BUFFER = 1;


    public static final int FILTER_ENTER = 0;
    public static final int FILTER_UPDATE = 1;
    public static final int FILTER_END = 3;


    private int mRenderType = RENDER_TYPE_SURFACE_TEXTURE;
    private final Shader mOffscreenShader = new Shader();

    //Variables used by vertex shader.
    private ByteBuffer mFullQuadVertices;
    private float[] mTransformM = new float[16];
    private float[] mOrientationM = new float[16];
    private float[] mRatio = new float[2];

    int mFilterState = Filter.FILTER_END;

    int mCameraWidth;
    int mCamereHeight;

    int mSurfaceWidth;
    int mSurfaceHeight;

    public Filter()
    {

    }

    public Filter(int camWidth,int camHeight,int surfaceWidth,int surfaceHeight)
    {
        mCameraWidth = camWidth;
        mCamereHeight = camHeight;

        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;

    }

    public void initShaderAndQuad(Context context)
    {
        try {
            mOffscreenShader.setProgram(R.raw.vshader, R.raw.fshader, context);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
        mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
        mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);
    }

    void initFilterWidthAndHeight(int camWidth,int camHeight,int surfaceWidth,int surfaceHeight)
    {
        mCameraWidth = camWidth;
        mCamereHeight = camHeight;

        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;

        if(/*mContext.getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_PORTRAIT*/true){
            Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
            mRatio[1] = mCameraWidth*1.0f/mSurfaceHeight;
            mRatio[0] = mCamereHeight*1.0f/mSurfaceWidth;
        }
        else{
            Matrix.setRotateM(mOrientationM, 0, 0.0f, 0f, 0f, 1f);
            mRatio[1] = mCamereHeight*1.0f/mSurfaceHeight;
            mRatio[0] = mCameraWidth*1.0f/mSurfaceWidth;
        }

    }

    public void setFilterState(int state)
    {
        mFilterState = state;
    }

    public int getmFilterState()
    {
        return mFilterState;
    }

    public int getRenderType(){
        return mRenderType;
    }

    public float [] getTransformMatrix()
    {
        return mTransformM;
    }

    void onDraw(int mtextureId)
    {
        switch(getmFilterState())
        {
            case Filter.FILTER_ENTER:
                if(/*mContext.getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_PORTRAIT*/true){
                    Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
                    mRatio[1] = mCameraWidth*1.0f/mSurfaceHeight;
                    mRatio[0] = mCamereHeight*1.0f/mSurfaceWidth;
                }
                else{
                    Matrix.setRotateM(mOrientationM, 0, 0.0f, 0f, 0f, 1f);
                    mRatio[1] = mCamereHeight*1.0f/mSurfaceHeight;
                    mRatio[0] = mCameraWidth*1.0f/mSurfaceWidth;
                }

                final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
                mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
                mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);

                setFilterState(Filter.FILTER_UPDATE);
                break;
            case Filter.FILTER_UPDATE:

                mOffscreenShader.useProgram();

                int uTransformM = mOffscreenShader.getHandle("uTransformM");
                int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
                int uRatioV = mOffscreenShader.getHandle("ratios");
                int aPosition = mOffscreenShader.getHandle("aPosition");

                GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
                GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
                GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mtextureId);

                GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
                GLES20.glEnableVertexAttribArray(aPosition);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                break;
        }





    }
}
