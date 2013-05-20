package net.majorkernelpanic.example2;

import java.io.IOException;
import java.net.InetAddress;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

	private Button mButton;
	private SurfaceView mSurfaceView;
	private EditText mEditText;
	private Session mSession;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mButton = (Button) findViewById(R.id.button1);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditText = (EditText) findViewById(R.id.editText1);

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setSurfaceHolder(mSurfaceView.getHolder())
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_AMRNB)
		.setVideoEncoder(SessionBuilder.VIDEO_H263);
		
		// Creates the Session
		try {
			mSession = SessionBuilder.getInstance().build();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		mButton.setOnClickListener(this);

	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSession != null && mSession.isStreaming()) {
			mButton.setText(R.string.stop);
		} else {
			mButton.setText(R.string.start);
		}
	}
	
	@Override
	public void onClick(View v) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				if (mSession !=null) {
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
							mSession.getSessionDescription();
							
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
					}
				}
				return null;
			}
			
		}).execute();
	}	

	private Runnable mRunnableStart = new Runnable() {
		@Override
		public void run() {
			mButton.setText(R.string.start);
		}
	};	
	
	private Runnable mRunnableStop = new Runnable() {
		@Override
		public void run() {
			mButton.setText(R.string.stop);
		}
	};

}
