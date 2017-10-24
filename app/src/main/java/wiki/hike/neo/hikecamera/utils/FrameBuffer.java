package wiki.hike.neo.hikecamera.utils;

import android.opengl.GLES20;

/**
 * Created by souravsharma on 21/10/17.
 */

public class FrameBuffer {

    public static int TEXTURE_2D_FBO = 1;
    public static int TEXTURE_2D_FBO_DEPTH = 2;

    private int mWidth = 0;
    private int mHeight = 0;
    private int mImageType = 0;
    private int[] mTexture = new int[1];
    private int[] mFrameBuffer = new int[1];
    private int[] mDepthBuffer = new int[1];


    public FrameBuffer(int image_type, int width, int height){

        mWidth = width;
        mHeight = height;

        mImageType = image_type;

        mTexture[0] = GLES20.GL_NONE;
        mFrameBuffer[0] = GLES20.GL_NONE;
        mDepthBuffer[0] = GLES20.GL_NONE;

        GLES20.glGenTextures(1, mTexture, 0);
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexture[0], 0);

        if (mImageType == TEXTURE_2D_FBO_DEPTH) {
            GLES20.glGenRenderbuffers(1, mDepthBuffer, 0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer[0]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBuffer[0]);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        int error = GLES20.glGetError();
    }

    public void releaseFrameBuffer(){

            if (mTexture[0] != GLES20.GL_NONE) {
                GLES20.glDeleteTextures(1, mTexture, 0);
            }
            mTexture[0] = GLES20.GL_NONE;

            if (mFrameBuffer[0] != GLES20.GL_NONE) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GLES20.glDeleteFramebuffers(1, mFrameBuffer,0);
            }
            mFrameBuffer[0] = GLES20.GL_NONE;

            if (mDepthBuffer[0] != GLES20.GL_NONE) {
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
                GLES20.glDeleteRenderbuffers(1, mDepthBuffer,0);
            }
            mDepthBuffer[0] = GLES20.GL_NONE;
    }

    public void bindTexture(){

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
    }

    public void bindFrameBuffer(){

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
    }


    public void unbindTexture(){

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void unbindFrameBuffer(){

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int getTextureID(){

        return mTexture[0];
    }

    public int[] getTextureIDArr()
    {
        return mTexture;
    }
}
