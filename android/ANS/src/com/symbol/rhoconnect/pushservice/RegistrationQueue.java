/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.NetworkInfo;
import android.util.Log;


public class RegistrationQueue implements INetworkStatus {

	private static final String TAG = "RegistrationQueue-ANS";
	
	private static final boolean D =  ANSApplication.LOGLEVEL > 0;
	private static final boolean V =  ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	private ANSApplication mANSApplication;
	
	private Thread       mProcessorThread;
	private RegProcessor mProcessor;
	
	public String mRegQueueStatus = "not running";
	
	
	/**************************************************************************
	 * CONSTRUCTOR
	 *************************************************************************/
	public RegistrationQueue(ANSApplication ansApplication) {
		
		if (D) Log.d(TAG, "$$$ RegistrationQueue() constructor $$$");
		
		mANSApplication = ansApplication;
		
		// Setup the queue itself next
		mQueue           = new LinkedBlockingQueue<RegQueueElement>();
		mProcessor       = new RegProcessor();
		mProcessorThread = new Thread(mProcessor);
		synchronized (mUpFlag) {
			mUpFlag.up = ansApplication.isNetworkConnected();
			mUpFlag.notifyAll();
		}
		ansApplication.registerNetworkListener(this);
	}
	
	
	/**************************************************************************
	 * INNER CLASSES ANS ASSOCIATED OBJECTS
	 *************************************************************************/
	
	private class NetworkUpFlag {
		public boolean up;
		public NetworkUpFlag() {
			up = false;
		}
	}
	private NetworkUpFlag mUpFlag  = new NetworkUpFlag();
	
	private class RegQueueWaitFlag {
		public boolean waiting;
		public RegQueueWaitFlag() {
			waiting = false;
		}
	}
	private RegQueueWaitFlag mWaitFlag = new RegQueueWaitFlag();
	
	// Queue
	private LinkedBlockingQueue<RegQueueElement> mQueue;
	private class QueueAccessMutex {
		public boolean timedOut;
		public QueueAccessMutex() {
			timedOut = false;
		}
	}
	private QueueAccessMutex mQueueMutex = new QueueAccessMutex();
	
	//
	// Holds the result of the registration attempt so the queue loop
	// can decide whether or not to inform the app and/or de-queue the
	// request
	//
	private static final int NO_ERROR              = 0;
	private static final int DEVICE_SERVICE_ERROR  = 1; // error in client service
	private static final int NETWORK_ERROR         = 2; // error exists between device and server
	private static final int RESPONSE_ERROR        = 3; // error in server response
	private static final int INSTANCE_COOKIE_ERROR = 4; // instance ID or cookie missing
	private static final int SERVER_CODE_ERROR     = 5; // server error-code response
	private class RegResultStruct {
		public final boolean success;
		public final int reason;
		public RegResultStruct(boolean success, int reason) {
			this.success = success;
			this.reason = reason;
		}
	}
	
	//
	// Runnable for processing registration messages pushed into the
	// queue by the outer RegistrationQueue object
	//
	private class RegProcessor implements Runnable {
		
		// I chose to use notify and wait to handle network down,
		// but I may use this later?
		private boolean mContinueLooping;
		public synchronized void setContinueLooping(boolean flag) {
			mContinueLooping = flag;
		}

