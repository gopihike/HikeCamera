package wiki.hike.neo.hikecamera.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: RenderHandler.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import wiki.hike.neo.hikecamera.gl.Filter;

/**
 * Helper class to draw texture to whole view on private thread
 */
public final class RenderHandler implements Runnable {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "RenderHandler";

	private final Object mSync = new Object();
    private EGLContext mShard_context;
    private boolean mIsRecordable;
    private Object mSurface;
	private  int mTexId = -1;
	private float[] mMatrix = new float[32];

	private boolean mRequestSetEglContext;
	private boolean mRequestRelease;
	private int mRequestDraw;
	private FloatBuffer mTextureMatrixBuff;
	private FloatBuffer mMVPMatrixBuff;


	Filter mFilter;
	boolean mBufferCreation  = false;
	private FloatBuffer mVertexBuffer = null;
	private ShortBuffer mIndexBuffer = null;

	protected float[] mVerticesForSampler2D = {
			-1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
			1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
			-1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 1.0f };
	protected int mVertexBufferObjectId;
	protected int mElementBufferObjectId;
	int []mTex = {-1};
	protected short[] mIndices = {0, 1, 2, 1, 2, 3};
	protected final int FLOAT_SIZE_BYTES = 4;
	protected final int SHORT_SIZE_BYTES = 2;


	public static final RenderHandler createHandler(final String name) {
		if (DEBUG) Log.v(TAG, "createHandler:");
		final RenderHandler handler = new RenderHandler();
		synchronized (handler.mSync) {
			new Thread(handler, !TextUtils.isEmpty(name) ? name : TAG).start();
			try {
				handler.mSync.wait();
			} catch (final InterruptedException e) {
			}
		}
		return handler;
	}

	public final void setEglContext(final EGLContext shared_context, final int tex_id, final Object surface, final boolean isRecordable) {
		if (DEBUG) Log.i(TAG, "setEglContext:");
		if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof SurfaceHolder))
			throw new RuntimeException("unsupported window type:" + surface);
		synchronized (mSync) {
			if (mRequestRelease) return;
			mShard_context = shared_context;
			mTexId = tex_id;
			mSurface = surface;
			mIsRecordable = isRecordable;
			mRequestSetEglContext = true;
			Matrix.setIdentityM(mMatrix, 0);
			Matrix.setIdentityM(mMatrix, 16);
			mSync.notifyAll();
			try {
				mSync.wait();
			} catch (final InterruptedException e) {
			}
		}
	}

	public final void draw() {
		draw(mTexId, mMatrix, null);
	}

	public final void draw(final int tex_id) {
		draw(tex_id, mMatrix, null);
	}

	public final void draw(final float[] tex_matrix) {
		draw(mTexId, tex_matrix, null);
	}

	public final void draw(final FloatBuffer tex_matrix, final FloatBuffer mvp_matrix) {
		mTextureMatrixBuff =  mvp_matrix;
		mMVPMatrixBuff = tex_matrix;
		draw();
	}

	public final void draw(final int tex_id, final float[] tex_matrix) {
		draw(tex_id, tex_matrix, null);
	}

	public final void draw(final int tex_id, final float[] tex_matrix, final float[] mvp_matrix) {
		synchronized (mSync) {
			if (mRequestRelease) return;
			mTexId = tex_id;
			if ((tex_matrix != null) && (tex_matrix.length >= 16)) {
				System.arraycopy(tex_matrix, 0, mMatrix, 0, 16);
			} else {
				Matrix.setIdentityM(mMatrix, 0);
			}
			if ((mvp_matrix != null) && (mvp_matrix.length >= 16)) {
				System.arraycopy(mvp_matrix, 0, mMatrix, 16, 16);
			} else {
				Matrix.setIdentityM(mMatrix, 16);
			}
			mRequestDraw++;
			mSync.notifyAll();
/*			try {
				mSync.wait();
			} catch (final InterruptedException e) {
			} */
		}
	}

	public boolean isValid() {
		synchronized (mSync) {
			return !(mSurface instanceof Surface) || ((Surface)mSurface).isValid();
		}
	}

	public final void release() {
		if (DEBUG) Log.i(TAG, "release:");
		synchronized (mSync) {
			if (mRequestRelease) return;
			mRequestRelease = true;
			mSync.notifyAll();
			try {
				mSync.wait();
			} catch (final InterruptedException e) {
			}
		}
	}

//********************************************************************************
//********************************************************************************
	private EGLBase mEgl;
	private EGLBase.EglSurface mInputSurface;

	@Override
	public final void run() {
		if (DEBUG) Log.i(TAG, "RenderHandler thread started:");
		synchronized (mSync) {
			mRequestSetEglContext = mRequestRelease = false;
			mRequestDraw = 0;
			mSync.notifyAll();
		}
        boolean localRequestDraw;
        for (;;) {
        	synchronized (mSync) {
        		if (mRequestRelease) break;
	        	if (mRequestSetEglContext) {
	        		mRequestSetEglContext = false;
	        		internalPrepare();
	        	}
	        	localRequestDraw = mRequestDraw > 0;
	        	if (localRequestDraw) {
	        		mRequestDraw--;
//					mSync.notifyAll();
				}
        	}
        	if (localRequestDraw) {
        		if ((mEgl != null) && mTexId >= 0) {
            		mInputSurface.makeCurrent();
					// clear screen with yellow color so that you can see rendering rectangle
					GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
					GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

					if(mBufferCreation == false)
					{
						mFilter = new Filter();
						mFilter.init();

						//Create texture buffer for filters.
						mVertexBuffer = ByteBuffer.allocateDirect(mVerticesForSampler2D.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
						mVertexBuffer.put(mVerticesForSampler2D).position(0);

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

						mBufferCreation = true;
					}


					//GPUImageFilter mFilter = new GPUImageFilter();
					//mFilter.init();
					//mFilter.onOutputSizeChanged(1000, 4000);
					mTex[0] = mTexId;

					mFilter.onDraw(mTex, mVertexBufferObjectId, mElementBufferObjectId);

//            		mDrawer.draw(mTexId, mMatrix);
            		mInputSurface.swap();
        		}
        	} else {
        		synchronized(mSync) {
        			try {
						mSync.wait();
					} catch (final InterruptedException e) {
						break;
					}
        		}
        	}
        }
        synchronized (mSync) {
        	mRequestRelease = true;
            internalRelease();
            mSync.notifyAll();
        }
		if (DEBUG) Log.i(TAG, "RenderHandler thread finished:");
	}

	private final void internalPrepare() {
		if (DEBUG) Log.i(TAG, "internalPrepare:");
		internalRelease();
		mEgl = new EGLBase(mShard_context, false, mIsRecordable);

   		mInputSurface = mEgl.createFromSurface(mSurface);

		mInputSurface.makeCurrent();
		mSurface = null;
		mSync.notifyAll();
	}

	private final void internalRelease() {
		if (DEBUG) Log.i(TAG, "internalRelease:");
		if (mInputSurface != null) {
			mInputSurface.release();
			mInputSurface = null;
		}
		if (mEgl != null) {
			mEgl.release();
			mEgl = null;
		}
	}

}
