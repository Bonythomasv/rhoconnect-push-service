/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


public class ANSApplication extends Application {
	
	// TOKEN is used by both app-info persistence objects and ANS message objects
	public static final String TOKEN            = "token";
	
	// For app-info persistence JSONObject
	public static final String PACKAGE_NAME     = "package_name";
	public static final String APP_NAME         = "app_name";
    public static final String APP_REG_ACTION   = "app_action";
	public static final String USER_NAME        = "user_name";
	public static final String USER_PASSWORD    = "user_password";
	public static final String SERVER_URL       = "server_url";
	public static final String APP_SESSION      = "APP_SESSION";
	
	// For persistence of network state
	private static final String NETWORK_STATE   = "network_state";
	
	// For persistence of Instance ID and cookie
	public  static final String INSTANCE_ID     = "instance_id";
	public  static final String INSTANCE_COOKIE = "instance_cookie";
	public  static final String INSTANCE        = "instance";
	
	// For ANS message JSONObjects
	public static final String APP              = "app";
	public static final String USER             = "user";
	public static final String REGISTRATIONS    = "registrations";
	public static final String DEV              = "dev";
	public static final String TOKENS           = "tokens";
	public static final String ERROR            = "error";
	public static final String ID               = "id";
	public static final String REG_LIST         = "regList";
	public static final String DATA             = "data";
	public static final String CONNECTED        = "connected";
	public static final String MESSAGE          = "message";
	
	// For preferences
	private static final String REG_INFO             = "com.symbol.rhoconnect.pushservice.reginfo";
	private static final String NET_INFO             = "com.symbol.rhoconnect.pushservice.netinfo";
	private static final String QUEUE_INFO           = "com.symbol.rhoconnect.pushservice.queueinfo";
	private static final String INSTANCE_COOKIE_INFO = "com.symbol.rhoconnect.pushservice.instancecookieinfo";
	private static final String INSTANCE_ID_INFO     = "com.symbol.rhoconnect.pushservice.instanceidinfo";
	private static final String LAST_MSG_ID_INFO     = "com.symbol.rhoconnect.pushservice.lastmessageidinfo";
	private SharedPreferences   mRegSharedPreferences;
	private SharedPreferences   mNetSharedPreferences;
	private SharedPreferences   mQueueSharedPreferences;
	private SharedPreferences   mInstanceCookieSharedPreferences;
	private SharedPreferences   mInstanceIdSharedPreferences;
	private SharedPreferences   mLastMessageIdPreferences;
	
	
	// These will be fetched from the manifest
//	public  String  mServerUrl;
	private String  mErrorMessageSuffix;
	private String  mWarningMessageSuffix;
	private String  mInfoMessageSuffix;
	private boolean mErrorVibrate;
	private boolean mWarningVibrate;
	private boolean mInfoVibrate;
	public  boolean mWiFiLock;
	public  boolean mSecureIgnoreHostname;
	public  boolean mSecureIgnoreCertChecks;
	public  boolean mDisableSecurityNotifications;
	public  boolean mDisableWarningNotifications;
	public  boolean mDisableInfoNotifications;
	
	// For reading data from manifest
	private static final String WIFI_LOCK = "wifi_lock";
	private static final String SECURE_IGNORE_HOSTNAME = "secure_ignore_hostname";
	private static final String SECURE_IGNORE_CERT_CHECKS = "secure_ignore_cert_checks";
	private static final String ERROR_SUFFIX = "error_suffix";
	private static final String WARNING_SUFFIX = "warning_suffix";
	private static final String INFO_SUFFIX = "info_suffix";
	private static final String ERROR_VIBRATE = "error_vibrate";
	private static final String WARNING_VIBRATE = "warning_vibrate";
	private static final String INFO_VIBRATE = "info_vibrate";
	private static final String ENABLE_SECURITY_NOTIFICATIONS = "secure_disable_notifications";
	private static final String ENABLE_WARNING_NOTIFICATIONS = "warning_disabled";
	private static final String DISABLE_INFO_NOTIFICATIONS = "info_disabled";

	public  RegistrationQueue mRegistrationQueue;
	public  ConnectionHandler mConnectionHandler;
	public  TokenChecker      mTokenChecker;
	private Thread            mTokenCheckerThread;
	
	public  Object mRegistrationStateLock;
	
	
	// system services
	private ConnectivityManager mConnectivityManager;
	public  WifiManager         mWiFiManager;
	private NotificationManager mNotificationManager;
	private int                 mCurrentErrorNotificationId;
	
	
	/**************************************************************************
	 * LOGGING AND DEBUG SETUP
	 *************************************************************************/
	// For all of ANS, make change here
	// level = 3 -- print everything
	// level = 2 -- print all messages except "very verbose"
	// level = 1 -- skip "verbose" messages
	// level = 0 -- skip all "verbose" and "debug" messages (only print "error" and "warning" messages)
	public static final int LOGLEVEL = 2;
	// For local logging in ANSApplication
	private static final String TAG = "ANSApplication-ANS";
	private static final boolean D =  ANSApplication.LOGLEVEL > 0;
	private static final boolean V =  ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	// Handles retries for token checks
	private int mCurrentTokenCheckRetryDelay;
	public  int getCurrentTokenCheckRetryDelay() {
		return mCurrentTokenCheckRetryDelay;
	}
	public void setCurrentTokenCheckRetryDelay(int delay) {
		mCurrentTokenCheckRetryDelay = delay;
	}
	