		@Override
		public void run() {
			
			if (D) Log.d(TAG, "... run() ...");
			Log.i(TAG, "ANS registration queue starting");

			RegQueueElement fetchedElement;
			RegResultStruct result;
			// Since this is implemented as a blocking queue, the outer loop will be based on a
			// flag that is set via a synchronized method that can be called by 
			// this thread or another.
			setContinueLooping(true);
			while (mContinueLooping) {

				if (mUpFlag.up) {
					mRegQueueStatus = "running";
					try {
						if (D) Log.d(TAG, ".....       - network up, taking next queue element (or blocking until one is available) .....");
						fetchedElement = null;
						while (fetchedElement == null) {
							synchronized (mQueueMutex) {
								fetchedElement = mQueue.peek();
								if (fetchedElement == null) {
									if (D) Log.d(TAG, ".....       - nothing in queue, waiting .....");
									mQueueMutex.wait(ANSInternalConstants.QUEUE_PEEK_TIMEOUT);
									if (D) Log.d(TAG, ".....       - queue fetch wait over (either timed out or fetched) .....");
								}
								mQueueMutex.notifyAll();
							}
							// If the network is down at this point, we should break out of the peek loop
							if (!mUpFlag.up) {
								if (D) Log.d(TAG, ".....       - network down, so breaking out of peek loop .....");
								break;
							}
						}
						// If the fetched queue element is null, then we must have gotten here via a network down event, so 
						// don't bother trying to do the registration, just go continue.
						if (fetchedElement == null) {
							if (D) Log.d(TAG, ".....       - no fetched element after peek, must have been a network change, continuing .....");
							continue;
						}

						if (D) Log.d(TAG, ".....       - extracted element from queue - " + fetchedElement.toString() + " .....");
						
						// synchronizing with the mRegistrationStateLock of ANSApplication so that the code that
						// checks the list of current registrations will operate atomically and not interleave with 
						// any queued register/unregister commands. This block will wait until the token check
						// loop completes before processing the next queue element. Likewise, the token check
						// loop will not do its thing until any in-flight registration queue element processing is 
						// complete.
						synchronized (mANSApplication.mRegistrationStateLock) {
							result = processQueueElement(fetchedElement);
						}

						// REGISTER/UNREGISTER was a fail
						if (!result.success) {
							if (D) Log.d(TAG, "..... ERROR - must remove offending element from reg queue .....");
							synchronized (mQueueMutex) {
								mANSApplication.removePersistedQueueElement(fetchedElement);
								mQueue.remove(fetchedElement);
								mQueueMutex.notifyAll();
							}
						}
						// REGISTER/UNREGISTER was a success
						else {
							// need to make sure no writes to the queue are in process before trying to remove
							// the element from the queue and persistent store.
							if (D) Log.d(TAG, ".....       - REG/UNREG success, removing element from queue and persistent store .....");
							synchronized (mQueueMutex) {
								mANSApplication.removePersistedQueueElement(fetchedElement);
								mQueue.remove(fetchedElement);
								mQueueMutex.notifyAll();
							}
						}
							
					} catch (InterruptedException e) {
						if (D) Log.d(TAG, ".....       - wait for queue element interrupted .....");
						continue;
					}
				}

				// Network down loop, passed over when network is up
				while (!mUpFlag.up) {
					mRegQueueStatus = "paused";
					synchronized (mUpFlag) {
						if (D) Log.d(TAG, ".....       - network down, waiting .....");
						try {
							mUpFlag.wait(ANSInternalConstants.DOWN_WAIT_TIMEOUT);
						} catch (InterruptedException e) {
							if (D) Log.d(TAG, ".....       - mUpFlag wait interrupted .....");
						}
						if (mUpFlag.up) {
							if (D) Log.d(TAG, ".....       - mUpFlag wait notified .....");
						}
						else {
							if (D) Log.d(TAG, ".....       - mUpFlag wait timed out .....");
						}
					}
				}
			}

			if (D) Log.d(TAG, ".....       - leaving RegProcessor thread .....");
		}

	}
	
	
	/**************************************************************************
	 * INTERFACE METHODS (INetworkStatus)
	 *************************************************************************/
	@Override
	public void networkDown() {
		
		if (D) Log.d(TAG, "..... networkDown() .....");
		
		// can be waiting in one of three places -- mUpFlag, mWaitFlag, and queue
		// notify both mUpFlag, mWaitFlag, and queue wait, since we want to stop the queue
		// when this network down event occurs
		synchronized (mUpFlag) {
			mUpFlag.up = false;
			mUpFlag.notifyAll();
		}
		synchronized (mWaitFlag) {
			mWaitFlag.waiting = false;
			mWaitFlag.notifyAll();
		}
		synchronized (mQueueMutex) {
			mQueueMutex.timedOut = false;
			mQueueMutex.notifyAll();
		}
	}
	@Override
	public void networkUp() {
		
		if (D) Log.d(TAG, "..... networkUp() .....");
		
		// can be waiting in one of three places -- mUpFlag, mWaitFlag, and queue
		// notify both mUpFlag and mWaitFlag, but let queue notify itself when element arrives
		synchronized (mUpFlag) {
			mUpFlag.up = true;
			mUpFlag.notifyAll();
		}
		synchronized (mWaitFlag) {
			mWaitFlag.waiting = false;
			mWaitFlag.notifyAll();
		}
	}
	@Override
	public void newNetworkInfo(NetworkInfo info) {
		
	}


