package wiki.hike.neo.hikecamera;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import wiki.hike.neo.hikecamera.gl.CameraEngine;


public class MainActivity extends Activity {

	CameraEngine mCameraEngine;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		GLSurfaceView mSurfaceView = (GLSurfaceView)findViewById(R.id.renderer_view);
		mCameraEngine = new CameraEngine(mSurfaceView,this.getApplicationContext());
	}

	@Override
	public void onStart(){
		super.onStart();
	}

	@Override
	public void onPause(){
		super.onPause();
		mCameraEngine.onPause();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mCameraEngine.onResume();

	}

}
