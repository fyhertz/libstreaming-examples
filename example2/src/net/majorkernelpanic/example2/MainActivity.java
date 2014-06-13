package net.majorkernelpanic.example2;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * A straightforward example of how to stream AMR and H.263 to some public IP using libstreaming.
 * Note that this example may not be using the latest version of libstreaming !
 */
public class MainActivity extends Activity implements OnClickListener, Session.Callback, SurfaceHolder.Callback {

	private final static String TAG = "MainActivity";

	private Button mButton1, mButton2;
	private SurfaceView mSurfaceView;
	private EditText mEditText;
	private Session mSession;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditText = (EditText) findViewById(R.id.editText1);

		mSession = SessionBuilder.getInstance()
		.setCallback(this)
		.setSurfaceView(mSurfaceView)
		.setPreviewOrientation(90)
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setAudioQuality(new AudioQuality(16000, 32000))
		.setVideoEncoder(SessionBuilder.VIDEO_H264)
		.setVideoQuality(new VideoQuality(320,240,20,500000))
		.build();

		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);

		mSurfaceView.getHolder().addCallback(this);
		
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSession.isStreaming()) {
			mButton1.setText(R.string.stop);
		} else {
			mButton1.setText(R.string.start);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSession.release();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button1) {
			// Starts/stops streaming
			mSession.setDestination(mEditText.getText().toString());
			if (!mSession.isStreaming()) {
				mSession.configure();
			} else {
				mSession.stop();
			}
			mButton1.setEnabled(false);
		} else {
			// Switch between the two cameras
			mSession.switchCamera();
		}
	}

	@Override
	public void onBitrateUpdate(long bitrate) {
		Log.d(TAG,"Bitrate: "+bitrate);
	}

	@Override
	public void onSessionError(int message, int streamType, Exception e) {
		mButton1.setEnabled(true);
		if (e != null) {
			logError(e.getMessage());
		}
	}

	@Override
	
	public void onPreviewStarted() {
		Log.d(TAG,"Preview started.");
	}

	@Override
	public void onSessionConfigured() {
		Log.d(TAG,"Preview configured.");
		// Once the stream is configured, you can get a SDP formated session description
		// that you can send to the receiver of the stream.
		// For example, to receive the stream in VLC, store the session description in a .sdp file
		// and open it with VLC while streming.
		Log.d(TAG, mSession.getSessionDescription());
		mSession.start();
	}

	@Override
	public void onSessionStarted() {
		Log.d(TAG,"Session started.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.stop);
	}

	@Override
	public void onSessionStopped() {
		Log.d(TAG,"Session stopped.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.start);
	}	
	
	/** Displays a popup to report the eror to the user */
	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSession.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSession.stop();
	}

}
