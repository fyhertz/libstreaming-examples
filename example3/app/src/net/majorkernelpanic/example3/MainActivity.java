package net.majorkernelpanic.example3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, RtspClient.Callback, Session.Callback, SurfaceHolder.Callback {

	public final static String TAG = "MainActivity";

	private Button mButtonStart;
	private Button mButtonFlash;
	private Button mButtonCamera;
	private Button mButtonQuality;
	private Button mButtonSelect;
	private RadioGroup mRadioGroup;
	private LinearLayout mLayoutMenu; 
	private SurfaceView mSurfaceView;
	private TextView mTextBitrate;
	private EditText mEditTextURI;
	private EditText mEditTextPassword;
	private EditText mEditTextUsername;
	private Session mSession;
	private RtspClient mClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		mButtonStart = (Button) findViewById(R.id.start);
		mButtonFlash = (Button) findViewById(R.id.flash);
		mButtonCamera = (Button) findViewById(R.id.camera);
		mButtonQuality = (Button) findViewById(R.id.quality);
		mButtonSelect = (Button) findViewById(R.id.select);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditTextURI = (EditText) findViewById(R.id.uri);
		mEditTextUsername = (EditText) findViewById(R.id.username);
		mEditTextPassword = (EditText) findViewById(R.id.password);
		mTextBitrate = (TextView) findViewById(R.id.bitrate);
		mLayoutMenu =  (LinearLayout) findViewById(R.id.menu);
		mRadioGroup =  (RadioGroup) findViewById(R.id.radio);

		mButtonStart.setOnClickListener(this);
		mButtonFlash.setOnClickListener(this);
		mButtonCamera.setOnClickListener(this);
		mButtonQuality.setOnClickListener(this);
		mButtonSelect.setOnClickListener(this);
		
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		mEditTextURI.setText(mPrefs.getString("uri", getString(R.string.default_stream)));
		mEditTextPassword.setText(mPrefs.getString("password", getString(R.string.default_password)));
		mEditTextUsername.setText(mPrefs.getString("username", getString(R.string.default_username)));
		
		// Configures the SessionBuilder
		mSession = SessionBuilder.getInstance()
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setAudioQuality(new AudioQuality(8000,16000))
		.setVideoEncoder(SessionBuilder.VIDEO_H264)
		.setSurfaceView(mSurfaceView)
		.setPreviewOrientation(0)
		.setCallback(this)
		.build();

		// Configures the RTSP client
		mClient = new RtspClient();
		mClient.setSession(mSession);
		mClient.setCallback(this);

		// Use this to force streaming with the MediaRecorder API
		//mSession.getVideoTrack().setStreamingMethod(MediaStream.MODE_MEDIARECORDER_API);
	
		mSurfaceView.getHolder().addCallback(this);
		
		selectQuality();
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start:
			disableUI();
			toggleStream();
			break;
		case R.id.flash:
			mSession.toggleFlash();
			break;
		case R.id.camera:
			mSession.switchCamera();
			break;
		case R.id.quality:
			mLayoutMenu.setVisibility(View.VISIBLE);
			break;
		case R.id.select:
			mLayoutMenu.setVisibility(View.GONE);
			selectQuality();
			break;	
		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		mClient.release();
		mSession.release();
	}

	private void selectQuality() {
		int id = mRadioGroup.getCheckedRadioButtonId();
		RadioButton button = (RadioButton) findViewById(id);
		String text = button.getText().toString();
		Pattern pattern = Pattern.compile("(\\d+)x(\\d+)\\D+(\\d+)\\D+(\\d+)");
		Matcher matcher = pattern.matcher(text);

		matcher.find();
		int width = Integer.parseInt(matcher.group(1));
		int height = Integer.parseInt(matcher.group(2));
		int framerate = Integer.parseInt(matcher.group(3));
		int bitrate = Integer.parseInt(matcher.group(4))*1000;

		mSession.setVideoQuality(new VideoQuality(width, height, framerate, bitrate));
		Toast.makeText(this, ((RadioButton)findViewById(id)).getText(), Toast.LENGTH_SHORT).show();
		
		Log.d(TAG, "Selected resolution: "+width+"x"+height);
	}

	private void enableUI() {
		mButtonStart.setEnabled(true);
		mButtonCamera.setEnabled(true);
	}

	private void disableUI() {
		mButtonStart.setEnabled(false);
		mButtonCamera.setEnabled(false);
	}

	// Connects/disconnects to the RTSP server and starts/stops the stream
	public void toggleStream() {
		if (!mClient.isStreaming()) {
			String ip,port,path;

			// We save the content user inputs in Shared Preferences
			SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			Editor editor = mPrefs.edit();
			editor.putString("uri", mEditTextURI.getText().toString());
			editor.putString("password", mEditTextPassword.getText().toString());
			editor.putString("username", mEditTextUsername.getText().toString());
			editor.commit();

			// We parse the URI written in the Editext
			Pattern uri = Pattern.compile("rtsp://(.+):(\\d+)/(.+)");
			Matcher m = uri.matcher(mEditTextURI.getText()); m.find();
			ip = m.group(1);
			port = m.group(2);
			path = m.group(3);
			
			mClient.setCredentials(mEditTextUsername.getText().toString(), mEditTextPassword.getText().toString());
			mClient.setServerAddress(ip, Integer.parseInt(port));
			mClient.setStreamPath("/"+path);
			mClient.startStream();
			
		} else {
			// Stops the stream and disconnects from the RTSP server
			mClient.stopStream();
		}
	}

	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		// Displays a popup to report the eror to the user
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onBitrareUpdate(long bitrate) {
		mTextBitrate.setText(""+bitrate/1000+" kbps");
	}

	@Override
	public void onPreviewStarted() {
		if (mSession.getCamera() == CameraInfo.CAMERA_FACING_FRONT)
			mButtonFlash.setEnabled(false);
		else 
			mButtonFlash.setEnabled(true);
	}	
	
	@Override
	public void onSessionConfigured() {
		
	}

	@Override
	public void onSessionStarted() {
		enableUI();
		mButtonStart.setText(R.string.stop);
	}

	@Override
	public void onSessionStopped() {
		enableUI();
		mButtonStart.setText(R.string.start);
	}

	@Override
	public void onSessionError(int reason, int streamType, Exception e) {
		switch (reason) {
		case Session.ERROR_CAMERA_ALREADY_IN_USE:
			break;
		case Session.ERROR_CAMERA_HAS_NO_FLASH:
			break;
		case Session.ERROR_INVALID_SURFACE:
			break;
		case Session.ERROR_STORAGE_NOT_READY:
			break;
		case Session.ERROR_CONFIGURATION_NOT_SUPPORTED:
			break;
		case Session.ERROR_OTHER:
			break;
		}
		
		if (e != null) {
			logError(e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void onRtspUpdate(int message, Exception e) {
		switch (message) {
		case RtspClient.ERROR_CONNECTION_FAILED:
		case RtspClient.ERROR_WRONG_CREDENTIALS:
			enableUI();
			logError(e.getMessage());
			e.printStackTrace();
			break;
		}
	}
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSession.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mClient.stopStream();
	}
}