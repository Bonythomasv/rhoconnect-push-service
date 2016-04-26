/**
 *
 */
package com.symbol.rhoconnect.pushservice;

import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.symbol.rhoconnect.pushservice.ANSApplication.AllRegsStruct;

public class TokenChecker implements Runnable {

	private static final String TAG = "TokenChecker-ANS";

	private static final boolean D =  ANSApplication.LOGLEVEL > 0;
	private static final boolean V =  ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;

	private ANSApplication mANSApplication;
	private Handler        mANSHandler;

	public  String         mCheckStatus;
	public  int            mTotalToCheck;
	public  int            mCheckCount;
	public  int            mIncorrectCount;
	public  int            mRetryCount;


	/**************************************************************************
	 * CONSTRUCTOR
	 *************************************************************************/
	public TokenChecker(ANSApplication application) {

		if (D) Log.d(TAG, "@@@ TokenChecker() constructor @@@");

		mANSApplication = application;
		mANSHandler = mANSApplication.getANSHandler();
		mCheckStatus = "not checking";
		mTotalToCheck = 0;
		mCheckCount = 0;
		mIncorrectCount = 0;
		mRetryCount = 0;
	}

	// Provide a runnable interface so this process can be launched in a separate thread. When ANS starts up,
	// this check needs to be initiated from onCreate(), but it can't be called directly or onCreate() will
	// block, which causes the system to kill ANS. The remainder of the times this is called, such as when
	// a delayed retry is initiated, the checkInstanceAndTokens() is called directly.
	@Override
	public void run() {
		checkTokensManager();
	}


	/**************************************************************************
	 * CHECKER METHODS
	 *************************************************************************/

