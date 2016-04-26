/**
 * This service is used simply as a way to keep the ANS application alive, 
 * as that object contains the retry mechanisms for registration. This service
 * will be started, do nothing but force ANSApplication to start, and then
 * be monitored by the system (START_SITCKY return value) and restarted when
 * it 
 */
package com.symbol.rhoconnect.pushservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ANSKeepAliveService extends Service {
	
	private static final String TAG = "ANSKeepAliveService-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	/* package */ static final String ANS_KEEP_ALIVE_ACTION = "com.symbol.rhoconnect.pushservice.action.KEEP_ALIVE";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (D) Log.d(TAG, "&&& onStartCommand() &&&");
		return START_STICKY;
	}
	
	// We will never bind
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**************************************************************************
	 * LIFECYCLE METHODS
	 *************************************************************************/
	@Override
	public void onCreate() {
		
		if (D) Log.d(TAG, "&&& onCreate() &&&");
		
		super.onCreate();
	}
	@Override
	public void onDestroy() {

		if (D) Log.d(TAG, "&&& onDestroy() &&&");
		
		super.onDestroy();
	}
}
