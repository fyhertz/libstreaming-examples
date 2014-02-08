package net.majorkernelpanic.example1;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

/**
 * A straightforward example of how to use the RTSP server included in libstreaming.
 */
public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";

	private SurfaceView mSurfaceView;
	private PowerManager.WakeLock mWakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		
		// Sets the port of the RTSP server to 1234
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
		editor.commit();

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setSurfaceView(mSurfaceView)
		.setPreviewOrientation(90)
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setVideoEncoder(SessionBuilder.VIDEO_H264);
		
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
