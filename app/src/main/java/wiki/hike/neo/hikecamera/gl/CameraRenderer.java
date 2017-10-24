package wiki.hike.neo.hikecamera.gl;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import wiki.hike.neo.hikecamera.encoder.MediaVideoEncoder;
import wiki.hike.neo.hikecamera.utils.BitmapUtils;
import wiki.hike.neo.hikecamera.utils.FrameBuffer;
import wiki.hike.neo.hikecamera.utils.OESTexture;

public class CameraRenderer implements
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {
	private Context mContext;
	private CameraManager mManager;
	private SurfaceTexture mSurfaceTexture;
	GLSurfaceView mSurfaceView;
	private Filter mFilter = null;

	//Video.
	private boolean mIsRecordingStarted = false;
	private Filter mFilterFrameBuffer = null; //This is called only when.
	private MediaVideoEncoder mVideoEncoder;

	private Queue<Runnable> mRunOnDraw = null;
	private Queue<Runnable> mRunOnDrawEnd = null;
	//Renderer observer
	private Observer mObserver;

	//private final FBORenderTarget mRenderTarget = new FBORenderTarget();
	private final PreviewFrameTexture  mPreviewFrameTexture = new PreviewFrameTexture();
	private final OESTexture mCameraTexture = new OESTexture();
	private int mSurfaceWidth, mSurfaceHeight;
	private int mPreviewWidth,mPreviewHeight;


	ByteBuffer mBufferY;
	ByteBuffer mBufferUV;

	protected float[] mVerticesFrontCamera = {
			-1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f };

	protected float[] mVerticesBackCamera = {
			-1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

	//Video. Remove this code and make it a part of video filter.
	protected float[] mPositionCoords =
	{
			-1.0f,-1.0f,
			1.0f,-1.0f,
			-1.0f,1.0f,
			1.0f,1.0f,
	};

	protected float[] mTextureCoordsFront =
	{
			0.0f,1.0f,
			0.0f,0.0f,
			1.0f,1.0f,
			1.0f,0.0f,
	};

	protected float[] mTextureCoordsBack =
	{
			1.0f,1.0f,
			1.0f,0.0f,
			0.0f,1.0f,
			0.0f,0.0f,
	};

	protected FloatBuffer mGLCubeBuffer = null;

	protected FloatBuffer mGLTextureBuffer = null;
	//**********************************

	protected short[] mIndices = {0, 1, 2, 1, 2, 3};

	protected int mVertexBufferObjectId;
	private int mCameraSource;

	protected int mElementBufferObjectId;

	private FloatBuffer mVertexBuffer = null;
	private FloatBuffer mVertexBufferFront = null;
	private FloatBuffer mVertextBufferBack = null;

	private ShortBuffer mIndexBuffer = null;

	protected final int SHORT_SIZE_BYTES = 2;
	protected final int FLOAT_SIZE_BYTES = 4;

	private boolean updateTexture = false;

	//Video . Remove this ugly call once video recording is done.
	FrameBuffer mFrameBuffer = null;

	public CameraRenderer(Context context,int previewWidth,int previewHeight) {
		//super(context);
		mContext = context;
		mRunOnDraw = new LinkedList<>();
		mRunOnDrawEnd  = new LinkedList<>();

		mPreviewWidth = previewWidth;
		mPreviewHeight = previewHeight;

		mGLCubeBuffer = ByteBuffer.allocateDirect(mPositionCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mGLCubeBuffer.put(mPositionCoords).position(0);

		//Video
		//Frame buffer created.
		//Set default filer
	}

	public void onResume()
	{
		//setFilter again safe check.
	}

	//Video. Kindly beautify this piece of code.VideoEncoder should act as filter.
	public void setVideoEncoder(MediaVideoEncoder videoEncoder)
	{
		this.mVideoEncoder = videoEncoder;
		if(videoEncoder == null) {
			mIsRecordingStarted = false;
			return;
		}
		//This should be pushed in start queueOneTime
		//Create frame buffer here in Gl thread.
		//Frame buffer creation might be a costly operation.Take a call on if we want to create frame buffer every time when a recording event happens or just create once.

		mSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				if(mFilterFrameBuffer== null) {
					mFilterFrameBuffer = new Filter();
					mFilterFrameBuffer.init();
				}

				if(mFrameBuffer == null)
					mFrameBuffer = new FrameBuffer(FrameBuffer.TEXTURE_2D_FBO,mSurfaceWidth,mSurfaceHeight);

				if (mVideoEncoder != null) {
					mVideoEncoder.setEglContext(EGL14.eglGetCurrentContext(),mFrameBuffer.getTextureID());
				}
				mIsRecordingStarted = true;
			}
		});
	}

	//Called after startpreview has started.
	public void initPreviewFrameRenderer(/*int previewWidth,int previewHeight*/)
	{
		//mPreviewWidth = previewWidth;
		//mPreviewHeight = previewHeight;

		runOnDraw(new Runnable() {
			@Override
			public void run() {
				mPreviewFrameTexture.initWithPreview(mPreviewWidth,mPreviewHeight);
			}
		});

		//  Y channel. Used for facial tracking
		mBufferY = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight);
		//  UV channel. Used for color rendering
		mBufferUV = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight / 2);

		//mPreviewFrameTexture.init(mPreviewWidth,mPreviewHeight);
		//Position of this call will be changed once camera initPreview size is placed at the right location.
		//Once preview size is set
		mManager.getCamera().setPreviewCallbackWithBuffer(this);

			for (int i = 0; i < 3; i++) {
			mManager.getCamera().addCallbackBuffer(new byte[mPreviewWidth *mPreviewHeight * 3 / 2]);
		}

	}

	public void setSurfaceView(GLSurfaceView surfaceView) {
		mSurfaceView = surfaceView;
	}

	public void setCameraManager(CameraManager camManager) {
		mManager = camManager;
	}

	@Override
	public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
		updateTexture = true;
		mSurfaceView.requestRender();
	}

	byte[] prevBytes;
	@Override
	public void onPreviewFrame(final byte[] bytes,final Camera camera) {
		mSurfaceView.queueEvent(new Runnable() {
			public void run()
			{
				if (prevBytes == null || prevBytes.length != bytes.length) {
					prevBytes = new byte[bytes.length];
				}
				System.arraycopy(bytes, 0, prevBytes, 0, bytes.length);
				processVideoFrame(prevBytes);
				camera.addCallbackBuffer(bytes);
			}
		});
	}

	public int getSurfaceWidth(){

	    return mSurfaceWidth;
    }

    public int getSurfaceHeight(){

        return mSurfaceHeight;
    }

	private void processVideoFrame(byte[] videoFrame)
	{
		mBufferY.position(0);
		mBufferY.put(videoFrame, 0, mPreviewWidth * mPreviewHeight);
		mBufferUV.position(0);
		mBufferUV.put(videoFrame,
				mPreviewWidth * mPreviewHeight,
				mPreviewWidth * mPreviewHeight / 2);

		mBufferY.position(0);
		mBufferUV.position(0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getYTextureHandle());
		GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
				0,
				0,
				0,
				mPreviewWidth,
				mPreviewHeight,
				GLES20.GL_LUMINANCE,
				GLES20.GL_UNSIGNED_BYTE,
				mBufferY
		);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getUVTextureHandle());
		GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
				0,
				0,
				0,
				mPreviewWidth / 2,
				mPreviewHeight / 2,
				GLES20.GL_LUMINANCE_ALPHA,
				GLES20.GL_UNSIGNED_BYTE,
				mBufferUV
		);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
		Log.e("ENGINE","REQUEST RENDER CALLED");

		mSurfaceView.requestRender();
	}

	@Override
	public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Code to create vertext and element buffer : Bascially  quad that is used to render objects.

		//Decision to be taken whether we should create front and back buffer separately or should create only one.
		//Flip time to be measured with both
		flip(mManager.getCameraFacing());

		//generate camera texture------------------------
		mCameraTexture.init();

		mPreviewFrameTexture.init();

		//set up surfacetexture------------------
		SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
		mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId()[0]);
		if (oldSurfaceTexture != null) {
			oldSurfaceTexture.release();
		}

		mSurfaceTexture.setOnFrameAvailableListener(this);

		if (mObserver != null) {
			mObserver.onSurfaceTextureCreated(mSurfaceTexture);
		}
	}

	public void flipWithCallBackReset(final int cameraFacing)
	{
		setCallBackBasedOnFilter();
		flip(cameraFacing);
	}

	float[] modifyTextureBuffers(int cameraFacing)
	{
		//Algorithm used : Explaination:
		/*
			https://stackoverflow.com/questions/6565703/math-algorithm-fit-image-to-screen-retain-aspect-ratio
			So for example:

			20
			|------------------|
			10
			|---------|

			--------------------     ---   ---
			|         |        |      | 7   |
			|         |        |      |     | 10
			|----------        |     ---    |
			|                  |            |
		--------------------           ---

				ws = 20
				hs = 10
				wi = 10
				hi = 7
				20/10 > 10/7 ==> (wi * hs/hi, hs) = (10 * 10/7, 10) = (100/7, 10) ~ (14.3, 10)

		*/
		//float surfaceAspectRatio = (1.0f*mSurfaceHeight)/mSurfaceWidth;
		//float previewAspectRatio = (1.0f*mPreviewWidth)/mPreviewHeight; //Camera preview width and height are swapped.
		float surfaceW = mSurfaceHeight;
		float surfaceH = mSurfaceWidth;

		float previewW = mPreviewWidth;
		float previewH = mPreviewHeight ;

		float surfaceAspectRatio = surfaceW/surfaceH;
		float previewAspectRatio = previewW/previewH;

		float[] modifiedBuffer;
		if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			modifiedBuffer = mVerticesFrontCamera.clone();
		} else {
			modifiedBuffer = mVerticesBackCamera.clone();
		}
		if (surfaceAspectRatio > previewAspectRatio)
		{
			float newWidth;
			float newHeight;
			newHeight = previewH * surfaceW / previewW;
			//Coordinates for surface width and height.
			float yFactor = (newHeight - surfaceH) / 2;
			//float xFactor = (newWidth - surfaceW)/2;
			//float newAspectRatio = newWidth/newHeight;
			float offset = yFactor/newHeight;
			modifiedBuffer[4] = modifiedBuffer[4] - offset;
			modifiedBuffer[9] = modifiedBuffer[9] + offset;
			modifiedBuffer[14] = modifiedBuffer[14] - offset;
			modifiedBuffer[19] = modifiedBuffer[19] + offset;
		}
		return modifiedBuffer;
	}

	float[] modifyPositionBuffers(int cameraFacing){

		float surfaceAspectRatio = (1.0f*mSurfaceHeight)/mSurfaceWidth;
		float previewAspectRatio = (1.0f*mPreviewWidth)/mPreviewHeight;

		float[] modifiedBuffer = mVerticesBackCamera.clone();

        if(cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            modifiedBuffer = mVerticesFrontCamera.clone();
        }

        if(surfaceAspectRatio > previewAspectRatio){

			float offset = (surfaceAspectRatio - previewAspectRatio)/2.0f;

            if(cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

                modifiedBuffer[1] = modifiedBuffer[1] + offset;
                modifiedBuffer[6] = modifiedBuffer[6] + offset;
                modifiedBuffer[11] = modifiedBuffer[11] - offset;
                modifiedBuffer[16] = modifiedBuffer[16] - offset;
            }
            else{

                modifiedBuffer[1] = modifiedBuffer[1] + offset;
                modifiedBuffer[6] = modifiedBuffer[6] + offset;
                modifiedBuffer[11] = modifiedBuffer[11] - offset;
                modifiedBuffer[16] = modifiedBuffer[16] - offset;
            }
		}

		return modifiedBuffer;
	}

	public void flip(final int cameraFacing)
	{
		//Optimize this call dont do buffer creations here.
		//Just buffer ids here to make it performant but on doing in this we have cost of extra memory at native side.
		//Take a call on this or use profiler.
		runOnDraw(new Runnable() {
			@Override
			public void run() {

				if(cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
				{
                    mVertexBuffer = ByteBuffer.allocateDirect(mVerticesFrontCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
                    float[] modifiedBuffer = modifyTextureBuffers(cameraFacing);//modifyTextureBuffers(cameraFacing);//
                    mVertexBuffer.put(modifiedBuffer).position(0);

					//Video
					mGLTextureBuffer = ByteBuffer.allocateDirect(mTextureCoordsFront.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					mGLTextureBuffer.put(mTextureCoordsFront).position(0);
				}
				else
				{
					mVertexBuffer = ByteBuffer.allocateDirect(mVerticesBackCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
                    float[] modifiedBuffer = modifyTextureBuffers(cameraFacing);;
				    mVertexBuffer.put(modifiedBuffer).position(0);

					//Video
					mGLTextureBuffer = ByteBuffer.allocateDirect(mTextureCoordsBack.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					mGLTextureBuffer.put(mTextureCoordsBack).position(0);
				}

				mVertexBufferFront = ByteBuffer.allocateDirect(mVerticesFrontCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
				mVertexBufferFront.put(mVerticesFrontCamera).position(0);
				mVertextBufferBack = ByteBuffer.allocateDirect(mVerticesBackCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
				mVertextBufferBack.put(mVerticesBackCamera).position(0);

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
			}
		});
	}


	@Override
	public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {

	    Log.v("Sourav", "Surface width : " + width + ", Surface height : " + height);
		mSurfaceWidth = width;
		mSurfaceHeight = height;

		//Video
		//mFrameBuffer = new FrameBuffer(FrameBuffer.TEXTURE_2D_FBO,mSurfaceWidth,mSurfaceHeight);

		GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		runAll(mRunOnDraw); //Using this queue to process anything that comes from thread withoutGL context.

		//Put this ugly call in start queue.Permanence is valid till start and stop video recording.
		//Video encoder should act as filter.Use of boolean is not at all allowed.
		if (mVideoEncoder != null && mIsRecordingStarted) {
				mFrameBuffer.bindFrameBuffer();
		}

		switch (mFilter.getRenderType())
		{
			case Filter.RENDER_TYPE_PREVIEW_BUFFER:

				Log.e("ENGINE","DRAW CALLED");
				mFilter.onDraw(mPreviewFrameTexture.getTextureHandle(),mVertexBufferObjectId,mElementBufferObjectId);
				//mFilter.onDraw(mPreviewFrameTexture.getYTextureHandle(),mPreviewFrameTexture.getUVTextureHandle());
				break;
			case Filter.RENDER_TYPE_SURFACE_TEXTURE:
				if(updateTexture){
					mSurfaceTexture.updateTexImage();
					//mSurfaceTexture.getTransformMatrix(mFilter.getTransformMatrix());
					updateTexture = false;
					mFilter.onDraw(mCameraTexture.getTextureId(),mVertexBufferObjectId,mElementBufferObjectId);
				}
				break;
		}

		if(mVideoEncoder != null && mIsRecordingStarted)
		{
			mVideoEncoder.frameAvailableSoon(mGLCubeBuffer, mGLTextureBuffer);
			//VIDEO: Put this call inside end queue that process till start and end recording.
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			//Create a filter of type  Sampler2D.
			mFilterFrameBuffer.onDraw(mFrameBuffer.getTextureIDArr(),mVertexBufferObjectId,mElementBufferObjectId);
		}

		runAll(mRunOnDrawEnd);
	}
	
	public void onDestroy(){
		updateTexture = false;
		mSurfaceTexture.release();
	}


	public void setCallBackBasedOnFilter()
	{
		switch (mFilter.getRenderType())
		{
			case Filter.RENDER_TYPE_PREVIEW_BUFFER:
				mSurfaceTexture.setOnFrameAvailableListener(null);
				mManager.getCamera().setPreviewCallbackWithBuffer(this);
				for (int i = 0; i < 3; i++) {
					mManager.getCamera().addCallbackBuffer(new byte[mPreviewWidth *mPreviewHeight * 3 / 2]);
				}
				break;
			case Filter.RENDER_TYPE_SURFACE_TEXTURE:
				mManager.getCamera().setPreviewCallbackWithBuffer(null);
				mSurfaceTexture.setOnFrameAvailableListener(this);
				break;
		}
	}

	public void setFilter(final Filter filter) {
		runOnDraw(new Runnable() {

			@Override
			public void run() {
				final Filter oldFilter = mFilter;
				mFilter = filter;
				if (oldFilter != null) {
					oldFilter.destroy();
				}
				setCallBackBasedOnFilter();
				mFilter.init();
			}
		});
	}

	private void runAll(Queue<Runnable> queue) {
		synchronized (queue) {
			while (!queue.isEmpty()) {
				queue.poll().run();
			}
		}
	}

	protected void runOnDraw(final Runnable runnable) {
		synchronized (mRunOnDraw) {
			mRunOnDraw.add(runnable);
		}
	}

	protected void runOnDrawEnd(final Runnable runnable) {
		synchronized (mRunOnDrawEnd) {
			mRunOnDrawEnd.add(runnable);
		}
	}


	public void setObserver(Observer observer) {
		mObserver = observer;
	}

	public interface Observer {
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
	}

	public static byte[] bitmapToBytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
		if (bitmap == null) {
			byte[] b = new byte[]{0};
			return b;
		}
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bitmap.compress(format, quality, bao);
		return bao.toByteArray();
	}
	static int count = 100;

	public static void saveBitMapDebug(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int quality)throws IOException {
		{
			FileOutputStream fos = null;
			try {
				File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + "test"+ count +".jpg");
				fos = new FileOutputStream(f);
				++count;

				byte[] b = bitmapToBytes(bitmap, compressFormat, quality);
				if (b == null) {
					throw new IOException();
				}
				fos.write(b);
				fos.flush();
				fos.getFD().sync();
			} finally {
				if (fos != null)
					fos.close();
			}
		}
	}


    public void takePicture(final Filter.TakePictureListener listener) {

        runOnDrawEnd(new Runnable() {
            @Override
            public void run() {
				Bitmap snapshot = BitmapUtils.getBitmap(mSurfaceWidth, mSurfaceHeight);
				try {
				    listener.onPictureTaken(snapshot);
                } catch (FileNotFoundException e) {
					e.printStackTrace();
				}

            }
        });
    }

}