	/**************************************************************************
	 * CREATE NETWORK INTERFACE
	 *************************************************************************/
	public HttpClient getNewNetworkClient() {
		
		HttpClient httpClient;
		
		SSLSocketFactory ssf = SSLSocketFactory.getSocketFactory();
		NoCheckSocketFactory ncsf = null;
		if (mSecureIgnoreCertChecks) {
			try {
				ncsf = new NoCheckSocketFactory();
				ncsf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			} catch (Exception e) {
				Log.e(TAG, "ERROR - ANS startup - Cannot create NoCheckSocketFactory object");
				displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, 
						            "could not create no certification check socket factory", 
						            true);
			}
		}
		else if (mSecureIgnoreHostname) {
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		}
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", ((ncsf == null) ? ssf : ncsf), 443));

		HttpParams params = new BasicHttpParams();
		ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		httpClient = new DefaultHttpClient(connManager, params);
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), ANSInternalConstants.FETCH_AND_REGISTER_TIMEOUT);
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), ANSInternalConstants.CONNECT_ATTEMPT_TIMEOUT);
		if (V) Log.d(TAG, "@@@@@          - http client socket timeout     = " + HttpConnectionParams.getSoTimeout(httpClient.getParams()) + " @@@@@");
		if (V) Log.d(TAG, "@@@@@          - http client connection timeout = " + HttpConnectionParams.getConnectionTimeout(httpClient.getParams()) + " @@@@@");
		
		return httpClient;
	}


	/**************************************************************************
	 * HANDLER
	 *************************************************************************/
	// Looper/Handler messages for creating delayed network retries
	public  final static int CONNECT_RETRY         = 1;
	public  final static int TOKEN_CHECK_RETRY     = 2;	
		
	private HandlerThread mHandlerThread;	
	//
	// Handler to handle delayed messages to retry network stuff.
	//
	private class ANSHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (V) Log.d(TAG, "*** mANSHandler.handleMessage(" + msg.what + ") ***"); 
			switch (msg.what) {
			case CONNECT_RETRY:
				if (V) Log.d(TAG, "***** got delayed connect retry message *****");
				break;
			case TOKEN_CHECK_RETRY:
				if (V) Log.d(TAG, "***** got delayed Token Check retry message *****");
				if (D) Log.d(TAG, "*****      - (delay was " + mCurrentTokenCheckRetryDelay/1000 + " seconds) *****");
				mCurrentTokenCheckRetryDelay *= 2;
				if (mCurrentTokenCheckRetryDelay > ANSInternalConstants.INSTANCE_RETRY_DELAY_MAX) {
					mCurrentTokenCheckRetryDelay = ANSInternalConstants.INSTANCE_RETRY_DELAY_MAX;
				}
				mTokenChecker.checkTokensManager();
				break;
			default:
				super.handleMessage(msg);
			}
		}
		public ANSHandler(Looper looper) {
			super(looper);
		}
	}
	private ANSHandler mANSHandler;
	
	public  ANSHandler getANSHandler() {
		return mANSHandler;
	}

	//
	// Thread to handle fetching of instance ID and instance cookie
	//   This needs to be a separate thread, as it was breaking the completion of
	//   onCreate() -- onCreate() was locking up on instance ID/cookie fetch and
	//   not returning.
	//
	private Thread mCheckerThread;
		
	
	/**************************************************************************
	 * INSTANCE ID AND COOKIE
	 *************************************************************************/
	public String getInstanceCookie(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ getInstanceCookie() @@@");
		
		return mInstanceCookieSharedPreferences.getString(serverUrl, "");
	}
	public void setInstanceCookie(String cookie, String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ setInstanceCookie(" + cookie + ") @@@");
		
		synchronized (mInstanceCookieSharedPreferences) {
			Editor editor = mInstanceCookieSharedPreferences.edit();
			editor.putString(serverUrl, cookie);
			editor.commit();
			editor = null;
		}
	}
	public void deleteInstanceCookie(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ clearInstanceCookie() @@@");
		
		synchronized (mInstanceCookieSharedPreferences) {
			Editor editor = mInstanceCookieSharedPreferences.edit();
			editor.remove(serverUrl);
			editor.commit();
			editor = null;
		}
	}
	public String getInstanceId(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ getInstanceId() @@@");
		
		return mInstanceIdSharedPreferences.getString(serverUrl, "");
	}
	public void setInstanceId(String instanceId, String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ setInstanceId(" + instanceId + ") @@@");
		
		synchronized (mInstanceIdSharedPreferences) {
			Editor editor = mInstanceIdSharedPreferences.edit();
			editor.putString(serverUrl, instanceId);
			editor.commit();
			editor = null;
		}
	}
	public void deleteInstanceId(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ deleteInstanceId() @@@");
		
		synchronized (mInstanceIdSharedPreferences) {
			Editor editor = mInstanceIdSharedPreferences.edit();
			editor.remove(serverUrl);
			editor.commit();
			editor = null;
		}
	}
	
	public void deleteAllInstanceData(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ deleteInstanceInfo(" + serverUrl + ") @@@");
		
		AllRegsStruct allRegistered = getAllRegistered();
		
		// Synchronize all this with the registration queue, effectively pausing the
		// queue while we work this out. Once this is done, the queue may continue.
		synchronized (mRegistrationStateLock) {

			// First, unregister all apps that are using this server
			for (JSONObject obj : allRegistered.regObjects) {
				// If this object matches the server URL, then it needs to be unregistered
				try {
					if (serverUrl.equals(obj.get(SERVER_URL))) {
						handleUnregistrationResponse(obj.getString(PACKAGE_NAME), obj.getString(APP_NAME), obj.getString(APP_REG_ACTION), obj.getString(USER_NAME));
					}
				} catch (JSONException e) {
					if (V) Log.d(TAG, "@@@@@     - error extracting app info from JSONObject @@@@@");
				}
			}

			// Second, remove the lastMessage info
			removeLastMessageId(serverUrl);
		
			// Third, remove the cookie. If the ID also has to be deleted, this needs to be
			// handled separately
			deleteInstanceCookie(serverUrl);
		}
	}
	
	public void checkTokens() {
		// checker blocks, so put it in a thread
		if (D) Log.d(TAG, "@@@@@     - starting TokenChecker thread @@@@@");
		mCheckerThread = new Thread(mTokenChecker);
		mCheckerThread.start();
	}
	

	/**************************************************************************
	 * LAST MESSAGE ID
	 *************************************************************************/
	public long getLastMessageId(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ getLastMessageId(" + serverUrl + ") @@@");
		
		return mLastMessageIdPreferences.getLong(serverUrl, -1);
	}
	public void setLastMessageId(String serverUrl, long id) {
		
		if (V) Log.d(TAG, "@@@ getLastMessageId("  + serverUrl + ", " + id + ") @@@");
		
		synchronized (mLastMessageIdPreferences) {
			Editor editor = mLastMessageIdPreferences.edit();
			editor.putLong(serverUrl, id);
			editor.commit();
			editor = null;
		}
	}
	public void removeLastMessageId(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ removeLastMessageId(" + serverUrl + ") @@@");

		synchronized (mLastMessageIdPreferences) {
			Editor editor = mLastMessageIdPreferences.edit();
			editor.remove(serverUrl);
			editor.commit();
			editor = null;
		}
	}
	
	
	/**************************************************************************
	 * NETWORK STATUS
	 *************************************************************************/
	private ArrayList<INetworkStatus> mListenerList;
	
	public boolean isNetworkConnected() {
		
		if (V) Log.d(TAG, "@@@ isNetworkConnected() @@@");
		
		return mNetSharedPreferences.getBoolean(NETWORK_STATE, false);
	}
	private void setIsNetworkConnected(boolean connected) {
		
		if (V) Log.d(TAG, "@@@ setIsNetworkConnected(" + connected + ") @@@");

		synchronized (mNetSharedPreferences) {
			Editor editor = mNetSharedPreferences.edit();
			editor.putBoolean(NETWORK_STATE, connected);
			editor.commit();
			editor = null;
		}
	}
	//
	// handle changes in network status
	//
	// This method is here, as it needs access to the list of registered listeners. So, 
	// the ANSService calls this when a network status change intent arrives.
	public void onNetworkStatusChange() {

		if (V) Log.d(TAG, "/// onNetworkStatusChange() ///");
		
		boolean systemIsConnected;
		boolean ansIsConnected = isNetworkConnected();
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		
		if (info == null) {
			systemIsConnected = false;
		}
		else {
			systemIsConnected = (info.isConnected() && info.isAvailable());
		}
		if (D) Log.d(TAG, "///// - Network status change... /////");
		if (D) Log.d(TAG, "/////     - System isConnected = " + systemIsConnected + " /////");
		if (D) Log.d(TAG, "/////     - ANS isConnected = " + ansIsConnected + " /////");
		Log.i(TAG, "                        - network is " + ((systemIsConnected) ? "up" : "down"));

		if (systemIsConnected == ansIsConnected) {
			if (V) Log.d(TAG, "/////     - no change /////");
			return;
		}
		
		setIsNetworkConnected(systemIsConnected);
		
		if (V) Log.d(TAG, "/////     - processing listeners /////");
		for (INetworkStatus listener : mListenerList) {
			if (VV) Log.d(TAG, "/////          - processing listener /////");
			listener.newNetworkInfo(info);
			if (systemIsConnected) {
				listener.networkUp();
			}
			else {
				listener.networkDown();
			}
		}		
	}
	//
	// Used by network status listeners to register themselves for network status
	// change notifications
	//
	public void registerNetworkListener(INetworkStatus listener) {

		if (V) Log.d(TAG, "@@@ regsiterNetworkListener() @@@");
		
		if (mListenerList.contains(listener)) {
			if (V) Log.d(TAG, "@@@@@     - listener already exists in list, returning @@@@@");
		}
		else {
			if (V) Log.d(TAG, "@@@@@     - adding listener @@@@@");
			mListenerList.add(listener);
		}
	}
	public void unregisterNetworkListener(INetworkStatus listener) {
		
		if (V) Log.d(TAG, "@@@ unregisterNetworkListener() @@@");
		
		if (mListenerList.contains(listener)) {
			if (V) Log.d(TAG, "@@@@@     - found listener, removing @@@@@");
			mListenerList.remove(listener);
		}
		else {
			if (V) Log.d(TAG, "@@@@@     - listener not found, returning @@@@@");
		}
	}

	
	/**************************************************************************
	 * REGISTRATION STATE AND PERSISTENCE METHODS
	 *************************************************************************/
	
	// Class to hold registration information when all current registrations are fetched. It
	// holds the JSONObjects for each registration and a string containing information on all
	// current registrations
	public class AllRegsStruct {
		public String regsString;
		public ArrayList<JSONObject> regObjects;
		public ArrayList<String> serverUrls;
		AllRegsStruct() {};
		AllRegsStruct(String rs, ArrayList<JSONObject> ros, ArrayList<String> urls) {
			regsString = rs;
			regObjects = ros;
			serverUrls = urls;
		}
	}
		
	// check to see if the app/id pair is already registered
	public boolean isRegistered(String appPackageName, String appName, String userName) {
		
		if (V) Log.d(TAG, "@@@ isRegistered(\"" + appPackageName + "\", \"" + appName + "\", \"" + userName + "\") @@@");
		
		String persistenceKey = appPackageName + appName + userName;
		
		if (mRegSharedPreferences.getString(persistenceKey, "").equals("")) {
			return false;
		}
		return true;
	}
	// check to see if the token is already registered
	public boolean isRegistered(String token) {
		
		if (V) Log.d(TAG, "@@@ isRegistered(\"" + token + "\") @@@");
		
		if (mRegSharedPreferences.getString(token, "").equals("")) {
			return false;
		}
		return true;
	}
	// determine whether any app/user pairs are still registered and returns
	// and ArrayList of all servers that still have registrations
	public ArrayList<String> anyRegistered() {
		
		if (V) Log.d(TAG, "@@@ anyRegistered() @@@");
		
		ArrayList<String> serverUrls = new ArrayList<String>();
		HashMap<String, ?> allRegistered;
		String jsonString;
		JSONObject jsonObject;
		String url;
		
		allRegistered = (HashMap<String, ?>)mRegSharedPreferences.getAll();
		if (allRegistered.isEmpty()) {
			return serverUrls;
		}
		else {
			for (Object obj : allRegistered.values()) {
				jsonString = (String)obj;
				try {
					jsonObject = new JSONObject(jsonString);
					url = jsonObject.getString(SERVER_URL);
					if (serverUrls.contains(url)) continue;
					serverUrls.add(url);
				} catch (JSONException e) {
					if (V) Log.d(TAG, "@@@@@     - error creating JSONObject from " + jsonString + " @@@@@");
				}
			}
			return serverUrls;
		}
	}
	// boolean version of above function that operates over a single server URL
	public boolean anyRegistered(String serverUrl) {
		
		if (V) Log.d(TAG, "@@@ anyRegistered (" + serverUrl + ") @@@");
		
		ArrayList<String> serverUrls = anyRegistered();
		return serverUrls.contains(serverUrl);
	}
	// determine how many registrations are remaining
	public int numRegistered() {
				
		if (V) Log.d(TAG, "@@@ numRegistered() @@@");
		
		HashMap<String, ?> allRegistered;
		allRegistered = (HashMap<String, ?>)mRegSharedPreferences.getAll();
		
		// Every registration takes up two entries in prefs, so right-shift to divide by two
		if (V) Log.d(TAG, "@@@ numRegistered() = " + (allRegistered.size()>>1) + " @@@");
		
		return allRegistered.size()>>1;
	}
	// assemble a structure containing all registered apps for printing and/or checking
	public AllRegsStruct getAllRegistered() {
		
		if (V) Log.d(TAG, "@@@ getAllRegistered() @@@");
		
		ArrayList<String> tokenList =  new ArrayList<String>();
		HashMap<String, ?> allRegistered;
		String jsonString;
		JSONObject jsonObject;
		String token;
		String serverUrl;
		AllRegsStruct result = new AllRegsStruct("", new ArrayList<JSONObject>(), new ArrayList<String>());

		allRegistered = (HashMap<String, ?>)mRegSharedPreferences.getAll();
		if (allRegistered.size() == 0) {
			return new AllRegsStruct("NONE", null, null);
		}
		for (Object obj : allRegistered.values()) {
			jsonString = (String)obj;
			try {
				jsonObject = new JSONObject(jsonString);
				token = jsonObject.getString(TOKEN);
				serverUrl = jsonObject.getString(SERVER_URL);
				if (tokenList.contains(token)) continue;
				tokenList.add(token);
				if (!result.serverUrls.contains(serverUrl)) {
					result.serverUrls.add(serverUrl);
				}
				result.regObjects.add(jsonObject);
				result.regsString += "--------------------\n";
				result.regsString += "  token   = " + token + "\n";
				result.regsString += "  user    = " + jsonObject.getString(USER_NAME) + "\n";
				result.regsString += "  app     = " + jsonObject.getString(APP_NAME) + "\n";
				result.regsString += "  package = " + jsonObject.getString(PACKAGE_NAME) + "\n";
				result.regsString += "  server  = " + jsonObject.getString(SERVER_URL) + "\n";
			} catch (JSONException e) {
				if (V) Log.d(TAG, "@@@@@     - error creating JSONObject from " + jsonString + " @@@@@");
				return new AllRegsStruct("ERROR", null, null);
			}
		}
		result.regsString += "--------------------";
		return result;
	}
	
	// store the app info keyed off a String
	public void persistAppInfo(String key, JSONObject appInfo) {
		
		if (V) Log.d(TAG, "@@@ persistAppInfo(\"" + key + "\", appInfo) @@@");

		synchronized (mRegSharedPreferences) {
			Editor editor = mRegSharedPreferences.edit();
			editor.putString(key, appInfo.toString());
			editor.commit();
			editor = null;
		}
	}
	// remove the information for an app from persistent storage, which will cause that app/sender
	// pair to appear unregistered until the next time they are registered.
	public boolean removeAppInfo(String appPackageName, String appName, String userName) {

		if (V) Log.d(TAG, "@@@ removeAppInfo(\"" + appPackageName + "\", \"" + appName + "\", \"" + userName + "\") @@@");
		
		String persistenceKey = appPackageName + appName + userName;
		String token;
		
		try {
			token = new JSONObject(mRegSharedPreferences.getString(persistenceKey, "")).getString(TOKEN);
		} catch (JSONException e) {
			token = null;
			if (V) Log.d(TAG, "@@@@@     - cannot extract token from preferences for '" + persistenceKey + "' - not registered @@@@@");
			return false;
		}
		
		if (token != null) {
			if (V) Log.d(TAG, "@@@@@     - found token '" + token + "', removing @@@@@");
			synchronized (mRegSharedPreferences) {
				Editor editor = mRegSharedPreferences.edit();
				editor.remove(token);
				editor.remove(persistenceKey);
				editor.commit();
				editor = null;
			}
			return true;
		}
		else {
			return false;
		}
	}
	// return the appInfo object for an already registered app - package+id reference
	public JSONObject getAppInfo(String appPackageName, String appName, String userName) {
		
		if (V) Log.d(TAG, "@@@ getAppInfo(\"" + appPackageName + "\", \"" + appName + "\", \"" + userName + "\") @@@");
		
		String persistenceKey = appPackageName + appName + userName;
		
		try {
			String jsonString = mRegSharedPreferences.getString(persistenceKey, "");
			if ("".equals(jsonString) || jsonString == null) {
				if (V) Log.d(TAG, "@@@@@     - jsonString is either empty or null, returning null @@@@@");
				return null;
			}
			return (new JSONObject(jsonString));
		} catch (JSONException e) {
			if (V) Log.d(TAG, "@@@@@     - JSONException thrown when creating the new JSONObject, returning null @@@@@");
			return null;
		}
	}
	// return the appInfo object for an already registered app - token reference
	public JSONObject getAppInfo(String token) {
		
		if (V) Log.d(TAG, "@@@ getAppInfo(\"" + token + "\") @@@");
		
		try {
			String jsonString = mRegSharedPreferences.getString(token, "");
			if ("".equals(jsonString) || jsonString == null) {
				if (V) Log.d(TAG, "@@@@@     - jsonString is either empty or null, returning null @@@@@");
				return null;
			}
			return (new JSONObject(jsonString));
		} catch (JSONException e) {
			if (V) Log.d(TAG, "@@@@@     - JSONException thrown when creating the new JSONObject, returning null @@@@@");
			return null;
		}
	}
	
	
	/**************************************************************************
	 * REGISTRATION QUEUE PERSISTENCE
	 *************************************************************************/
	public void persistQueueElement(RegQueueElement element) {
		
		if (V) Log.d(TAG, "@@@ persistQueueElement(" + element.toString() + ") @@@");
		
		// key = URL elements = instanceId + userName + appName
		String key = element.userName + element.appName;
		if (mQueueSharedPreferences.contains(key)) {
			if (V) Log.d(TAG, "@@@@@     - persistent store already contains entry @@@@@");
		}
		else {
			synchronized (mQueueSharedPreferences) {
				Editor editor = mQueueSharedPreferences.edit();
				try {
					editor.putString(key, element.toJson().toString());
				} catch (JSONException e) {
					Log.e(TAG, "ERROR - ANS registration queue - could not convert element to JSON for persistence");
					return;
				}
				if (V) Log.d(TAG, "@@@@@     - persisted element");
				editor.commit();
				editor = null;
			}
		}
	}
	public void removePersistedQueueElement(RegQueueElement element) {

		if (V) Log.d(TAG, "@@@ removeQueueElement(" + element.toString() + ") @@@");

		// key = URL elements = instanceId + userName + appName
		String key = element.userName + element.appName;
		synchronized (mQueueSharedPreferences) {
			Editor editor = mQueueSharedPreferences.edit();
			if (mQueueSharedPreferences.contains(key)) {
				if (V) Log.d(TAG, "@@@@@     - found element in persistent store, removing @@@@@");
				editor.remove(key);
			}
			else {
				Log.d(TAG, "@@@@@ WARNING - element not found @@@@@");
			}
			editor.commit();
			editor = null;
		}
	}
	public void fillQueueFromStore(RegistrationQueue queue) {
		
		if (V) Log.d(TAG, "@@@ fillQueueFromStore() @@@");

		RegQueueElement queueEntry;
		JSONObject jsonEntry;
		HashMap<String, ?> allPersisted = (HashMap<String, ?>)mQueueSharedPreferences.getAll();
		
		if (!allPersisted.isEmpty()) {
			Object[] allValues = allPersisted.values().toArray();
			for (Object entry : allValues) {
				String stringEntry = (String)entry;
				try {
					jsonEntry = new JSONObject(stringEntry);
					queueEntry = RegQueueElement.fromJson(jsonEntry);
				} catch (JSONException e) {
					Log.e(TAG, "ERROR - ANS registration queue - JSONException thrown while extracting queue from persistent store");
					return;
				}
				queue.insert(queueEntry);
				if (V) Log.d(TAG, "@@@@@     - inserted '" + queueEntry.toString() + "' back into queue @@@@@");
			}
		}
		else {
			if (V) Log.d(TAG, "@@@@@     - persistent store is empty, nothing to retrieve @@@@@");
		}
	}


	/**************************************************************************
	 * PROCESS REGISTRATION AND UNREGISTRATION RESPONSES
	 * @param appAction TODO
	 *************************************************************************/
	// The registration and unregistration handlers are accessed by multiple objects in the 
	// ANS app, so those methods are placed in the ANSApplication object, where all services
	// can see them, and where they will not be killed unless the entire ANS service is killed.
	//
	// handle persisting registration information and sending the Intent to the app
	public void handleRegistrationResponse(String appPackageName, 
			                               String appName, 
			                               String appAction, 
			                               String userName, 
			                               String userPassword, 
			                               String serverUrl, String token) {
		
		if (V) Log.d(TAG, "@@@ handleRegistrationResponse() @@@");
		
		JSONObject appInfo;
		
		String appCategory    = appPackageName;
		String appPermission  = appPackageName + ".permission.ANS";
		String persistenceKey = appPackageName + appName + userName;
				
		// Populate JSON object that will hold the application info and reg info 
		// and persist it, but only if this token has not already been registered
		if (!isRegistered(token)) {
			appInfo = new JSONObject();
			try {
				appInfo.put(TOKEN, token);
				appInfo.put(PACKAGE_NAME, appPackageName);
				appInfo.put(APP_NAME, appName);
				appInfo.put(APP_REG_ACTION, appAction);
				appInfo.put(USER_NAME, userName);
				appInfo.put(USER_PASSWORD, userPassword);
				appInfo.put(SERVER_URL, serverUrl);
			} catch (JSONException e) {
				Log.e(TAG, "ERROR - ANS registration response prep - JSON exception thrown when populating appInfo");
				sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
				return;
			}
			if (VV) Log.d(TAG, "@@@@@     - created JSONObject for appInfo @@@@@");
			try {
				if (VV) Log.d(TAG, appInfo.toString(4));
			} catch (JSONException e) {
				if (V) Log.d(TAG, "@@@@@      - exception thrown while extracting the contents @@@@@");
			}
			// persist the appInfo twice -- once indexed off the token, and once off the apppkg + appname + user
			persistAppInfo(persistenceKey, appInfo);
			persistAppInfo(token, appInfo);
			if (V) Log.d(TAG, "@@@@@     - appInfo persisted, sending response @@@@@");
			
		}
		else {
			if (V) Log.d(TAG, "@@@@@     - " + token + " already listed as registered, sending response anyway @@@@@");
		}
		//
		// Send broadcast intent back to the app whether or not the app has been registered in the past
		//
		sendIntentToApp(false, ANSConstants.ANS_REGISTER_RESPONSE, token, appAction, appCategory, appPermission);
	}
	//
	// handle clearing persisted registration information and informing the app via Intent
	//
	public void handleUnregistrationResponse(String appPackageName, String appName, String appAction, String userName) {
		
		if (V) Log.d(TAG, "@@@ handleUnregistrationResponse() @@@");
		
		String appCategory    = appPackageName;
		String appPermission  = appPackageName + ".permission.ANS";

		if (removeAppInfo(appPackageName, appName, userName)) {
			if (V) Log.d(TAG, "@@@@@     - found and removed registered app, sending response @@@@@");
		}
		else {
			if (V) Log.d(TAG, "@@@@@     - did not find a registered app, sending response anyway @@@@@");
		}
		sendIntentToApp(false, ANSConstants.ANS_UNREGISTER_RESPONSE, null, appAction, appCategory, appPermission);
	}
	//
	// Single point where all Intents are sent to the apps
	//
	public void sendIntentToApp(boolean error, String responseType, String tokenOrError, String appAction, String appCategory, String appPermission) {
		
		if (D) Log.d(TAG, "@@@ sendIntentToApp @@@");
		if (V) Log.d(TAG, "Resp: " + responseType
		                                + ",\npayload: " + tokenOrError
                                        + ",\nappAction: " + appAction
		                                + ",\nappCategory: " + appCategory
		                                + ",\npermission: " + appPermission);
		
		Intent replyIntent;
		
		replyIntent = new Intent(appAction);
		if (error) {
			replyIntent.putExtra(ANSConstants.ANS_EXTRA_ERROR, tokenOrError);
		}
		else {
			if (tokenOrError != null) {
				replyIntent.putExtra(ANSConstants.ANS_EXTRA_TOKEN, tokenOrError);
			}
		}
		replyIntent.putExtra(ANSConstants.ANS_EXTRA_RESPONSE_TYPE, responseType);
		replyIntent.addCategory(appCategory);
		
		if (V) {
		    StringBuffer message = new StringBuffer();
		    message.append(replyIntent.toString()).append('\n');
		    for(String key: replyIntent.getExtras().keySet()) {
		        message.append(key).append(": ").append(replyIntent.getStringExtra(key)).append('\n');
		    }
		    Log.d(TAG, message.toString());
		}
		
		sendBroadcast(replyIntent, appPermission);		
	}
		
	
	/**************************************************************************
	 * NOTIFICATION METHODS
	 *************************************************************************/
	public static final String ANS_ERROR_DIALOG_ACTION        = "com.symbol.rhoconnect.pushservice.action.ERROR_DIALOG";
	public static final String ANS_WARNING_DIALOG_ACTION      = "com.symbol.rhoconnect.pushservice.action.WARNING_DIALOG";
	public static final String ANS_INFO_DIALOG_ACTION         = "com.symbol.rhoconnect.pushservice.action.INFO_DIALOG";
	public static final String ANS_NOTIFICATION_MESSAGE_EXTRA = "message";
	
	public  static final int ANS_ERROR_NOTIFICATION_ID               = 1;
	public  static final int ANS_INFO_NOTIFICATION_ID                = 2;
	public  static final int ANS_CONNECT_WARNING_NOTIFICATION_ID     = 3;
	public  static final int ANS_INSTANCEID_WARNING_NOTIFICATION_ID  = 4;
	public  static final int ANS_COOKIE_WARNING_NOTIFICATION_ID      = 5;
	public  static final int ANS_CERTIFICATE_WARNING_NOTIFICATION_ID = 6;
	public  static final int ANS_HTTPS_WARNING_NOTIFICATION_ID       = 7;
	private static final int ANS_FIRST_ERROR_NOTIFICATION_ID         = 10;
		
	public void removeNotification(int id) {
		mNotificationManager.cancel(id);
	}
	
	public void displayNotification(int type, String message, boolean displaySuffix) {
		
		Notification        notification;
		CharSequence        contentTitle;
		CharSequence        contentText;

		Intent        intent;
		String        intentAction;
		String        packageName;
		PendingIntent pendingIntent;
		
		long    when;
		int     icon;
		int     id = 0;
		String  ticker;
		boolean vibrate = false;

		// The icon, ticker text, and time are needed to create the notification
		switch (type) {
		case ANS_ERROR_NOTIFICATION_ID:
			// Error notifications must be manually cleared, so give each one a new ID
			// so the old one(s) won't be cancelled
			id = mCurrentErrorNotificationId++;
			icon = R.drawable.alert_orange;
			ticker = "ANS Service Error";
			if ("".equals(mErrorMessageSuffix)) displaySuffix = false;
			contentText = "Error - " + message + ((displaySuffix) ? (" - " + mErrorMessageSuffix) : "");
			vibrate = mErrorVibrate;
			intentAction = ANS_ERROR_DIALOG_ACTION;
			break;
		case ANS_CONNECT_WARNING_NOTIFICATION_ID:
		case ANS_INSTANCEID_WARNING_NOTIFICATION_ID:
		case ANS_COOKIE_WARNING_NOTIFICATION_ID:
		case ANS_CERTIFICATE_WARNING_NOTIFICATION_ID:
		case ANS_HTTPS_WARNING_NOTIFICATION_ID:
			// If warnings are disabled, return
			if (mDisableWarningNotifications) return;
			// Warning notifications can be cancelled, so they have the same ID always. Also,
			// any new warning of the same type will auto-cancel the old notification
			id = type;
			icon = R.drawable.alert_alert;
			ticker = "ANS Warning";
			if ("".equals(mWarningMessageSuffix)) displaySuffix = false;
			contentText = "Warning - " + message + ((displaySuffix) ? (" - " + mWarningMessageSuffix) : "");
			vibrate = mWarningVibrate;
			intentAction = ANS_WARNING_DIALOG_ACTION;
			break;
		case ANS_INFO_NOTIFICATION_ID:
			// if info notifications are disabled, return
			if (mDisableInfoNotifications) return;
			id = ANS_INFO_NOTIFICATION_ID;
			icon = R.drawable.alert_info;
			ticker = "ANS Information";
			if ("".equals(mInfoMessageSuffix)) displaySuffix = false;
			contentText = message + ((displaySuffix) ? (" - " +  mInfoMessageSuffix) : "");
			vibrate = mInfoVibrate;
			intentAction = ANS_INFO_DIALOG_ACTION;
			break;
		default:
			icon = R.drawable.icon;
			ticker = "*****";
			contentText = "*****";
			intentAction = ANS_INFO_DIALOG_ACTION;
		}
		when = System.currentTimeMillis();
		notification = new Notification(icon, ticker, when);

		// once the notification object has been created, you need to set the intent
		// and additional text that is displayed in the expanded view
		contentTitle = "Asynchronous Notification Service";

		intent       = new Intent();
		intent.setAction(intentAction);
		packageName = getPackageName();
		intent.setPackage(packageName);
		intent.putExtra(ANS_NOTIFICATION_MESSAGE_EXTRA, contentText);
		pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		notification.setLatestEventInfo(this, contentTitle, contentText, pendingIntent);
		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
		// use the user's defaults (from system settings) for notifications
		notification.defaults = notification.defaults | Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS;
		if (vibrate) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}

		mNotificationManager.notify(id, notification);
	}
	
	
	/**************************************************************************
	 * LIFECYCLE METHODS
	 *************************************************************************/
	@Override
	public void onCreate() {
		
		super.onCreate();
		if (D) Log.d(TAG, "@@@ onCreate() @@@");
		Log.i(TAG, "ANS Starting");
				
		// Exponential backoff setup for token checking
		mCurrentTokenCheckRetryDelay     = ANSInternalConstants.INSTANCE_RETRY_DELAY_MIN;
		
		// Setup persistence objects
		mRegSharedPreferences            = getSharedPreferences(REG_INFO,             Context.MODE_PRIVATE);
		mNetSharedPreferences            = getSharedPreferences(NET_INFO,             Context.MODE_PRIVATE);
		mQueueSharedPreferences          = getSharedPreferences(QUEUE_INFO,           Context.MODE_PRIVATE);
		mInstanceIdSharedPreferences     = getSharedPreferences(INSTANCE_ID_INFO,     Context.MODE_PRIVATE);
		mInstanceCookieSharedPreferences = getSharedPreferences(INSTANCE_COOKIE_INFO, Context.MODE_PRIVATE);
		mLastMessageIdPreferences        = getSharedPreferences(LAST_MSG_ID_INFO,     Context.MODE_PRIVATE);
		
		// List of listeners who want to be informed when the network status changes
		mListenerList = new ArrayList<INetworkStatus>();
		
		// System connectivity info obtained via this object
		mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// System WiFi is managed via this object
		mWiFiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		
		// Post notifications via this object
		mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mCurrentErrorNotificationId = ANS_FIRST_ERROR_NOTIFICATION_ID;
		
		// This object acts as a lock on any routines that will interact with the server
		// and allows only one to operate at a time. This prevents the checkTokens() routine
		// in TokenChecker from corrupting the activities of the registration queue.
		mRegistrationStateLock = new Object();
		
		ApplicationInfo ai;
		// Fetch this stuff from the manifest file
		try {
			ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "ERROR - ANS startup - could not fetch application constants from manifest file");
			displayNotification(ANS_ERROR_NOTIFICATION_ID, "could not fetch application constants from manifest file", true);
			ai = null;
		}
		if (ai != null) {
			mErrorMessageSuffix           = ai.metaData.getString(ERROR_SUFFIX);
			mWarningMessageSuffix         = ai.metaData.getString(WARNING_SUFFIX);
			mInfoMessageSuffix            = ai.metaData.getString(INFO_SUFFIX);
			mErrorVibrate                 = ai.metaData.getBoolean(ERROR_VIBRATE);
			mWarningVibrate               = ai.metaData.getBoolean(WARNING_VIBRATE);
			mInfoVibrate                  = ai.metaData.getBoolean(INFO_VIBRATE);
			mWiFiLock                     = ai.metaData.getBoolean(WIFI_LOCK);
			mSecureIgnoreHostname         = ai.metaData.getBoolean(SECURE_IGNORE_HOSTNAME);
			mSecureIgnoreCertChecks       = ai.metaData.getBoolean(SECURE_IGNORE_CERT_CHECKS);
			mDisableSecurityNotifications = ai.metaData.getBoolean(ENABLE_SECURITY_NOTIFICATIONS);
			mDisableWarningNotifications  = ai.metaData.getBoolean(ENABLE_WARNING_NOTIFICATIONS);
			mDisableInfoNotifications     = ai.metaData.getBoolean(DISABLE_INFO_NOTIFICATIONS);
		}
		else {
			mErrorMessageSuffix = "";
			mWarningMessageSuffix = "";
			mInfoMessageSuffix = "";
			mErrorVibrate = false;
			mWarningVibrate = false;
			mInfoVibrate = false;
			mWiFiLock = false;
			mSecureIgnoreHostname = false;
			mSecureIgnoreCertChecks = false;
			mDisableSecurityNotifications = false;
			mDisableWarningNotifications = false;
			mDisableInfoNotifications = true;
		}
		if (!mDisableSecurityNotifications) {
			if (mSecureIgnoreCertChecks) {
				displayNotification(ANS_CERTIFICATE_WARNING_NOTIFICATION_ID, 
						"Certificate checking has been disabled - confirm that this is correct behavior", 
						false);
			}
			else if (mSecureIgnoreHostname) {
				displayNotification(ANS_CERTIFICATE_WARNING_NOTIFICATION_ID, 
						"Secure hostname checking has been disabled - confirm that this is correct behavior", 
						false);
			}
		}

		// Start keep-alive intent
		Intent keepAliveIntent = new Intent(ANSKeepAliveService.ANS_KEEP_ALIVE_ACTION, null, this, ANSKeepAliveService.class);
		startService(keepAliveIntent);
		
		// Handler is used to post delayed retry messages when the network is up but the server
		// isn't responding
		mHandlerThread = new HandlerThread("ANSHandlerThread");
		mHandlerThread.start();
		mANSHandler = new ANSHandler(mHandlerThread.getLooper());
		
		// Set connected flag
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		if (info == null) {
			if (V) Log.d(TAG, "@@@@@     - null NetworkInfo object @@@@@");
			setIsNetworkConnected(false);
		}
		else {
			setIsNetworkConnected(info.isConnected() && info.isAvailable());
		}
				
		// Create the connection object first so the old connections are setup before any new ones 
		// are needed due to registrations in the queue
		// Create new connection handler object, which will manage the connections to the ANS servers.
		// Also, check to see if any connections should be made.
		mConnectionHandler = new ConnectionHandler(this);
		mConnectionHandler.checkConnect(ConnectionHandler.CHECK_CONNECT);
		
		// Instantiate the registration queue, fill it from persistent storage, and start it up
		mRegistrationQueue = new RegistrationQueue(this);
		fillQueueFromStore(mRegistrationQueue);
		mRegistrationQueue.start();
		
		// Establish objects used to check the instance ID, instance cookie, and current registration tokens
		mTokenChecker       = new TokenChecker(this);
		mTokenCheckerThread = new Thread(mTokenChecker);
		mTokenCheckerThread.start();
				
		if (VV) displayNotification(ANS_INFO_NOTIFICATION_ID, "ANS has (re)started", true);
	}
	@Override
	public void onTerminate() {
		
		super.onTerminate();
		
		if (D) Log.d(TAG, "@@@ onTerminate() @@@");
		mListenerList = null;
		mConnectivityManager = null;
		mRegSharedPreferences = null;
		mNetSharedPreferences = null;
		mQueueSharedPreferences = null;
		mInstanceIdSharedPreferences = null;
		mInstanceCookieSharedPreferences = null;
	}
	
}
