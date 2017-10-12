package wiki.hike.neo.hikecamera.gl;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class CameraRenderer implements
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {
	private Context mContext;
	private CameraManager mManager;
	private SurfaceTexture mSurfaceTexture;
	GLSurfaceView mSurfaceView;
	private Filter mFilter = null;

	private Queue<Runnable> mRunOnDraw = null;
	private Queue<Runnable> mRunOnDrawEnd = null;
	//Renderer observer
	private Observer mObserver;

	//private final FBORenderTarget mRenderTarget = new FBORenderTarget();
	private final PreviewFrameTexture  mPreviewFrameTexture = new PreviewFrameTexture();
	private final OESTexture mCameraTexture = new OESTexture();
	private int mWidth, mHeight;
	private int mPreviewWidth,mPreviewHeight;


	ByteBuffer mBufferY;
	ByteBuffer mBufferUV;

	protected float[] mVerticesFrontCamera = {
			-1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f};

	protected float[] mVerticesBackCamera = {
			-1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 0.0f};


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

	public CameraRenderer(Context context) {
		//super(context);
		mContext = context;
		mRunOnDraw = new LinkedList<>();
		mRunOnDrawEnd  = new LinkedList<>();
		//Set default filer

		setFilter(new FilterOES());
	}

	public void onResume()
	{
		//setFilter again safe check.


	}

	//Called after startpreview has started.
	public void initPreviewFrameRenderer(int previewWidth,
							 int previewHeight)
	{
		mPreviewWidth = previewWidth;
		mPreviewHeight = previewHeight;

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

	void processVideoFrame(byte[] videoFrame)
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
	}

	public void flipWithCallBackReset(final int cameraFacing)
	{
		setCallBackBasedOnFilter();
		flip(cameraFacing);
	}

	public void flip(final int cameraFacing)
	{
		//Optimize this call dont do buffer creations here.
		//Just buffer ids here to make it performant but on doing in this we have cost of extra memory at native side.
		//Take a call on this or use profiler.
		runOnDrawEnd(new Runnable() {
			@Override
			public void run() {
				if(cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
				{
					mVertexBuffer = ByteBuffer.allocateDirect(mVerticesFrontCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
					mVertexBuffer.put(mVerticesFrontCamera).position(0);
				}
				else
				{
					mVertexBuffer = ByteBuffer.allocateDirect(mVerticesBackCamera.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
					mVertexBuffer.put(mVerticesBackCamera).position(0);
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
		mWidth = width;
		mHeight = height;
		GLES20.glViewport(0, 0, mWidth, mHeight);

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

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		runAll(mRunOnDraw); //Using this queue to process anything that comes from thread withoutGL context.

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
}