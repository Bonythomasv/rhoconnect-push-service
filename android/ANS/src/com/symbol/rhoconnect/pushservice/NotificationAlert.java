/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * This Activity displays the full notification message that was sent due
 * to an error/warning/info message.
 * 
 */
public class NotificationAlert extends Activity implements OnClickListener {

	private static final String TAG = "NotificationAlert-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		if (V) Log.d(TAG, "--- onCreate() ---");
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.alert);
		setTitle("Asynchronous Notification Service");

		// Setup the click listeners
		View okButton = findViewById(R.id.okButton);
		okButton.setOnClickListener(this);
		TextView messageText = (TextView)findViewById(R.id.messageText);
		Intent intent = getIntent();
		messageText.setText(intent.getStringExtra(ANSApplication.ANS_NOTIFICATION_MESSAGE_EXTRA));
	}
	//
	// OnClickListener method
	//
	public void onClick(View v) {
		
		if (V) Log.d(TAG, "--- onClick(" + v.getId() + ") ---");
		
		switch (v.getId()) {
		case R.id.okButton:
			finish();
			break;
		}
	}
}