	/**************************************************************************
	 * PUBLIC METHODS FOR MANIPULATING QUEUE
	 *************************************************************************/
	public void start() {
		mProcessorThread.start();
	}
	public void stop() {
		
	}
	
	public void insert(RegQueueElement item) {
		
		Log.i(TAG, "ANS registration queue - inserting (" + item.toString() + ")");
		
		// First, collect information on current registration in order to
		// decide how to proceed with queue insertion.
//		boolean isAlreadyRegistered = (mANSApplication.getAppInfo(item.appPackageName, item.appName, item.userName) != null);
		
		// Next, check the request against the current
		// registration status and decide whether to insert the item 
		// 1. a reg req when the app is already registered
//		if (isAlreadyRegistered && item.messageType == RegQueueElement.REGISTRATION) {
//			if (V) Log.d(TAG, "$$$$$       - app is already registered, don't insert, just respond $$$$$");
//			Log.i(TAG, "---> Already registered, immediate response");
//			sendImmediateResponse(item);
//			return;
//		}
//		// 2. an unreg request when the app is unregistered
//		if (!isAlreadyRegistered && item.messageType == RegQueueElement.UNREGISTRATION) {
//			if (V) Log.d(TAG, "$$$$$       - app is already unregistered, send immediate response $$$$$");
//			Log.i(TAG, "---> Already unregistered, immediate response");
//			sendImmediateResponse(item);
//			return;
//		}
		try {
			synchronized (mQueueMutex) {
				if (V) Log.d(TAG, "$$$$$       - persisting item $$$$$");
				mANSApplication.persistQueueElement(item);
				if (V) Log.d(TAG, "$$$$$       - inserting item $$$$$");
				mQueue.put(item);
				mQueueMutex.notifyAll();
			}
		} catch (InterruptedException e1) {
			if (D) Log.d(TAG, "$$$$$ ERROR - delayed put to queue interrupted $$$$$");
		}
	}
	
	
	/**************************************************************************
	 * QUEUE ELEMENT PROCESSING
	 *************************************************************************/
	//
	// When an incoming registration kicks a waiting unregistration out of the queue (or vise versa), send
	// an immediate response back to the app
	//
//	private void sendImmediateResponse(RegQueueElement req) {
//		
//		if (V) Log.d(TAG, "$$$ sendImmediateResponse $$$");
//		
//		String token;
//		if (req.messageType == RegQueueElement.REGISTRATION) {
//			try {
//				JSONObject appInfo = mANSApplication.getAppInfo(req.appPackageName, req.appName, req.userName);
//				if (appInfo != null) {
//					token = appInfo.getString(ANSApplication.TOKEN);
//				}
//				else {
//					Log.e(TAG, "ERROR - ANS registration queue - app response - could not extract token, no entry in persistent store");
//					return;
//				}
//			} catch (JSONException e) {
//				if (D) Log.d(TAG, "$$$$$ ERROR - could not extract token from already registered appInfo $$$$$");
//				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, req.appPackageName, req.appPermission);
//				return;			
//			}
//			mANSApplication.handleRegistrationResponse(req.appPackageName, req.appName, req.userName, req.userPassword, req.serverUrl, token);
//		}
//		else {
//			// Do not need to notify connection of an unregister, as the registration message
//			// was never processed. Flag does not need to be checked for the same reason.
//			if (V) Log.d(TAG, "$$$$$       - sending immediate unregistration response to app $$$$$");
//			mANSApplication.handleUnregistrationResponse(req.appPackageName, req.appName, req.userName);
//		}
//	}
	
