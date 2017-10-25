package wiki.hike.neo.hikecamera.gl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Neo on 25/10/17.
 */

public class FilterRecorder extends Filter {

    private FloatBuffer mVertexBuffer = null;
    protected float[] mVerticesForSampler2D = {
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f };
    protected int mVertexBufferObjectId;

    public FilterRecorder()
    {
        super(NO_FILTER_VERTEX_SHADER,NO_FILTER_FRAGMENT_SHADER);
        mRenderType = RENDER_TYPE_SAMPLER2D;
    }

    @Override
    public void onInit() {
        super.onInit();

        //Create texture buffer for filters.
        mVertexBuffer = ByteBuffer.allocateDirect(mVerticesForSampler2D.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(mVerticesForSampler2D).position(0);

        int[] vboIds = new int[1];
        GLES20.glGenBuffers(1, vboIds, 0);
        mVertexBufferObjectId = vboIds[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * FLOAT_SIZE_BYTES, mVertexBuffer, GLES20.GL_STATIC_DRAW);

        mVertexBuffer = null;

    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    public void onDraw(final int[] textureArr ,int elementBufferObjectId)
    {
        super.onDraw(textureArr,elementBufferObjectId);

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        for(int i=0;i<textureArr.length;i++) {
            GLES20.glActiveTexture(mTextureConstant[i]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureArr[i]);
            GLES20.glUniform1i(mSamplers[i], i);
        }

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,  mVertexBufferObjectId);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, true, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);

        onDrawArraysPre();
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, elementBufferObjectId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * SHORT_SIZE_BYTES, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glDisableVertexAttribArray(maPositionHandle);

        GLES20.glFinish();
        GLES20.glUseProgram(0);
    }

}
