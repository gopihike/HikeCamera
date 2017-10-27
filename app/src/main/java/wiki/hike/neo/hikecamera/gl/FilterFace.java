package wiki.hike.neo.hikecamera.gl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import ai.deepar.ar.AREventListener;
import ai.deepar.ar.DeepAR;
import wiki.hike.neo.hikecamera.ApplicationCamera;
import wiki.hike.neo.hikecamera.encoder.OpenGlUtils;
import wiki.hike.neo.hikecamera.utils.OESTexture;

/**
 * Created by Neo on 26/10/17.
 */

public class FilterFace extends Filter implements AREventListener {

    public final static String SLOT_MASKS = "masks";
    public final static String SLOT_EFFECTS = "effects";
    public final static String SLOT_FILTER = "filters";


    private String currentSlot = SLOT_MASKS;

    private final OESTexture mCameraTextureFaceFilter = new OESTexture();
    SurfaceTexture mSurfaceTexture = null;

    private DeepAR mDeepAR = null;
    private ByteBuffer[] mBuffersDeepAr;
    int mCurrentBuffer = 0;
    private boolean updateTexImage = false;

    private FloatBuffer mVertexBuffer = null;

    /*protected float[] mVerticesForSampler2D = {
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f };*/

    protected float[] mVerticesForSampler2D = {
            -1.0f, -1.0f, 0.0f, 0.125f, 1.0f,
            1.0f, -1.0f, 0.0f, 0.875f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.125f, 0.0f,
            1.0f, 1.0f, 0.0f, 0.875f, 0.0f };

    protected int mVertexBufferObjectId;


    public static final String NO_FILTER_VERTEX_SHADER= "" +
            //"uniform mat4 u_MVPMatrix;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            //    "gl_Position = u_MVPMatrix * a_position;\n"+
            "gl_Position =  a_position;\n" +
            "v_texcoord = a_texcoord;\n" +
            "}";


    public static final String SURFACETEXTURE_OES_FS = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform lowp samplerExternalOES texSampler;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(texSampler, v_texcoord);\n" +
            "    gl_FragColor = color;\n" +
            //"    gl_FragColor = vec4(1.0,0.0,0.0,0.0);\n"+

            "}";

    public FilterFace()
    {
        super(NO_FILTER_VERTEX_SHADER,SURFACETEXTURE_OES_FS);
        mRenderType = RENDER_TYPE_FACE_FILTER;
    }

    public void init()
    {
        onInit();

        onInitialized();
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

        Context cont = OpenGlUtils.getApplicationContext();
        mDeepAR = new DeepAR();
        mDeepAR.initialize(cont,this);

        this.mBuffersDeepAr = new ByteBuffer[2];
        for(int i = 0; i < 2; ++i) {
            this.mBuffersDeepAr[i] = ByteBuffer.allocateDirect(/*460800*/1382400);
            this.mBuffersDeepAr[i].order(ByteOrder.nativeOrder());
            this.mBuffersDeepAr[i].position(0);
        }

        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(new Runnable() {
                @Override
                public void run() {
                    mCameraTextureFaceFilter.init();
                    mSurfaceTexture = new SurfaceTexture(mCameraTextureFaceFilter.getTextureId()[0]);
                    mDeepAR.setRenderSurface(new Surface(mSurfaceTexture),512,512);
                    mDeepAR.setFrameRenderedCallback(new DeepAR.FrameRenderedCallback() {
                        @Override
                        public void frameRendered() {
                            updateTexImage = true;

                            //onProcessing done.
                            mObserver.onProcessingDone();
                        }
                    });

                }
            });
        }
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    public OESTexture getTexture()
    {
        return mCameraTextureFaceFilter;
    }

    public void onPreviewFrame(final byte[] bytes, final Camera camera,int orientation)
    {
        if(isInitialized() == false)
            return;
        super.onPreviewFrame(bytes,camera,orientation);
        mBuffersDeepAr[mCurrentBuffer].put(bytes);
        mBuffersDeepAr[mCurrentBuffer].position(0);
        mDeepAR.receiveFrame(mBuffersDeepAr[mCurrentBuffer],orientation); //Use parameters instead of singleton and static.
        mCurrentBuffer = (mCurrentBuffer +1) %2;
    }

    public void onDraw( int vertexBufferObjectId, int elementBufferObjectId)
    {
        if (updateTexImage) {
            updateTexImage = false;
            synchronized (this) {
                mSurfaceTexture.updateTexImage();
            }
        }

        super.onDraw(vertexBufferObjectId,elementBufferObjectId);
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();

        if (!isInitialized()) {
            return;
        }

        for (int i = 0; i < mCameraTextureFaceFilter.getTextureId().length; i++) {
            GLES20.glActiveTexture(mTextureConstant[i]);
            int textureType = GLES20.GL_TEXTURE_2D;
            switch (getRenderType()) {
                case RENDER_TYPE_SURFACE_TEXTURE:
                case RENDER_TYPE_FACE_FILTER:
                    textureType = GL_TEXTURE_EXTERNAL_OES;
                    break;
                case RENDER_TYPE_SAMPLER2D:
                case RENDER_TYPE_PREVIEW_BUFFER:
                    textureType = GLES20.GL_TEXTURE_2D;
                    break;
            }
            GLES20.glBindTexture(textureType, mCameraTextureFaceFilter.getTextureId()[i]);
            GLES20.glUniform1i(mSamplers[i], i);
        }

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
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

    public void setFaceFilterType(final String slot,final String path)
    {
       synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    @Override
    public void screenshotTaken(Bitmap bitmap) {

    }

    @Override
    public void videoRecordingStarted() {

    }

    @Override
    public void videoRecordingFinished() {

    }

    @Override
    public void videoRecordingFailed() {

    }

    @Override
    public void videoRecordingPrepared() {

    }

    @Override
    public void initialized() {

        mDeepAR.switchEffect(FilterFace.SLOT_MASKS,"file:///android_asset/aviators");

    }

    @Override
    public void faceVisibilityChanged(boolean b) {

    }

}
