package wiki.hike.neo.hikecamera;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import wiki.hike.neo.hikecamera.gl.CameraEngine;

public class MainActivity extends Activity implements View.OnClickListener {

	CameraEngine mCameraEngine;
	ImageView btn_cross,btn_flip,btn_capture;
	TextView txt_fps,txt_rendermode,txt_opentime,txt_fliptime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_work);

		txt_fps = (TextView) findViewById(R.id.fps);
		txt_rendermode = (TextView) findViewById(R.id.rendermode);
		txt_opentime = (TextView) findViewById(R.id.opentime);
		txt_fliptime = (TextView) findViewById(R.id.fliptime);

		btn_cross = (ImageView) findViewById(R.id.btn_camera_cancel);
		btn_flip = (ImageView) findViewById(R.id.btn_camera_switch);
		btn_capture = (ImageView) findViewById(R.id.btn_camera_capture);
		btn_cross.setOnClickListener(this);
		btn_flip.setOnClickListener(this);
		btn_capture.setOnClickListener(this);

		GLSurfaceView mSurfaceView = (GLSurfaceView)findViewById(R.id.renderer_view);
		mCameraEngine = new CameraEngine(mSurfaceView,this.getApplicationContext());
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_camera_capture:
				mCameraEngine.processCommand(CameraEngine.COMMAND_RECORD);
				break;
			case R.id.btn_camera_cancel:
				this.finish();
				break;
			case R.id.btn_camera_switch:
				mCameraEngine.processCommand(CameraEngine.COMMAND_FLIP);
				break;
		}

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
