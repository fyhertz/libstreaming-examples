package net.majorkernelpanic.example2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.video.VideoStream;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera.CameraInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * A straightforward example of how to stream AMR and H.263 to some public IP using libstreaming.
 * Note that this example may not be using the latest version of libstreaming !
 */
public class MainActivity extends Activity implements OnClickListener {

	private final static String TAG = "MainActivity";

	private Button mButton1, mButton2;
	private SurfaceView mSurfaceView;
	private EditText mEditText;
	private Session mSession;
	private PowerManager.WakeLock mWakeLock;

	private int mCurrentCamera = CameraInfo.CAMERA_FACING_BACK;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditText = (EditText) findViewById(R.id.editText1);

		// Required on old version of Android
		mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setSurfaceHolder(mSurfaceView.getHolder())
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setVideoEncoder(SessionBuilder.VIDEO_H264);

		// Creates the Session
		try {
			mSession = SessionBuilder.getInstance().build();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,"net.majorkernelpanic.example1.wakelock");

		mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (mSession != null)
					try {
						mSession.getVideoTrack().startPreview();
					} catch (RuntimeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {}

		});

		runOnUiThread(mUpdateBitrate);
		
	}

	@Override
	public void onStart() {
		super.onStart();
		// Lock screen
		mWakeLock.acquire();
	}

	@Override
	public void onStop() {
		super.onStop();
		// Unlock screen
		if (mWakeLock.isHeld()) mWakeLock.release();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSession != null && mSession.isStreaming()) {
			mButton1.setText(R.string.stop);
		} else {
			mButton1.setText(R.string.start);
		}
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == R.id.button1) {

			// Starts/stops streaming
			new ToggleStreamingTask().execute();

		} else {

			// Switch between the two cameras
			new SwitchCamerasTask().execute();

		}

	}	

	// An AsyncTask for switching between the two cameras of the phone
	private class SwitchCamerasTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (mSession != null) {
				
				// Disable the UI while this task is going on
				runOnUiThread(mDisableUI);
				
				VideoStream stream = mSession.getVideoTrack();
				boolean streaming = stream.isStreaming();
				mCurrentCamera = (mCurrentCamera == CameraInfo.CAMERA_FACING_BACK) ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK; 
				stream.setCamera(mCurrentCamera);
				stream.stopPreview();
				try {
					mSession.getVideoTrack().startPreview();
					// We restart the stream if needed
					if (streaming) mSession.getVideoTrack().start(); 
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// We can now reenable the UI
			runOnUiThread(mEnableUI);
			
			return null;
		}

	};

	// An AsyncTask that starts or stops the stream asynchronously
	private class ToggleStreamingTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (mSession !=null) {
				
				// Disable the UI while this task is going on
				runOnUiThread(mDisableUI);
				
				try {
					if (!mSession.isStreaming()) {

						// Must be called off the main thread
						InetAddress destination = InetAddress.getByName(mEditText.getText().toString());

						// Starts streaming the IP address indicated in the 
						mSession.setDestination(destination);

						// Returns the SDP that wou will need to send to the receiver for
						// him to decode the stream properly. Must be called before starting
						// the stream.
						// Must be called off the main thread as well !
						String sdp = mSession.getSessionDescription();
						
						// We save the SDP in a file
						if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
							File file = new File(Environment.getExternalStorageDirectory().getPath() +  "/stream.sdp");
							OutputStream os = new FileOutputStream(file);
							os.write(sdp.getBytes());
							os.close();
						}

						// Starts the stream
						mSession.start();

						// Updates the title of the button
						runOnUiThread(mRunnableStop);

					} else {

						// Stops the stream
						mSession.stop();

						// Updates the title of the button
						runOnUiThread(mRunnableStart);

					}
				} catch (Exception e) {
					Log.e(TAG,e.getMessage());
					e.printStackTrace();
				}
				
				// We can now reenable the UI
				runOnUiThread(mEnableUI);
			}
			return null;
		}

	};

	private Runnable mEnableUI = new Runnable() {
		@Override
		public void run() {
			mButton1.setEnabled(true);
			mButton2.setEnabled(true);
		}
	};
	
	// When an asynchronous operation is going on (such as toggling the stream) we disable
	// the UI to prevent the user to start mutliple instances of the task
	private Runnable mDisableUI = new Runnable() {
		@Override
		public void run() {
			mButton1.setEnabled(false);
			mButton2.setEnabled(false);
		}
	};
	
	private Runnable mRunnableStart = new Runnable() {
		@Override
		public void run() {
			mButton1.setText(R.string.start);
		}
	};	

	private Runnable mRunnableStop = new Runnable() {
		@Override
		public void run() {
			mButton1.setText(R.string.stop);
		}
	};
	
	private Runnable mUpdateBitrate = new Runnable() {
		@Override
		public void run() {
			long bitrate =  (mSession != null) ? mSession.getBitrate() : 0;
			Log.d(TAG,"Bitrate: "+bitrate);
			new Handler().postDelayed(mUpdateBitrate, 1000);
		}
	};

}
