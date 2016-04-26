/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ConnectionHandler implements INetworkStatus {
	
	private static final String TAG = "ConnectionHandler-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	private       ANSApplication     mANSApplication;
	
	// For incoming calls to checkConnect (from RegistrationQueue or ANSService)
	public static final int CHECK_DISCONNECT = 0;
	public static final int CHECK_CONNECT    = 1;
	
	private HashMap<String, Thread>     mConnectionThreads = null;
	private HashMap<String, Connection> mConnections = null;	
			
	
	/**************************************************************************
	 * CONNECTION OBJECTS
	 *************************************************************************/
	private class NetworkUpFlag {
		public boolean up;
		public NetworkUpFlag() {
			up = false;
		}
	}
	private NetworkUpFlag mNetworkUpFlag;
	
	
	/**************************************************************************
	 * CONSTRUCTOR
	 *************************************************************************/
	public ConnectionHandler(ANSApplication ansApplication) {
		
		if (D) Log.d(TAG, "!!! ConnectionHandler Constructor !!!");
				
		mANSApplication = ansApplication;
		//
		// Setup connect attempt flags and connect to network events
		mANSApplication.registerNetworkListener(this);
		if (V) Log.d(TAG, "!!!!!     - setting up connection flags !!!!!");
		mNetworkUpFlag    = new NetworkUpFlag();
		mNetworkUpFlag.up = mANSApplication.isNetworkConnected();
				
		// Setup connection containers
		if (V) Log.d(TAG, "!!!!!     - setting up connection objects !!!!!");
		mConnections       = new HashMap<String, Connection>();
		mConnectionThreads = new HashMap<String, Thread>();
	}
	
	
	/**************************************************************************
	 * INTERFACE TO THE REST OF ANS
	 *************************************************************************/
	//
	// Handle the incoming connect check request. Make sure that only one
	// of these is being called at a time by synchronizing it. There should never
	// be multiple simultaneous calls to this method on account of how the rest
	// of the code is structured, but better safe than sorry.
	//
	public synchronized void checkConnect(int checkType) {
		
		if (D) Log.d(TAG, "!!! checkConnect(" + checkType + ") !!!");
		
		ArrayList<String> registrationsUrlList;
		ArrayList<String> connectionsUrlList;

		Connection newConnection;
		Thread     newThread;
		
		registrationsUrlList = mANSApplication.anyRegistered();
		connectionsUrlList = new ArrayList<String>();
		for (String url : mConnections.keySet()) {
			connectionsUrlList.add(url);
		}
		
		if (checkType == CHECK_CONNECT) {
			Log.i(TAG, "ANS Checking registrations to see if any connections need to be created...");
			if (registrationsUrlList.isEmpty()) {
				Log.i(TAG, "===> No registrations, not setting up connections");
				return;
			}
			for (String regUrl : registrationsUrlList) {
				// If the connection exists for a particular URL, do nothing
				if (mConnections.containsKey(regUrl)) {
					Log.i(TAG, "===> Connection already exists for " + regUrl);
					continue;
				}
				// If we are here, then the connection is missing for the current URL
				Log.i(TAG, "===> Setting up connection for " + regUrl);
				newConnection = new Connection(regUrl);
				mConnections.put(regUrl, newConnection);
				newThread = new Thread(newConnection);
				newThread.start();
				mConnectionThreads.put(regUrl, newThread);
			}
		}
		else if (checkType == CHECK_DISCONNECT) {
			Log.i(TAG, "ANS Checking registrations to see if any connections need to be deleted...");
			for (String connUrl : connectionsUrlList) {
				if (!registrationsUrlList.contains(connUrl)) {
					Log.i(TAG, "===> Connection object for " + connUrl + " has no registration match, deleting");
					// Remove any unused connections
					Connection conn = mConnections.get(connUrl);
					if (conn != null) {
						conn.setDead();
					}
					mConnectionThreads.remove(connUrl);
					mConnections.remove(connUrl);
					// Remove any unused nextMessageIds
					mANSApplication.removeLastMessageId(connUrl);
				}
				else {
					Log.i(TAG, "===> Connection matches registration for " + connUrl + ", not deleting");
				}
			}
		}
		else {
			Log.e(TAG, "ERROR - unknown checkType in ConnectionHandler.checkConnect() - " + checkType);
		}
	}
	
	public String getConnectionStatus() {
		
		if (D) Log.d(TAG, "!!! getConnectionStatus() !!!");
		
		String       connectStatusString;
		
		if (mConnections.isEmpty()) {
			connectStatusString = "no connections";
		}
		else {
			connectStatusString = "";
			for (String url : mConnections.keySet()) {
				connectStatusString += mConnections.get(url).getFetchStatus();
				connectStatusString += " - " + url + "\n";
			}
		}
		return connectStatusString;
	}
	
	/**************************************************************************
	 * ANS NETWORK STATUS CALLBACKS
	 *************************************************************************/
	@Override
	public void networkDown() {
		
		if (D) Log.d(TAG, "!!! networkDown() !!!");

		synchronized (mNetworkUpFlag) {
			mNetworkUpFlag.up = false;
			mNetworkUpFlag.notifyAll();
		}
	}
	@Override
	public void networkUp() {
		
		if (D) Log.d(TAG, "!!! networkUp() !!!");
		
		synchronized (mNetworkUpFlag) {
			mNetworkUpFlag.up = true;
			mNetworkUpFlag.notifyAll();
		}
	}
	@Override
	public void newNetworkInfo(NetworkInfo info) {
		
		if (D) Log.d(TAG, "!!! newNetworkInfo() !!!");
	}
	

	/**************************************************************************
	 * CONNECTION CLASS
	 *************************************************************************/
	//
	// Class that maintains the connection to the server and processes messages
	//
	private class Connection implements Runnable {
		
		private HttpGet     httpGet;
		private HttpEntity  response;
		private int         status;
		private String      messageString;
		private JSONObject  messageObject;
		private int         messageProcessingResult;
		private HttpClient  httpClient;
		private String      url;
		
		private boolean     deadFlag;
		private boolean     keepOpen;
		private boolean     retryDelayFlag;
		
		SharedNetworkInterface.NetworkResultStruct networkResult;
		
		private String fetchStatus;
		
		private Long   lastMessageId;
		private String serverUrl;
		
		private String instanceCookie;
		private String instanceId;
		
		private int numberOfConnectRetries;
		private int currentRetryDelay;
		
		private WifiManager.WifiLock wifiLock = null;
		
		public Connection(String serverUrl) {
			// Setup the HttpClient object through which connections will be made
			if (D) Log.d(TAG, "!!!!!     - creating ConnectionHandler HTTP client object !!!!!");
			fetchStatus = "not running";
			httpClient = mANSApplication.getNewNetworkClient();
			this.serverUrl = serverUrl;
			deadFlag = false;
			retryDelayFlag = false;
			keepOpen = true;
		}
		
		public String getServerUrl() {
			return serverUrl;
		}
		public String getFetchStatus() {
			return fetchStatus;
		}
		
		public void setDead() {
			deadFlag = true;
			fetchStatus = "dead";
		}
		
		@Override
		public void run() {
			
			if (D) Log.d(TAG, "^^^ run() ^^^");
			Log.i(TAG, "ANS connection handler started for " + serverUrl);
			
			// if the wifi_lock entry in the manifest file is set to "true", acquire a WiFi lock
			if (mANSApplication.mWiFiLock) {
				if (D) Log.d(TAG, "^^^^^       - WiFi locking enabled in manifest, acquiring lock ^^^^^");
				wifiLock = mANSApplication.mWiFiManager.createWifiLock("ANSWifiLock");
				wifiLock.acquire();
			}

			numberOfConnectRetries = 0;
			currentRetryDelay = ANSInternalConstants.CONNECT_RETRY_DELAY_MIN;
			
			// First, retrieve and check instance cookie and ID. 
			instanceCookie = mANSApplication.getInstanceCookie(serverUrl);
			instanceId     = mANSApplication.getInstanceId(serverUrl);
			if (instanceId == null || "".equals(instanceId) || instanceCookie == null || "".equals(instanceCookie)) {
				Log.w(TAG, "WARNING - instance ID/cookie missing");
				return;
			}

			//
			// MAIN LOOP
			//
			// Connect loop - controls when connections will no longer be attempted
			if (D) Log.d(TAG, "^^^^^       - entering main connect loop ^^^^^");
			while (keepOpen) {
				
				// If we are dead, just return
				if (deadFlag) {
					Log.i(TAG, "===> This connection (" + serverUrl + ") is flagged as dead, returning");
					return;
				}
				
				// If we are not supposed to attempt a connection (no registrations), just return
				if (!mANSApplication.anyRegistered(serverUrl)) {
					Log.i(TAG, "===> This connection (" + serverUrl + ") no longer has any associated registrations, returning");
					return;
				}

				// If the network is down, just wait for it to come back
				synchronized (mNetworkUpFlag) {
					while (!mNetworkUpFlag.up) {
						fetchStatus = "network wait";
						// clear delay so we don't wait again right after this loop
						retryDelayFlag = false;
						Log.i(TAG, "===> Network down - waiting for network to come up (" + serverUrl + ")");
						if (D) Log.d(TAG, "^^^^^       - network up flag not set, waiting ^^^^^");
						try {
							mNetworkUpFlag.wait(ANSInternalConstants.NETWORK_WAIT_TIMEOUT);
						} catch (InterruptedException e) {
							if (D) Log.d(TAG, "^^^^^       - network up flag wait interrupted ^^^^^");
						}
						if (D) Log.d(TAG, "^^^^^       - network up flag wait ended (either timed out or notified) ^^^^^");
						// If we are not supposed to attempt a connection (no registrations), just return
						if (deadFlag) {
							Log.i(TAG, "===> This connection (" + serverUrl + ") is flagged as dead, returning");
							return;
						}
						if (!mANSApplication.anyRegistered(serverUrl)) {
							Log.i(TAG, "===> This connection (" + serverUrl + ") no longer has any associated registrations, returning");
							return;
						}
					}
				}

				// If the network is up...
				if (mNetworkUpFlag.up) {
					
					Log.i(TAG, "ANS initiating nextMessage fetch for " + serverUrl);
					fetchStatus = "fetching";
					
					// First, if this is a retry, sleep for a while (exponential increase on repeated re-entry)
					if (retryDelayFlag) {
						Log.i(TAG, "===> trouble fetching last message - initiating " + currentRetryDelay + "ms delayed retry (" + serverUrl + ")");
						try {
							numberOfConnectRetries++;
							if (D) Log.d(TAG, "^^^^^       - attempt number " + numberOfConnectRetries + " ^^^^^");
							if (numberOfConnectRetries > ANSInternalConstants.CONNECT_RETRIES_TO_NOTIFY) {
								numberOfConnectRetries = 0;
								mANSApplication.displayNotification(ANSApplication.ANS_CONNECT_WARNING_NOTIFICATION_ID, 
										                            "Server failures while connecting", true);
							}
							Thread.sleep(currentRetryDelay);
							currentRetryDelay *= 2;
							if (currentRetryDelay > ANSInternalConstants.CONNECT_RETRY_DELAY_MAX) {
								currentRetryDelay = ANSInternalConstants.CONNECT_RETRY_DELAY_MAX;
							}
						} catch (InterruptedException e1) {
							if (D) Log.d(TAG, "^^^^^       - sleep interrupted ^^^^^");
							currentRetryDelay = ANSInternalConstants.CONNECT_RETRY_DELAY_MIN;
							numberOfConnectRetries = 0;
						}
					}
					else {
						currentRetryDelay = ANSInternalConstants.CONNECT_RETRY_DELAY_MIN;
						numberOfConnectRetries = 0;
					}

					// Second, check to see if there is a valid network connected.
					// Next time the network status changes to "up", the connection will be retried.
					if (!mANSApplication.isNetworkConnected()) {
						if (D) Log.d(TAG, "^^^^^       - No active network, aborting connect attempt ^^^^^");
						synchronized (mNetworkUpFlag) {
							mNetworkUpFlag.up = false;
							mNetworkUpFlag.notifyAll();
						}
						continue; // will block on the mNetworkUpFlag wait
					}

					// Third, do the actual connection
					//
					Log.i(TAG, "===> Establishing connection to " + serverUrl + "...");
					// Setup URL and HTTP transaction object
					lastMessageId = mANSApplication.getLastMessageId(serverUrl);
					if (lastMessageId < 0) {
						url = serverUrl + "/nextMessage/" + instanceId;
					}
					else {
						url = serverUrl + "/nextMessage/" + instanceId + "?lastMessage=" + lastMessageId.toString();
					}
					httpGet = new HttpGet(url);
					//
					// Connect and get response object
					networkResult = SharedNetworkInterface.performNetworkOperation(httpClient, httpGet, null, instanceCookie, null);
					// If we were killed (unregistered) while waiting on network, just return
					if (deadFlag) {
						Log.i(TAG, "===> This connection (" + serverUrl + ") is flagged as dead, returning");
						return;
					}
					if (!mANSApplication.anyRegistered(serverUrl)) {
						Log.i(TAG, "===> This connection (" + serverUrl + ") no longer has any associated registrations, returning");
						return;
					}

					//
					// Check for exceptions
					switch (networkResult.exceptionCode) {
					case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
					case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
						// These are really bad ones -- will eventually send up a notification
						Log.e(TAG, "ERROR - ANS connection service - could not connect to server - device error");
						mANSApplication.displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, 
								"Device failed while connecting", true);
						return; // bail out if the client-side is bad
					case SharedNetworkInterface.IO_EXCEPTION:
						// This is no big deal, schedule a retry
						Log.i(TAG, "===> IOException while getting next message from " + serverUrl);
						retryDelayFlag = true;
						continue; // go back and retry
					case SharedNetworkInterface.NO_EXCEPTION:
						break;
					default:
						break;
					}
					
					// Get status code and response entity
					status = networkResult.httpResponse.getStatusLine().getStatusCode();
					response = networkResult.httpResponse.getEntity();
					
					// Check status
					if (status != 200) {
						if (status == 204) {
							Log.i(TAG, "===> Response code = 204, keep-alive from " + serverUrl);
							Log.i(TAG, "------------------------------------");
							retryDelayFlag = false; // no delayed retry for a keep-alive
						}
						else if (status == -1) {
							Log.i(TAG, "===> Response code = -1, server timed out (" + serverUrl + ")");
							Log.i(TAG, "-----------------------------------------");
							// Since this seems to be the error when the persistent connection naturally times out, I don't want
							// to put a delay between this event and the next reconnect attempt
							retryDelayFlag = false;
						}
						else if (status == 401) {
							Log.i(TAG, "===> Response code = 401, killing connection and registrations, bad cookie from " + serverUrl);
							Log.i(TAG, "------------------------------------");
							// Since the cookie is corrupted, the entire registration process must be re-started for this server
							mANSApplication.deleteAllInstanceData(serverUrl);
							return;
						}
						else if (status == 404) {
							Log.i(TAG, "===> Response code = 404, killing connection and registrations, instance ID not found from " + serverUrl);
							Log.i(TAG, "------------------------------------");
							// Since the ID is not found, the entire registration process must be re-started for this server
							mANSApplication.deleteAllInstanceData(serverUrl);
							mANSApplication.deleteInstanceId(serverUrl);
							return;
						}
						else {
							Log.i(TAG, "===> Response code = " + status + " from " + serverUrl);
							if (response != null) {
								try {
									String errorResponseString = SharedNetworkInterface.getResponseText(response);
								} catch (Exception e) {
									Log.e(TAG, "===> ERROR - exception (" + e.getMessage() + ") while flushing error response (" + serverUrl + ")");
								}
							}
							Log.i(TAG, "--------------------------------");
							// All other errors introduce a delay in the retry
							retryDelayFlag = true;
						}
						continue; // go back and try again
					}
					else {
						if (V) Log.d(TAG, "^^^^^       - got 200 status ^^^^^");
					}
					//
					// Get message
					if (D) Log.d(TAG, "^^^^^       - extracting message from stream ^^^^^");
					if (response != null) {
						try {
							messageString = SharedNetworkInterface.getResponseText(response);
						} catch (Exception e) {
							Log.e(TAG, "===> ERROR - exception (" + e.getMessage() + ") while extracting message (" + serverUrl + ")");
						}
					}
					//
					// Check message
					Log.i(TAG, "===> Response code = " + status + " from " + serverUrl);
					// if real message, check and process
					if (messageString == null || "".equals(messageString)) {
						if (D) Log.d(TAG, "^^^^^ WARN  - empty message body ^^^^^");
						retryDelayFlag = true;
						continue; // go back and try again (after retry delay) -- this could be due to glitches					
					}
					//
					// Process response message (we made it out of the read loop without errors)
					try {
						messageObject = new JSONObject(messageString);
					} catch (JSONException e) {
						if (D) Log.d(TAG, "^^^^^ WARN  - JSONException while converting input string ^^^^^");
						retryDelayFlag = true;
						continue; // stay in main loop and attempt another message retrieval
					}

					// If all has gone well to this point, send the incoming message for processing
					messageProcessingResult = processMessage(messageObject);
					if (messageProcessingResult != NO_ERROR) {
						if (messageProcessingResult == CLIENT_ERROR) {
							Log.e(TAG, "ERROR - client problem processing message");
							mANSApplication.displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, 
									"Device failed while connecting", true);
							return; // bail out if the message can't be processed due to client error
						}
						else {
							if (D) Log.d(TAG, "^^^^^ WARN  - problem processing message ^^^^^");
							retryDelayFlag = true;
						}
						continue; // stay in main loop and attempt another message retrieval
					}

					// If we made it this far, set retry to false and get the next message (back to top of loop)
					Log.i(TAG, "===> message received and processed from " + serverUrl);
					Log.i(TAG, "==================================");
					// If there are any connection warnings, remove them since we have had a success
					mANSApplication.removeNotification(ANSApplication.ANS_CONNECT_WARNING_NOTIFICATION_ID);
					retryDelayFlag = false;
					
					// reset retry counter after each good connect
					numberOfConnectRetries = 0;

				} // if mNetworkUpFlag and mAttemptConnectionFlag

			} // END CONNECT LOOP
			
			if (wifiLock != null) {
				if (D) Log.d(TAG, "^^^^^       - releasing WiFi lock ^^^^^");
				wifiLock.release();
			}
		}
		
		
		/**************************************************************************
		 * INCOMING ANS MESAGE PROCESSING
		 *************************************************************************/
		public static final int NO_ERROR        = 0;
		public static final int BAD_TOKEN_FIELD = 1;
		public static final int BAD_DATA_FIELD  = 2;
		public static final int CLIENT_ERROR    = 3;
		
		public int processMessage(JSONObject messageObject) {

			if (V) Log.d(TAG, "@@@ processMessage() @@@");
			
			long   messageId = -1;
			String token;
			
			JSONObject dataObject;
			Intent     messageIntent;
			JSONObject appInfo;
			String     appPackageName;
			String     appAction;
			String     appCategory;
			String     appPermission;
			
			try {
				messageId = messageObject.getLong(ANSApplication.ID);
			} catch (JSONException e) {
				if (D) Log.d(TAG, "@@@@@ WARN  - bad 'id' field in message @@@@@");
			}
			try {
				token = messageObject.getString(ANSApplication.TOKEN);
			} catch (JSONException e) {
				Log.e(TAG, "ERROR - Bad 'token' field in message");
				return BAD_TOKEN_FIELD;
			}
			try {
				dataObject = messageObject.getJSONObject(ANSApplication.DATA);
			} catch (JSONException e) {
				Log.e(TAG, "ERROR - JSONException while extracting 'data' field from message");
				return BAD_DATA_FIELD;
			}
			
			if (D) Log.d(TAG, "@@@@@     - getting app info from mesage token - " + token + " @@@@@");
			appInfo = mANSApplication.getAppInfo(token);
			if (appInfo == null) {
				if (D) Log.d(TAG, "@@@@@ WARN  - App not found, probably unregistered @@@@@");
				// Fail silently here, no need to cause trouble
				return NO_ERROR;
			}
			
			try {
				appPackageName = appInfo.getString(ANSApplication.PACKAGE_NAME);
				appAction = appInfo.getString(ANSApplication.APP_REG_ACTION);
			} catch (JSONException e) {
				Log.e(TAG, "ERROR - JSONException while extracting app info");
				return CLIENT_ERROR;
			}
			if (D) Log.d(TAG, "@@@@@     - got app info -- " + appPackageName + " @@@@@");
			appCategory   = appPackageName;
			appPermission = appPackageName + ".permission.ANS";
			
			
			if (appAction.compareTo(ANSConstants.ANS_REG_RESULT_ACTION) == 0) {
			    appAction = ANSConstants.ANS_RECEIVE_ACTION;
			} else {
			    Log.e(TAG, "ERROR - Wrong reg action value for application("+appPackageName+"): " + appAction);
			    return CLIENT_ERROR;
			}
			
			// Prepare broadcast Intent that will be sent to the app
			messageIntent = new Intent(appAction);
			messageIntent.putExtra(ANSConstants.ANS_EXTRA_JSON_PAYLOAD, dataObject.toString());
			messageIntent.addCategory(appCategory);
			mANSApplication.sendBroadcast(messageIntent, appPermission);
			
			// persist the successful message ID for the next request
			mANSApplication.setLastMessageId(serverUrl, messageId);
			return NO_ERROR;
		}

	}
	
}
