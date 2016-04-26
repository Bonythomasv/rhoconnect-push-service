/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ANSReceiver extends BroadcastReceiver {
	
	private static final String TAG = "ANSReceiver-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;

	/**
	 * Simply start the service when a user logs in or when the device boots or when the network status changes
	 */

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		if (D) Log.d(TAG, "@@@ onReceive (context, " + intent.toString() + ") @@@");
		
		String intentAction = null;
		if (intent != null) 
			intentAction = intent.getAction();
		
		// Check to see why we received this intent
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intentAction)) {
			// Send an explicit intent to the ANSService object telling it that the status of the
			// network has changed
			Intent networkChangeIntent = new Intent(ANSService.NETWORK_CHANGE_ACTION, null, context, ANSService.class);
			networkChangeIntent.putExtra(ConnectivityManager.EXTRA_NETWORK_INFO, intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO));
			context.startService(networkChangeIntent);
		}
		else {
			// Send an explicit intent to the ANSService object telling it to startup
			//
			context.startService(new Intent(ANSService.START_SERVICE_ACTION, null, context, ANSService.class));
		}
	}

}