	//
	// Process the queue element, sending either a reg or unreg request and dealing with 
	// responses and errors
	//
	public RegResultStruct processQueueElement(RegQueueElement fetchedElement) {
		
		Log.i(TAG, "ANS registration queue - processing queue element (" + fetchedElement.toString() + ")");
		
		String   token;
		String   appName        = fetchedElement.appName;
		String   userName       = fetchedElement.userName;
		String   userPassword   = fetchedElement.userPassword;
		String   serverUrl      = fetchedElement.serverUrl;
		String   appPackageName = fetchedElement.appPackageName;
		String   appPermission  = fetchedElement.appPermission;
		String   appAction      = fetchedElement.appAction;
		String   appCategory    = fetchedElement.appPackageName;
		String   appCookie	= fetchedElement.appSession;
		String   instanceId     = mANSApplication.getInstanceId(serverUrl);
		String   instanceCookie = mANSApplication.getInstanceCookie(serverUrl);
		String   messageCookie;
		String[] intermediateResult;
				
		JSONObject appInfo;
				
		HttpPost            httpPost;
		HttpPut             httpPut;
		HttpDelete          httpDelete;
		HttpGet             httpGet;
		HttpEntity          response;
		
		SharedNetworkInterface.NetworkResultStruct networkResult;

		String      credentials;
		
		JSONObject  responseMessage;
		
		String      errorString;
		
		//
		// INSTANCE ID AND COOKIE
		//
		// If there is no persisted instance ID, then the instance ID and instance
		// cookie fetch routine is executed.
		//
		if ("".equals(instanceId)) {
			//
			// ID
			//
			Log.i(TAG, "---> Fetching instance ID: " + serverUrl);
			// build the connection object
			httpPost = new HttpPost(serverUrl + "/instanceId");
			// Set credentials
			credentials = userName + ":" + userPassword;
			// send the request
			networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
					                                                       httpPost, credentials, null, appCookie);
			
			// Check for exceptions
			switch (networkResult.exceptionCode) {
			case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
			case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
				// These are really bad ones -- will eventually send up a notification
				Log.e(TAG, "ERROR - ANS registration queue - could not execute instance ID fetch message - device error");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
			case SharedNetworkInterface.IO_EXCEPTION:
				// This is no big deal, send an error back to app
				if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
				return new RegResultStruct(false, NETWORK_ERROR);
			case SharedNetworkInterface.NO_EXCEPTION:
				break;
			default:
				break;
			}

			// Check for the correct 200 status
			if (networkResult.httpResponse.getStatusLine().getStatusCode() != 200) {
				if (V) Log.d(TAG, "@@@@@       - non 200 status returned (" + networkResult.httpResponse.getStatusLine().getStatusCode() + ") @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, getErrorString(networkResult.httpResponse), appAction, appCategory, appPermission);
				return new RegResultStruct(false, SERVER_CODE_ERROR);
			}

			// Extract the ID from the post response
			response = networkResult.httpResponse.getEntity();
			if (response != null) {
				try {
					String postResponseString = SharedNetworkInterface.getResponseText(response);
					responseMessage = new JSONObject(postResponseString);
				} catch (Exception e) {
					if (D) Log.d(TAG, "@@@@@ ERROR - exception thrown when creating server response JSONArray @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
				try {
					instanceId = responseMessage.getString(ANSApplication.INSTANCE);
				} catch (JSONException e) {
					if (D) Log.d(TAG, "@@@@@ ERROR - JSON exception thrown when extracting instance ID string @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
			}
			else {
				if (D) Log.d(TAG, "@@@@@ ERROR - Instance ID response is null @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, RESPONSE_ERROR);
			}
			// Store result
			mANSApplication.setInstanceId(instanceId, serverUrl);

			//
			// COOKIE
			//
			Log.i(TAG, "---> Fetching instance cookie: " + serverUrl + ", app = " + appName);
			// build the connection object
			httpGet = new HttpGet(serverUrl + "/instanceId" + "/" + instanceId);
			// Set credentials
			credentials = userName + ":" + userPassword;
			// send the request
			networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
					                                                       httpGet, credentials, null, appCookie);
			
			// Check for exceptions
			switch (networkResult.exceptionCode) {
			case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
			case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
				// These are really bad ones -- will eventually send up a notification
				Log.e(TAG, "ERROR - ANS registration queue - could not execute instance cookie fetch message - device error");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
			case SharedNetworkInterface.IO_EXCEPTION:
				// This is no big deal, send an error back to app
				if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
				return new RegResultStruct(false, NETWORK_ERROR);
			case SharedNetworkInterface.NO_EXCEPTION:
				break;
			default:
				break;
			}

			// Check for the correct 204 status
			int statusCode = networkResult.httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 204) {
				if (V) Log.d(TAG, "@@@@@       - non 204 status returned (" + statusCode + ") @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, getErrorString(networkResult.httpResponse), appAction, appCategory, appPermission);
				// If the code is a 404, then the ID is unrecognized and should be deleted
				if (statusCode == 404) {
					mANSApplication.deleteInstanceId(serverUrl);
				}
				return new RegResultStruct(false, SERVER_CODE_ERROR);
			}

			// Extract the cookie from the post response
			messageCookie = networkResult.httpResponse.getFirstHeader("Set-Cookie").getValue();
			if (messageCookie == null) {
				Log.d(TAG, "@@@@@ WARN  - null Set-Cookie header in GET response @@@@@");
				// missing cookie is unexpected
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, RESPONSE_ERROR);
			}
			intermediateResult = messageCookie.split(";");
			intermediateResult = intermediateResult[0].split("=");
			if (!"instance".equalsIgnoreCase(intermediateResult[0])) {
				Log.d(TAG, "@@@@@ WARN  - cookie name (" + intermediateResult[0] + ") does not equal 'instance' @@@@@");
				// corrupted cookie is unexpected
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, RESPONSE_ERROR);
			}
			instanceCookie = intermediateResult[1];
			// Store result
			mANSApplication.setInstanceCookie(instanceCookie, serverUrl);
		}
		
		//
		// REGISTER
		//
		if (fetchedElement.messageType == RegQueueElement.REGISTRATION) {
			
			if (D) Log.d(TAG, "@@@@@ - REGISTER - @@@@@");
			// check to see if the app has already registered and reply immediately if true
			if (mANSApplication.isRegistered(appPackageName, appName, userName)) {
				if (V) Log.d(TAG, "@@@@@       - application is already registered @@@@@");
				appInfo = mANSApplication.getAppInfo(appPackageName, appName, userName);
				if (appInfo == null) {
					if (D) Log.d(TAG, "@@@@@ ERROR - could not fetch appInfo from persistent store @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
				}
				// FIXME: Commented try block is an original code  
				// try {
				// 	token = appInfo.getString(ANSApplication.TOKEN);
				// } catch (JSONException e) {
				// 	if (D) Log.d(TAG, "@@@@@ ERROR - could not extract token from already registered appInfo @@@@@");
				// 	mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appCategory, appPermission);
				// 	return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
				// }
				// Instead of cached token we fetching it from server ...
				
		        Log.i(TAG, "---> Fetching token of already registered app: " + serverUrl + ", app = " + appName);				
				// build the connection object
				httpGet = new HttpGet(serverUrl + "/registrations/" + instanceId + "/" + userName + "/" + appName);
				// Set credentials
				credentials = userName + ":" + userPassword;
				// send the request
				networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
						                                                       httpGet, credentials, instanceCookie, appCookie);

				// Check for exceptions
				switch (networkResult.exceptionCode) {
				case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
				case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
					// These are really bad ones -- will eventually send up a notification
					Log.e(TAG, "ERROR - ANS registration queue - could not execute check message - device error");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
				case SharedNetworkInterface.IO_EXCEPTION:
					// This is no big deal, but reply anyway. App can decide what to do.
					if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
					return new RegResultStruct(false, NETWORK_ERROR);
				case SharedNetworkInterface.NO_EXCEPTION:
					break;
				default:
					break;
				}

				if (networkResult.httpResponse.getStatusLine().getStatusCode() != 200) {
					if (D) Log.d(TAG, "@@@@@       - non 200 status returned (" + networkResult.httpResponse.getStatusLine().getStatusCode() + ") @@@@@");
					if (networkResult.httpResponse.getStatusLine().getStatusCode() == 404) {
						errorString = ANSConstants.ANS_REG_NOT_FOUND;
					}
					else {
						errorString = getErrorString(networkResult.httpResponse);
					}
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, errorString, appAction, appCategory, appPermission);
					return new RegResultStruct(false, SERVER_CODE_ERROR);
				}

				// Extract the token from the post response
				response = networkResult.httpResponse.getEntity();
				if (response != null) {
					try {
						String getResponseString = SharedNetworkInterface.getResponseText(response);
						responseMessage = new JSONObject(getResponseString);
					} catch (Exception e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - exception thrown when creating server response JSONArray @@@@@");
						mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
						return new RegResultStruct(false, RESPONSE_ERROR);
					}
					try {
						token = responseMessage.getString(ANSApplication.TOKEN);
					} catch (JSONException e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - JSON exception thrown when extracting token string @@@@@");
						mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
						return new RegResultStruct(false, RESPONSE_ERROR);
					}
				}
				else {
					if (D) Log.d(TAG, "@@@@@ ERROR - Check response is null @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
				
				mANSApplication.handleRegistrationResponse(appPackageName, appName, appAction, userName, userPassword, serverUrl, token);
				
			}
			
			// If there is no instance ID or cookie, we can't do the registration request, so just gen an error
			// and return.
			else if ("".equals(instanceId) || "".equals(instanceCookie)) {
				if (D) Log.d(TAG, "@@@@@ WARNING - no instance ID or cookie, can't complete registration at this time @@@@@");
				return new RegResultStruct(false, INSTANCE_COOKIE_ERROR);	
			}
			
			// If app has not already registered, then do the request with the server and
			// perisist the result
			else {
				
				Log.i(TAG, "---> Registering: " + serverUrl + ", app = " + appName);
				// build the connection object
				httpPut = new HttpPut(serverUrl + "/registrations/" + instanceId + "/" + userName + "/" + appName);
				// Set credentials
				credentials = userName + ":" + userPassword;
				// send the request
				networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
						                                                       httpPut, credentials, instanceCookie, appCookie);
				
				// Check for exceptions
				switch (networkResult.exceptionCode) {
				case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
				case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
					// These are really bad ones -- will eventually send up a notification
					Log.e(TAG, "ERROR - ANS registration queue - could not execute registration message - device error");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
				case SharedNetworkInterface.IO_EXCEPTION:
					// This is no big deal, send an error back to app
					if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
					return new RegResultStruct(false, NETWORK_ERROR);
				case SharedNetworkInterface.NO_EXCEPTION:
					break;
				default:
					break;
				}

				// Check for the correct 200 status
				if (networkResult.httpResponse.getStatusLine().getStatusCode() != 201) {
					if (V) Log.d(TAG, "@@@@@       - non 201 status returned (" + networkResult.httpResponse.getStatusLine().getStatusCode() + ") @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, getErrorString(networkResult.httpResponse), appAction, appCategory, appPermission);
					return new RegResultStruct(false, SERVER_CODE_ERROR);
				}

				// Extract the token from the post response
				response = networkResult.httpResponse.getEntity();
				if (response != null) {
					try {
						String postResponseString = SharedNetworkInterface.getResponseText(response);
						responseMessage = new JSONObject(postResponseString);
					} catch (Exception e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - exception thrown when creating server response JSONArray @@@@@");
						mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
						return new RegResultStruct(false, RESPONSE_ERROR);
					}
					try {
						token = responseMessage.getString(ANSApplication.TOKEN);
					} catch (JSONException e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - JSON exception thrown when extracting token string @@@@@");
						mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
						return new RegResultStruct(false, RESPONSE_ERROR);
					}
				}
				else {
					if (D) Log.d(TAG, "@@@@@ ERROR - Register response is null @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
				
				mANSApplication.handleRegistrationResponse(appPackageName, appName, appAction, userName, userPassword, serverUrl, token);

			}
			//
			// NOTIFY THE CONNECTION OF REGISTER
			// 
			// If the connection is already up, then the ConnectionHandler object
			// will do nothing.
			mANSApplication.mConnectionHandler.checkConnect(ConnectionHandler.CHECK_CONNECT);
		}
		//
		// UNREGISTER
		//
		else if (fetchedElement.messageType == RegQueueElement.UNREGISTRATION) {

			if (D) Log.d(TAG, "@@@@@ - UNREGISTER - @@@@@");
			// Set success flag to false for starters
			// Check to see if the app/sender combo is registered, and
			// do the unreg actions only if registration is true
			if (mANSApplication.isRegistered(appPackageName, appName, userName)) {
				if (V) Log.d(TAG, "@@@@@       - found registered application @@@@@");
				
				// Check for instance ID and cookie. If either one is not present, we send an INSTANCE_COOKIE error
				// so the queue loop will put the request back into the queue and do a retry
				if ("".equals(instanceId) || "".equals(instanceCookie)) {
					if (D) Log.d(TAG, "@@@@@ WARNING - no instance ID or cookie, can't complete unregistration at this time @@@@@");
					return new RegResultStruct(false, INSTANCE_COOKIE_ERROR);	
				}

				Log.i(TAG, "---> Unregistering: " + serverUrl + ", app = " + appName);
				// build the connection object
				httpDelete = new HttpDelete(serverUrl + "/registrations/" + instanceId + "/" + userName + "/" + appName);
				// Set credentials in header
				credentials = userName + ":" + userPassword;
				// send the request
				networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
						                                                       httpDelete, credentials, instanceCookie, appCookie);

				// Check for exceptions
				switch (networkResult.exceptionCode) {
				case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
				case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
					// These are really bad ones -- will eventually send up a notification
					Log.e(TAG, "ERROR - ANS regsitration queue - could not execute delete message - device error @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_UNREGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
				case SharedNetworkInterface.IO_EXCEPTION:
					// This is no big deal, schedule a retry
					if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_UNREGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
					return new RegResultStruct(false, NETWORK_ERROR);
				case SharedNetworkInterface.NO_EXCEPTION:
					break;
				default:
					break;
				}
				
				// 404 is a non-error in an unregistration situation
				if (networkResult.httpResponse.getStatusLine().getStatusCode() != 204 &&
					networkResult.httpResponse.getStatusLine().getStatusCode() != 404) {
					if (V) Log.d(TAG, "@@@@@       - non 204 status returned (" + networkResult.httpResponse.getStatusLine().getStatusCode() + ") @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_UNREGISTER_RESPONSE, getErrorString(networkResult.httpResponse), appAction, appCategory, appPermission);
					return new RegResultStruct(false, SERVER_CODE_ERROR);
				}
			}
			else {
				if (V) Log.d(TAG, "@@@@@       - application is already unregistered, sending response @@@@@");
			}
			
			mANSApplication.handleUnregistrationResponse(appPackageName, appName, appAction, userName);
			
			//
			// NOTIFY CONNECTION OF UNREGISTER
			//
			// If this was the last registered app/id pair, then the 
			// ConnectionHandler object will drop the connection and do a
			// self-stop
			mANSApplication.mConnectionHandler.checkConnect(ConnectionHandler.CHECK_DISCONNECT);
		}
		//
		// CHECK REGISTRATION
		//
		else {
			// Check registration is independent of the current reg state, so just send the message
			
			// Check for instance ID and cookie. If either one is not present, we send an INSTANCE_COOKIE error
			// so the queue loop will put the request back into the queue and do a retry
			if ("".equals(instanceId) || "".equals(instanceCookie)) {
				Log.d(TAG, "@@@@@ WARNING - no instance ID or cookie, can't complete unregistration at this time @@@@@");
				return new RegResultStruct(false, INSTANCE_COOKIE_ERROR);	
			}

			// build the connection object
			httpGet = new HttpGet(serverUrl + "/registrations/" + instanceId + "/" + userName + "/" + appName);
			// Set credentials
			credentials = userName + ":" + userPassword;
			// send the request
			networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(), 
					                                                       httpGet, credentials, instanceCookie, appCookie);

			// Check for exceptions
			switch (networkResult.exceptionCode) {
			case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
			case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
				// These are really bad ones -- will eventually send up a notification
				Log.e(TAG, "ERROR - ANS registration queue - could not execute check message - device error");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_DEVICE_ERROR, null, appCategory, appPermission);
				return new RegResultStruct(false, DEVICE_SERVICE_ERROR);
			case SharedNetworkInterface.IO_EXCEPTION:
				// This is no big deal, but reply anyway. App can decide what to do.
				if (D) Log.d(TAG, "@@@@@ WARNING - IOException while executing network access @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_SERVICE_NOT_AVAILABLE, appAction, appCategory, appPermission);
				return new RegResultStruct(false, NETWORK_ERROR);
			case SharedNetworkInterface.NO_EXCEPTION:
				break;
			default:
				break;
			}

			if (networkResult.httpResponse.getStatusLine().getStatusCode() != 200) {
				if (D) Log.d(TAG, "@@@@@       - non 200 status returned (" + networkResult.httpResponse.getStatusLine().getStatusCode() + ") @@@@@");
				if (networkResult.httpResponse.getStatusLine().getStatusCode() == 404) {
					errorString = ANSConstants.ANS_REG_NOT_FOUND;
				}
				else {
					errorString = getErrorString(networkResult.httpResponse);
				}
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, errorString, appAction, appCategory, appPermission);
				return new RegResultStruct(false, SERVER_CODE_ERROR);
			}

			// Extract the token from the post response
			response = networkResult.httpResponse.getEntity();
			if (response != null) {
				try {
					String getResponseString = SharedNetworkInterface.getResponseText(response);
					responseMessage = new JSONObject(getResponseString);
				} catch (Exception e) {
					if (D) Log.d(TAG, "@@@@@ ERROR - exception thrown when creating server response JSONArray @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
				try {
					token = responseMessage.getString(ANSApplication.TOKEN);
				} catch (JSONException e) {
					if (D) Log.d(TAG, "@@@@@ ERROR - JSON exception thrown when extracting token string @@@@@");
					mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
					return new RegResultStruct(false, RESPONSE_ERROR);
				}
			}
			else {
				if (D) Log.d(TAG, "@@@@@ ERROR - Check response is null @@@@@");
				mANSApplication.sendIntentToApp(true, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, ANSConstants.ANS_RESPONSE_ERROR, appAction, appCategory, appPermission);
				return new RegResultStruct(false, RESPONSE_ERROR);
			}

			mANSApplication.sendIntentToApp(false, ANSConstants.ANS_CHECK_REGISTER_RESPONSE, token, appAction, appCategory, appPermission);

		}
		return new RegResultStruct(true, NO_ERROR);
	}

	
	/**************************************************************************
	 * HELPERS
	 *************************************************************************/
	//
	// populate error string for http codes
	//
	private static String getErrorString(HttpResponse response) {
		String errorString;
		switch (response.getStatusLine().getStatusCode()) {
			case 401:
				errorString = ANSConstants.ANS_UNAUTHORIZED;
				break;
			case 404:
				errorString = ANSConstants.ANS_REG_NOT_FOUND;
				break;
			default:
				errorString = ANSConstants.ANS_SERVICE_NOT_AVAILABLE;
				break;
		}
		return errorString;
	}
	
	@Override
	public String toString() {
		
		String queueString = "";
		Iterator<RegQueueElement> queueItr;
		
		if (mQueue.isEmpty()) {
			return "EMPTY";
		}
		else {
			queueItr = mQueue.iterator();
			while (queueItr.hasNext()) {
				queueString += queueItr.next().toString() + "\n";
			}
			return queueString;
		}
	}
}