	// This is the normal entry point for objects of this class. All other methods are private, so all checking
	// begins here. The first call to this method at startup goes through run() above, because we need a separate
	// checker thread on the first call.
	public void checkTokensManager() {

		Log.i(TAG, "ANS token checking starting up");

		mCheckStatus = "attempting check";
		if (mANSApplication.isNetworkConnected()) {
			checkTokens();
		}
		else {
			if (D) Log.d(TAG, "@@@@@     - network is down, cannot check tokens @@@@@");
			mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
					mANSApplication.getCurrentTokenCheckRetryDelay());
			mRetryCount++;
			return;
		}
	}
	// If any registrations in persistent storage do not return a valid token, remove the registration
	// If any registrations in persistent storage return a different token, send new token to the app
	// If there is a network glitch, return false, else return true
	private boolean checkTokens() {

		HttpGet              httpGet;
		AllRegsStruct        allRegs;
		Iterator<JSONObject> itr;
		JSONObject           currentReg;

		SharedNetworkInterface.NetworkResultStruct  networkResult;

		String     serverUrl;
		String     token;
		String     appName;
        String     appAction;
		String     userName;
		String     userPassword;
		String     appPackageName;
		String     instanceId;
		String     instanceCookie;
		String     credentials;
		String     appCookie;

		HttpEntity response;
		JSONObject serverResObject;
		String     serverToken;

		int numChecked = 0;

		if (D) Log.d(TAG, "@@@ checkTokens() @@@");

		mCheckStatus = "checking";

		allRegs = mANSApplication.getAllRegistered();
		if (allRegs.regObjects == null) {
			// no apps are registered, so just return true.
			Log.i(TAG, "...> No registrations, so not checking");
			mCheckStatus = "check complete";
			return true;
		}
		itr = allRegs.regObjects.iterator();
		mTotalToCheck = allRegs.regObjects.size();

		// This whole loop is being synchronized with each individual registration queue operation, since
		// interleaving token check messages with actual registration/unregistration messages could cause
		// trouble. So, this will wait on any outstanding reg/unreg interaction with the server, and the
		// queue is also being made to wait on this block if the check loop is running.
		synchronized (mANSApplication.mRegistrationStateLock) {

			Log.i(TAG, "...> Registrations found, beginning server checks");

			while (itr.hasNext()) {

				currentReg = itr.next();
				appPackageName = appName = appAction = serverUrl = appCookie = null;
				token = userName = userPassword = null;
				try {
					token = currentReg.getString(ANSApplication.TOKEN);
					appName = currentReg.getString(ANSApplication.APP_NAME);
					appAction = currentReg.getString(ANSApplication.APP_REG_ACTION);
					userName = currentReg.getString(ANSApplication.USER_NAME);
					userPassword = currentReg.getString(ANSApplication.USER_PASSWORD);
					appPackageName = currentReg.getString(ANSApplication.PACKAGE_NAME);
					serverUrl = currentReg.getString(ANSApplication.SERVER_URL);
					appCookie = currentReg.getString(ANSApplication.APP_SESSION);
				} catch (JSONException e) {
					Log.e(TAG, "ERROR - ANS token check - registration exception: " + e.getMessage());
					// => No value for APP_SESSION
					//Log.e(TAG, "ERROR - ANS token check - no token found for a current registration (JSONException)");
					//mANSApplication.displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, "Device failed during token check (reg state)", true);
					// return false;
				}
				if (token == null || appName == null || appAction == null || userName == null || appPackageName == null || serverUrl == null) {
					Log.e(TAG, "ERROR - ANS token check - no token found for a current registration (null)");
					mANSApplication.displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, "Device failed during token check (reg state)", true);
					return false;
				}

				// Get URL and credential components
				instanceId = mANSApplication.getInstanceId(serverUrl);
				if ("".equals(instanceId)) {
					if (D) Log.d(TAG, "@@@@@ WARN  - no Instance ID in persistent storage, cannot check tokens @@@@@");
					mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
		                    mANSApplication.getCurrentTokenCheckRetryDelay());
					mRetryCount++;
					return false;
				}
				instanceCookie = mANSApplication.getInstanceCookie(serverUrl);
				if ("".equals(instanceCookie)) {
					if (D) Log.d(TAG, "@@@@@ WARN  - no Instance cookie found, cannot check tokens @@@@@");
					mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
		                    mANSApplication.getCurrentTokenCheckRetryDelay());
					mRetryCount++;
					return false;
				}

				// Set credentials
				credentials = userName + ":" + userPassword;

				// Send GET and wait for response
				httpGet = new HttpGet(serverUrl + "/registrations" + "/" + instanceId + "/" + userName + "/" + appName);
				if (D) Log.d(TAG, "@@@@@     - Checking token " + token + " @@@@@");

				networkResult = SharedNetworkInterface.performNetworkOperation(mANSApplication.getNewNetworkClient(),
						                                                       httpGet, credentials, instanceCookie, appCookie);

				// Check for exceptions
				switch (networkResult.exceptionCode) {
				case SharedNetworkInterface.BAD_FUNCTION_INPUTS:
				case SharedNetworkInterface.CLIENT_PROTOCOL_EXCEPTION:
					// These are really bad ones
					Log.e(TAG, "ERROR - ANS token check - client failure during token check");
					mANSApplication.displayNotification(ANSApplication.ANS_ERROR_NOTIFICATION_ID, "Device failed during token check (network)", true);
					return false;
				case SharedNetworkInterface.IO_EXCEPTION:
					// This is no big deal, schedule a retry
					mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
							                       mANSApplication.getCurrentTokenCheckRetryDelay());
					mRetryCount++;
					return false;
				case SharedNetworkInterface.NO_EXCEPTION:
					break;
				default:
					break;
				}

				// Check for errors, and also if there is no registration
				int status = networkResult.httpResponse.getStatusLine().getStatusCode();
				if (status != 200) {
					if (D) Log.d(TAG, "@@@@@     - non 200 status returned (" + status + ") @@@@@");
					if (status == 404) {
						// app should not be registered, so do the unregsiter
						mANSApplication.handleUnregistrationResponse(appPackageName, appName, appAction, userName);
						// Notify the connection manager that someone has been unregistered
						// If this was the last registered app/id pair, then the
						// ConnectionHandler object will drop the connection and do a
						// self-stop
						if (D) Log.d(TAG, "@@@@@     - token not on server -- signalling ConnectionHandler that it may need to stop @@@@@");
						mANSApplication.mConnectionHandler.checkConnect(ConnectionHandler.CHECK_DISCONNECT);

						numChecked++;
						mCheckCount = numChecked;
						continue;
					} else if (status == 401) {
						// Unauthorized: on android in result of reboot APP_SESSION lost
						Log.i(TAG, "===> Response code = 401, killing connection and registration, bad cookie from " + serverUrl);
						// Since the cookie is corrupted, the entire registration process must be re-started for this server
						mANSApplication.handleUnregistrationResponse(appPackageName, appName, appAction, userName);
						mANSApplication.handleRegistrationResponse(appPackageName, appName, appAction, userName, userPassword, serverUrl, token);
						numChecked++;
						mCheckCount = numChecked;
						continue;
					}
					else {
						mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
								                       mANSApplication.getCurrentTokenCheckRetryDelay());
						mRetryCount++;
						return false;
					}
				}

				// Extract token from response and compare
				response = networkResult.httpResponse.getEntity();
				if (response != null) {
					try {
						String getResponseString = SharedNetworkInterface.getResponseText(response);
						serverResObject = new JSONObject(getResponseString);
					} catch (Exception e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - exception thrown when creating server response JSONArray @@@@@");
						mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
			                                           mANSApplication.getCurrentTokenCheckRetryDelay());
						mRetryCount++;
						return false;
					}
					try {
						serverToken = serverResObject.getString(ANSApplication.TOKEN);
					} catch (JSONException e) {
						if (D) Log.d(TAG, "@@@@@ ERROR - JSON exception thrown when extracting token string @@@@@");
						mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
			                                           mANSApplication.getCurrentTokenCheckRetryDelay());
						mRetryCount++;
						return false;
					}
					if (serverToken == null) {
						if (D) Log.d(TAG, "@@@@@ ERROR - null token string from server @@@@@");
						mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
			                                           mANSApplication.getCurrentTokenCheckRetryDelay());
						mRetryCount++;
						return false;
					}
				}
				else {
					if (D) Log.d(TAG, "@@@@@ ERROR - Check response is null @@@@@");
					mANSHandler.sendMessageDelayed(mANSHandler.obtainMessage(ANSApplication.TOKEN_CHECK_RETRY),
		                                           mANSApplication.getCurrentTokenCheckRetryDelay());
					mRetryCount++;
					return false;
				}
				if (!token.equals(serverToken)) {
					// token doesn't match, so send the app an unregister, and the follow it with the correct register
					if (D) Log.d(TAG, "@@@@@ WARN - register token mismatch, unregistering local token and registering server token @@@@@");
					mANSApplication.handleUnregistrationResponse(appPackageName, appName, appAction, userName);
					mANSApplication.handleRegistrationResponse(appPackageName, appName, appAction, userName, userPassword, serverUrl, serverToken);
				}

				numChecked++;
				mCheckCount = numChecked;

			} // while loop

		} // synchronized

		Log.i(TAG, "ANS token registration check successful");
		mCheckStatus = "check complete";
		return true;
	}

}
