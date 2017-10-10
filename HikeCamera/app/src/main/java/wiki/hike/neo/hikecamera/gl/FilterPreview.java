package wiki.hike.neo.hikecamera.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Neo on 10/10/17.
 */

public class FilterPreview {
    public static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    public static final int RENDER_TYPE_SURFACE_TEXTURE = 0;
    public static final int RENDER_TYPE_PREVIEW_BUFFER = 1;
    private int mRenderType = RENDER_TYPE_PREVIEW_BUFFER;

    public static final String YUV_VS= "" +
            //"uniform mat4 u_MVPMatrix;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            //    "gl_Position = u_MVPMatrix * a_position;\n"+
            "gl_Position =  a_position;\n" +
            "v_texcoord = a_texcoord;\n" +
            "}";

    public static final String YUV_FS = "" +
            "precision highp float;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "uniform sampler2D luminanceTexture;" +
            "uniform sampler2D chrominanceTexture;" +
            "void main() {\n" +
            "   lowp float y = texture2D(luminanceTexture, v_texcoord).r;" +
            "   lowp vec4 uv = texture2D(chrominanceTexture, v_texcoord);" +
            "   mediump vec4 rgba = y * vec4(1.0, 1.0, 1.0, 1.0) + " +
            "                  (uv.a - 0.5) * vec4(0.0, -0.337633, 1.732446, 0.0) + " +
            "                  (uv.r - 0.5) * vec4(1.370705, -0.698001, 0.0, 0.0); " +
            "	gl_FragColor = rgba;" +
            "}";

    //  Shader drawing YUV video frames on the surface


    protected int mGLProgId;

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
    private int mCameraSource;

    protected int mElementBufferObjectId;

    private FloatBuffer mVertexBuffer = null;

    private ShortBuffer mIndexBuffer = null;

    protected final int SHORT_SIZE_BYTES = 2;
    protected final int FLOAT_SIZE_BYTES = 4;


    private boolean misSurfaceTexture;
    private boolean misOes;
    protected final static String A_POSITION = "a_position";
    protected final static String A_TEXCOORD = "a_texcoord";
    protected final static String U_MVPMATRIX = "u_MVPMatrix";
    protected final static String U_SAMPLER0 = "texSampler";
    protected final static String U_LUMINANCE_SAMPLER = "luminanceTexture";
    protected final static String U_CHROMINANCE_SAMPLER = "chrominanceTexture";


    protected int maPositionHandle;
    protected int maTextureHandle;
    protected int muMVPMatrixHandle;
    protected int muSampler0Handle;
    protected int mLumninanceSampler;
    protected int mChrominanceSampler;


    private float[] mProjMatrix = new float[16];

    private String mVertexShader;
    private String mFragmentShader;

    int mPreviewWidth;
    int mPreviewHeight;
    int mOrientation;
    boolean mIsFrontFacing;

    private boolean mIsInitialized;

    boolean isFilterInitialised()
    {
        return mIsInitialized;
    }

    public FilterPreview() {

        mIsInitialized = false;
        //this(SURFACETEXTURE_VERTEX_SHADER, SURFACETEXTURE_FRAGEMENT_SHADER_OES);
    }

    public FilterPreview(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public int getRenderType(){
        return mRenderType;
    }

    public final void init()
    {
        mVertexShader = YUV_VS;
        mFragmentShader = YUV_FS;
        onInit();
        onInitialized();
    }

    public void onInit()
    {
        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

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
        //muMVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgId, U_MVPMATRIX);
        //muSampler0Handle = GLES20.glGetUniformLocation(mGLProgId, U_SAMPLER0);
        mLumninanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_LUMINANCE_SAMPLER);
        mChrominanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_CHROMINANCE_SAMPLER);

        mIsInitialized = true;

    }

    public void onInitialized() {


    }

    public void onDraw()
    {

    }

    public void onDraw(final int textureIdY,final int textureIdUV)
    {
        GLES20.glUseProgram(mGLProgId);
        if (!mIsInitialized) {
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdY);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdUV);

        GLES20.glUniform1i(mLumninanceSampler, 0);
        GLES20.glUniform1i(mChrominanceSampler, 1);
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(FilterGPU.GL_TEXTURE_EXTERNAL_OES, textureIdY);
        //GLES20.glUniform1i(muSampler0Handle, 0);
        //GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mProjMatrix, 0);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, true, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);

        onDrawArraysPre();
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * SHORT_SIZE_BYTES, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glDisableVertexAttribArray(maPositionHandle);

        GLES20.glFinish();
        GLES20.glUseProgram(0);


    }

    protected void onDrawArraysPre() {
    }

    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    public void onDestroy() {
    }



}
