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
import java.util.LinkedList;
import java.util.Queue;


public class CameraRenderer implements
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {
	private Context mContext;
	private CameraManager mManager;
	private SurfaceTexture mSurfaceTexture;
	GLSurfaceView mSurfaceView;
	private FilterPreview mFilter = null;

	private Queue<Runnable> mRunOnDraw = null;

	//Renderer observer
	private Observer mObserver;

	//private final FBORenderTarget mRenderTarget = new FBORenderTarget();
	private final PreviewFrameTexture  mPreviewFrameTexture = new PreviewFrameTexture();
	private final OESTexture mCameraTexture = new OESTexture();
	private int mWidth, mHeight;
	private int mPreviewWidth,mPreviewHeight;
	private int mOreintation;
	boolean mIsFrontFacing;

	ByteBuffer mBufferY;
	ByteBuffer mBufferUV;

	private boolean updateTexture = false;

	public CameraRenderer(Context context) {
		//super(context);
		mContext = context;
		mRunOnDraw = new LinkedList<>();

		//Set default filer
		setFilter(new FilterPreview());
	}


	//Called after preview has started.
	public void initPreviewFrameRenderer(int orientation, int previewWidth,
							 int previewHeight, boolean isFrontFacing)
	{
		mPreviewWidth = previewWidth;
		mPreviewHeight = previewHeight;

		mOreintation = orientation;
		mIsFrontFacing = isFrontFacing;

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

	  	Camera.Size size = camera.getParameters().getPreviewSize();


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
			/*if (mRunOnDraw.isEmpty()) {
			runOnDraw(new Runnable() {
				@Override
				public void run() {

					if (prevBytes == null || prevBytes.length != bytes.length) {
						prevBytes = new byte[bytes.length];
					}
					System.arraycopy(bytes, 0, prevBytes, 0, bytes.length);
					processVideoFrame(prevBytes);
					camera.addCallbackBuffer(bytes);
				}
			});
			}*/

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
		mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
		if (oldSurfaceTexture != null) {
			oldSurfaceTexture.release();
		}

		mSurfaceTexture.setOnFrameAvailableListener(this);

		//Surface texture created.
		if (mObserver != null) {
			mObserver.onSurfaceTextureCreated(mSurfaceTexture, mWidth, mHeight);
		}

		//start render---------------------
		mSurfaceView.requestRender();
		//requestRender();
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
				mFilter.onDraw(mPreviewFrameTexture.getYTextureHandle(),mPreviewFrameTexture.getUVTextureHandle());
				break;
			case Filter.RENDER_TYPE_SURFACE_TEXTURE:
				if(updateTexture){
					mSurfaceTexture.updateTexImage();
					//mSurfaceTexture.getTransformMatrix(mFilter.getTransformMatrix());
					updateTexture = false;
					//mFilter.onDraw(mCameraTexture.getTextureId());
				}
				break;
		}
	}
	
	public void onDestroy(){
		updateTexture = false;
		mSurfaceTexture.release();
	}


	/*void setCallBackBasedOnFilter()
	{
		switch (mFilter.getRenderType())
		{
			case Filter.RENDER_TYPE_PREVIEW_BUFFER:
				mSurfaceTexture.setOnFrameAvailableListener(null);
				//mCamera.setPreviewCallbackWithBuffer(this);
				break;
			case Filter.RENDER_TYPE_SURFACE_TEXTURE:

				//mCamera.setPreviewCallbackWithBuffer(null);
				mSurfaceTexture.setOnFrameAvailableListener(this);
				break;
		}

	}*/

	public void setFilter(final FilterPreview filter) {
		runOnDraw(new Runnable() {

			@Override
			public void run() {
				final FilterPreview oldFilter = mFilter;
				mFilter = filter;
				if (oldFilter != null) {
					oldFilter.destroy();
				}
				mFilter.init();
				//mFilter.init(mOreintation,mPreviewWidth,mPreviewHeight,mIsFrontFacing,mWidth,mHeight);
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

	public void setObserver(Observer observer) {
		mObserver = observer;
	}



	public interface Observer {
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture, int width, int height);
	}


}