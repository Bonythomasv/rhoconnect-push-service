/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

/**
 * The ANSService class handles the incoming Intents that arrive at the
 * ANS service. This includes registration requests and unregistration
 * requests.
 */
public class ANSService extends IntentService {
	
	private static final String TAG = "ANSService-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	// For starting the service via user login or boot
	public static final String START_SERVICE_ACTION  = "com.symbol.rhoconnect.pushservice.action.START_SERVICE";
	public static final String NETWORK_CHANGE_ACTION = "com.symbol.rhoconnect.pushservice.action.NETWORK_CHANGE";
	
	
	// The compiler complains if I don't have this constructor defined
	public ANSService() {
		super(TAG);
	}
	
	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		
		String  appPackageName;
		String  appPermission;
		String  appCategory;
		String  appName;
		String  userName;
		String  userPassword;
		String  serverUrl;
		String  appCookie;
		
		String  responseType;
		int     messageType;
						
		ANSApplication ansApplication = (ANSApplication)getApplication();
		
		if (D) Log.d(TAG, "@@@ onHandleIntent(" + intent.getAction() + ") - START_SERVICE Intent is explicit @@@");
		
		//
		// REGISTRATION OR UNREGISTRATION INTENT
		//
		if (ANSConstants.ANS_REGISTER_ACTION.equals(intent.getAction()) || 
			ANSConstants.ANS_UNREGISTER_ACTION.equals(intent.getAction()) ||
			ANSConstants.ANS_CHECK_REGISTRATION_ACTION.equals(intent.getAction())) {
			
			// Prep message and response types
			if (ANSConstants.ANS_REGISTER_ACTION.equals(intent.getAction())) {
				messageType  = RegQueueElement.REGISTRATION;
				responseType = ANSConstants.ANS_REGISTER_RESPONSE;
			}
			else if (ANSConstants.ANS_UNREGISTER_ACTION.equals(intent.getAction())) {
				messageType  = RegQueueElement.UNREGISTRATION;
				responseType = ANSConstants.ANS_UNREGISTER_RESPONSE;
			}
			else {
				messageType  = RegQueueElement.CHECK_REGISTRATION;
				responseType = ANSConstants.ANS_CHECK_REGISTER_RESPONSE;
			}

			// Extract the Pending Intent that holds the application info. If this
			// generates an error, just log it as we can't send a reply intent without
			// the info contained in the PendingIntent
			PendingIntent pendingIntent = intent.getParcelableExtra(ANSConstants.ANS_EXTRA_APP);
			if (pendingIntent == null) {
				Log.d(TAG, "@@@@@ ERROR - null PendingIntent in incoming registration Intent @@@@@");
				return;
			}
			else {
				appPackageName = pendingIntent.getTargetPackage();
				if (appPackageName == null || "".equals(appPackageName)) {
					Log.d(TAG, "@@@@@ ERROR - bad package name in incoming registration Intent @@@@@");
					return;
				}
				appPermission = appPackageName + ".permission.ANS";
				appCategory   = appPackageName;
			}
			
			// extract the application name
			appName = intent.getStringExtra(ANSConstants.ANS_EXTRA_APP_NAME);
			if (appName == null || "".equals(appName)) {
				Log.d(TAG, "@@@@@ ERROR - appName null or empty string, sending error response @@@@@");
				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_APP_NAME, ANSConstants.ANS_REG_RESULT_ACTION, appCategory, appPermission);
				return;
			}
			
			// extract the user ID and password
			userName = intent.getStringExtra(ANSConstants.ANS_EXTRA_USER_NAME);
			if (userName == null || "".equals(userName)) {
				Log.d(TAG, "@@@@@ ERROR - userName null or empty string, sending error response @@@@@");
				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_USER_NAME, ANSConstants.ANS_REG_RESULT_ACTION, appCategory, appPermission);
				return;
			}
			userPassword = intent.getStringExtra(ANSConstants.ANS_EXTRA_USER_PASSWORD);
			// Password can be empty
			if (userPassword == null) {
				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_USER_PASSWORD, ANSConstants.ANS_REG_RESULT_ACTION, appCategory, appPermission);
				return;
			}
			
			appCookie = intent.getStringExtra(ANSConstants.ANS_EXTRA_USER_SESSION);
			if ( appCookie == null ) {
				Log.d(TAG, "@@@@@ ERROR - appCookie null, sending error response @@@@@");
				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_USER_SESSION, ANSConstants.ANS_REG_RESULT_ACTION, appCategory, appPermission);
				return;
			}
			
			// extract the server URL
			serverUrl = intent.getStringExtra(ANSConstants.ANS_EXTRA_SERVER_URL);
			if (serverUrl == null || "".equals(serverUrl)) {
				Log.d(TAG, "@@@@@ ERROR - serverUrl null or empty string, sending error response @@@@@");
				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_SERVER_URL, ANSConstants.ANS_REG_RESULT_ACTION, appCategory, appPermission);
				return;
			}
// FIXME: I don't think we need this. The HTTP interface object will complain on our behalf
//			if (serverUrl.length() < 8) {
//				Log.d(TAG, "@@@@@ ERROR - serverUrl too short, sending error response @@@@@");
//				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_SERVER_URL, appCategory, appPermission);
//				return;
//			}
//			if (!(serverUrl.substring(0, 7).equals("http://")) && !(serverUrl.substring(0, 8).equals("https://"))) {
//				Log.d(TAG, "@@@@@ ERROR - serverUrl malformed, sending error response @@@@@");
//				ansApplication.sendIntentToApp(true, responseType, ANSConstants.ANS_BAD_SERVER_URL, appCategory, appPermission);
//				return;
//			}

			//
			// PUT INFORMATION INTO THE REG/UNREG QUEUE
			//
			ansApplication.mRegistrationQueue.insert(new RegQueueElement(messageType, 
					                                                    appPackageName, 
					                                                    appPermission, 
					                                                    appName,
					                                                    ANSConstants.ANS_REG_RESULT_ACTION,
					                                                    userName,
					                                                    userPassword,
					                                                    serverUrl,
					                                                    appCookie)
			                                       );
		} 
		else if (START_SERVICE_ACTION.equals(intent.getAction()) ) {
			
			Log.i(TAG, "ANS Startservice Intent received - user present or boot");
			if (V) Log.d(TAG, "@@@@@ - START SERVICE - @@@@@");
						
			// Check connection with the ConnectionHandler object
// This should be unnecessary, as onCreate() for ANSApplication makes this call
// TODO: Remove this once it checks out
//			ansApplication.mConnectionHandler.checkConnect(ConnectionHandler.CHECK_CONNECT);

		}
		else if (NETWORK_CHANGE_ACTION.equals(intent.getAction()) ) {
			
			Log.i(TAG, "ANS Network Intent received - network status has changed");
			if (V) Log.d(TAG, "@@@@@ - NETWORK CHANGE - @@@@@");
			
			// Tell the Application object that the network has changed, so it can tell its listeners and perform bookkeeping
			ansApplication.onNetworkStatusChange();
		}
		//
		// ERROR
		//
		else {
			Log.e(TAG, "ERROR - incoming intent does not have recognized action set");
			return;
		}
	}
	
	
	/**************************************************************************
	 * LIFECYCLE METHODS
	 *************************************************************************/
	@Override
	public void onCreate() {
		
		super.onCreate();
		if (D) Log.d(TAG, "@@@ onCreate() @@@");
		
	}
	@Override
	public void onDestroy() {

		super.onDestroy();
		if (D) Log.d(TAG, "@@@ onDestroy() @@@");
		
	}
}
