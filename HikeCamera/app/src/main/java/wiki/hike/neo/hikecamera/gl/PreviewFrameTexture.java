package wiki.hike.neo.hikecamera.gl;

import android.opengl.GLES20;

/**
 * Created by Neo on 10/10/17.
 */

public class PreviewFrameTexture {

    int [] mPreviewTexture = {0,0};

    int [] getTextureHandle()
    {
        return mPreviewTexture;
    }

    int getYTextureHandle()
    {
       return mPreviewTexture[0];
    }

    int getUVTextureHandle()
    {
        return mPreviewTexture[1];
    }

    public void initWithPreview(int previewTextureWidth,int previewTextureHeight)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewTexture[0]);

        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                previewTextureWidth,
                previewTextureHeight,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                null
        );

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewTexture[1]);

        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE_ALPHA,
                previewTextureWidth / 2,
                previewTextureHeight / 2,
                0,
                GLES20.GL_LUMINANCE_ALPHA,
                GLES20.GL_UNSIGNED_BYTE,
                null
        );

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
    }

    public void init()
    {
        //  Initialize Y channel texture and UV channel texture
        GLES20.glGenTextures(2, mPreviewTexture, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewTexture[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewTexture[1]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
    }

}

