package wiki.hike.neo.hikecamera.utils;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.IntBuffer;

import static javax.microedition.khronos.opengles.GL10.GL_RGBA;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

/**
 * Created by souravsharma on 21/10/17.
 */

public class BitmapUtils {


    public static Bitmap getBitmap(int surfaceWidth, int surfaceHeight)
    {

        int mWidth = (int)(surfaceWidth);
        int mHeight = (int)(surfaceHeight);
        int[] iat = new int[mWidth * mHeight];
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GL_RGBA, GL_UNSIGNED_BYTE, ib);
        int[] ia = ib.array();
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                iat[(mHeight - i - 1) * mWidth + j] = ia[i * mWidth + j];
            }
        }
        Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));

        return mBitmap;
    }
}
