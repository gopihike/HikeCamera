package wiki.hike.neo.hikecamera.gl;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;


public class CameraRenderer implements
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {
	private Context mContext;
	private CameraManager mManager;
	private SurfaceTexture mSurfaceTexture;
	GLSurfaceView mSurfaceView;
	private FilterGPU mFilter = null;

	//Renderer observer
	private Observer mObserver;

	//private final FBORenderTarget mRenderTarget = new FBORenderTarget();
	private final OESTexture mCameraTexture = new OESTexture();
	private int mWidth, mHeight;
	private boolean updateTexture = false;

	public CameraRenderer(Context context) {
		//super(context);
		mContext = context;
		mFilter = new FilterGPU();
	}

	public void initRenderer()
	{

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


	@Override
	public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {

	}

	public void setObserver(Observer observer) {
		mObserver = observer;
	}

	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera) {

	}

	public interface Observer {
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture, int width, int height);
	}

	@Override
	public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		GLES20.glViewport(0, 0, mWidth, mHeight);

		//generate camera texture------------------------
		mCameraTexture.init();

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

	void setCallBackBasedOnFilter()
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

	}

	void setFilter(FilterGPU filter){
		mFilter = filter;
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		if(mFilter.isFilterInitialised() == false)
		{
			mFilter.init();
		}

		//render the texture to FBO if new frame is available
		switch (mFilter.getRenderType())
		{
			case Filter.RENDER_TYPE_PREVIEW_BUFFER:

				break;
			case Filter.RENDER_TYPE_SURFACE_TEXTURE:
				if(updateTexture){
					mSurfaceTexture.updateTexImage();
					//mSurfaceTexture.getTransformMatrix(mFilter.getTransformMatrix());
					updateTexture = false;
					mFilter.onDraw(mCameraTexture.getTextureId());
				}
				break;
		}
	}
	
	public void onDestroy(){
		updateTexture = false;
		mSurfaceTexture.release();
	}
	
}