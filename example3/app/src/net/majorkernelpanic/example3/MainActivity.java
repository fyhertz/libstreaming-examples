package net.majorkernelpanic.example3;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.rtsp.RtspClient.Callback;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, Callback {

	public final static String TAG = "MainActivity";

	private VideoQuality mQuality;
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
	private PowerManager.WakeLock mWakeLock;
	private RtspClient mClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

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

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setSurfaceHolder(mSurfaceView.getHolder())
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_AAC)
		.setAudioQuality(new AudioQuality(16000,16000))
		.setVideoEncoder(SessionBuilder.VIDEO_H264);

		// Configures the RTSP client
		mClient = new RtspClient();
		mClient.setCallback(this);

		// Creates the Session
		try {
			mSession = SessionBuilder.getInstance().build();
			selectQuality();
			mClient.setSession(mSession);
		} catch (Exception e) {
			logError(e.getMessage());
			e.printStackTrace();
		}

		mButtonStart.setOnClickListener(this);
		mButtonFlash.setOnClickListener(this);
		mButtonCamera.setOnClickListener(this);
		mButtonQuality.setOnClickListener(this);
		mButtonSelect.setOnClickListener(this);

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,"net.majorkernelpanic.example3.wakelock");

		mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (mSession != null) {
					try {
						if (mSession.getVideoTrack() != null) {
							mSession.getVideoTrack().setVideoQuality(mQuality);
							mSession.getVideoTrack().startPreview();
						}
					} catch (RuntimeException e) {
						logError(e.getMessage());
						e.printStackTrace();
					} catch (IOException e) {
						logError(e.getMessage());
						e.printStackTrace();
					}
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				mClient.stopStream();
			}

		});		

		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		mEditTextURI.setText(mPrefs.getString("uri", getString(R.string.default_stream)));
		mEditTextPassword.setText(mPrefs.getString("password", getString(R.string.default_password)));
		mEditTextUsername.setText(mPrefs.getString("username", getString(R.string.default_username)));

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
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start:
			disableUI();
			toggleStream();
			break;
		case R.id.flash:
			try {
				mSession.getVideoTrack().toggleFlash();
			} catch (Exception e) {
				logError(e.getMessage());
				e.printStackTrace();
			}
			break;
		case R.id.camera:
			disableUI();
			new SwitchCamerasTask().execute();
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

		mQuality = new VideoQuality(width, height, framerate, bitrate, 0);
		Toast.makeText(this, ((RadioButton)findViewById(id)).getText(), Toast.LENGTH_SHORT).show();
	}

	private void enableUI() {
		mButtonStart.setEnabled(true);
		mButtonCamera.setEnabled(true);
		mButtonFlash.setEnabled(true);
	}

	private void disableUI() {
		mButtonStart.setEnabled(false);
		mButtonCamera.setEnabled(false);
		mButtonFlash.setEnabled(false);
	}

	// An AsyncTask for switching between the two cameras of the phone
	private class SwitchCamerasTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				mSession.getVideoTrack().switchCamera();
			} catch (Exception e) {
				logError(e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			enableUI();
		}

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

			// Connection to the RTSP server
			if (mSession.getVideoTrack() != null) {
				mSession.getVideoTrack().setVideoQuality(mQuality);
			}
			mClient.setCredentials(mEditTextUsername.getText().toString(), mEditTextPassword.getText().toString());
			mClient.setServerAddress(ip, Integer.parseInt(port));
			mClient.setStreamPath("/"+path);
			mClient.startStream();
			
			mHandler.postDelayed(mUpdateBitrate, 500);
		} else {
			// Stops the stream and disconnects from the RTSP server
			mClient.stopStream();
		}
	}


	private final Handler mHandler = new Handler();

	private Runnable mUpdateBitrate = new Runnable() {
		@Override
		public void run() {
			if (mSession != null && mClient.isStreaming()) { 
				long bitrate =  (mSession != null) ? mSession.getBitrate() : 0;
				mTextBitrate.setText(""+bitrate/1000+" Kbps");
				mHandler.postDelayed(mUpdateBitrate, 500);
			} else {
				mTextBitrate.setText("0 Kbps");
			}
		}
	};

	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		Log.e(TAG,error);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// Displays a popup to report the eror to the user
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});
	}

	@Override
	public void onStatusUpdate(int message, Exception e) {
		switch (message) {
		case RtspClient.MESSAGE_STREAM_STARTED:
			enableUI();
			mButtonStart.setText(R.string.stop);
			break;
		case RtspClient.MESSAGE_STREAM_STOPPED:
			enableUI();
			mButtonStart.setText(R.string.start);
			break;
		case RtspClient.MESSAGE_CONNECTION_FAILED:
		case RtspClient.MESSAGE_START_FAILED:
		case RtspClient.MESSAGE_WRONG_CREDENTIALS:
			enableUI();
			logError(e.getMessage());
			e.printStackTrace();
			break;
		}
	}

}