package net.majorkernelpanic.example1;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 * A straightforward example of how to use the RTSP server included in libstreaming
 */
public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";

	private SurfaceView mSurfaceView;
	private EditText mEditText;
	private Session mSession;
	private PowerManager.WakeLock mWakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface);

		// Required on old version of Android
		mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Sets the port of the RTSP server to 1234
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
		editor.commit();

		// Configures the SessionBuilder
		// Now, by default a client connecting to the RTSP
		// server will receive AMR and H.263
		SessionBuilder.getInstance()
		.setSurfaceHolder(mSurfaceView.getHolder())
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_AMRNB)
		.setVideoEncoder(SessionBuilder.VIDEO_H263);
		
		// Starts the RTSP server
		this.startService(new Intent(this,RtspServer.class));

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,"net.majorkernelpanic.example1.wakelock");


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

}
