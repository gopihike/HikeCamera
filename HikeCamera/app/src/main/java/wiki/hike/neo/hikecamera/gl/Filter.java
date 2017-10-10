package wiki.hike.neo.hikecamera.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import wiki.hike.neo.hikecamera.R;

/**
 * Created by Neo on 09/10/17.
 */
//This is the Baap of all the filters

public class Filter {

    public static final int RENDER_TYPE_SURFACE_TEXTURE = 0;
    public static final int RENDER_TYPE_PREVIEW_BUFFER = 1;

    private int mRenderType = RENDER_TYPE_SURFACE_TEXTURE;


    protected float[] mVerticesFrontCamera = {
            -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 0.0f, 1.0f};

    protected float[] mVerticesBackCamera = {
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f};


    protected short[] mIndices = {0, 1, 2, 1, 2, 3};

    protected int mVertexBufferObjectId;

    protected int mElementBufferObjectId;

    private FloatBuffer mVertexBuffer = null;

    private ShortBuffer mIndexBuffer = null;

    protected final int SHORT_SIZE_BYTES = 2;
    protected final int FLOAT_SIZE_BYTES = 4;



    protected final static String A_POSITION = "a_position";
    protected final static String A_TEXCOORD = "a_texcoord";
    protected final static String U_MVPMATRIX = "u_MVPMatrix";
    protected final static String U_SAMPLER0 = "texSampler";

    protected int maPositionHandle;
    protected int maTextureHandle;
    protected int muMVPMatrixHandle;
    protected int muSampler0Handle;
    private float[] mProjMatrix = new float[16];

    private int  mCameraSource = 0;
    protected int mGLProgId;

    private String mVertexShader;
    private String mFragmentShader;

    void onInit()
    {

        if(mCameraSource == 0){
            mVertexBuffer = ByteBuffer.allocateDirect(mVerticesFrontCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertexBuffer.put(mVerticesFrontCamera).position(0);
        }else {
            mVertexBuffer = ByteBuffer.allocateDirect(mVerticesBackCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertexBuffer.put(mVerticesBackCamera).position(0);
        }

        mIndexBuffer = ByteBuffer.allocateDirect(mIndices.length * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndexBuffer.put(mIndices).position(0);

        int[] vboIds = new int[2];
        GLES20.glGenBuffers(2, vboIds, 0);
        mVertexBufferObjectId = vboIds[0];
        mElementBufferObjectId = vboIds[1];


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * FLOAT_SIZE_BYTES, mVertexBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer.capacity() * SHORT_SIZE_BYTES, mIndexBuffer, GLES20.GL_STATIC_DRAW);

        mIndexBuffer = null;
        mVertexBuffer = null;

        Matrix.orthoM(mProjMatrix, 0, -1, 1, 1, -1, 1, -1);
        maPositionHandle = GLES20.glGetAttribLocation(mGLProgId, A_POSITION);
        maTextureHandle = GLES20.glGetAttribLocation(mGLProgId, A_TEXCOORD);
    }

}
