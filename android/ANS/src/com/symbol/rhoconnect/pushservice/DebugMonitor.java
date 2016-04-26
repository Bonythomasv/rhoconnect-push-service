/**
 * 
 */
package com.symbol.rhoconnect.pushservice;


import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class DebugMonitor extends Activity {
	
	private static final String TAG = "DebugMonitor-ANS";
	
	private static final boolean D =  ANSApplication.LOGLEVEL > 0;
	private static final boolean V =  ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	private TextView networkStatusText;
	private TextView instanceIdText;
	private TextView instanceIdRetryText;
	private TextView instanceCookieText;
	private TextView instanceCookieRetryText;
	private TextView regRetryText;
	private TextView numRegisteredText;
	private TextView regQueueText;
	private TextView regsText;
	private TextView regQueueStatusText;
	private TextView fetchStatusText;
	private TextView tokenCheckStatusText;
	
	private ANSApplication thisApplication;
	
	//
	// LIFECYCLE METHODS
	//
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		if (V) Log.d(TAG, "@@@ onCreate @@@");
		
        super.onCreate(savedInstanceState);
        
        ArrayList<String> serverList;
        String            cookieText;
        String            idText;
        
		setContentView(R.layout.debug);
        networkStatusText       = (TextView)findViewById(R.id.statusText);
        instanceIdText          = (TextView)findViewById(R.id.instanceIdText);
        instanceCookieText      = (TextView)findViewById(R.id.instanceCookieText);
        numRegisteredText       = (TextView)findViewById(R.id.numRegisteredText);
        regQueueText            = (TextView)findViewById(R.id.regQueueText);
        regsText                = (TextView)findViewById(R.id.regsText);
        regRetryText            = (TextView)findViewById(R.id.registrationsRetry);
        instanceCookieRetryText = (TextView)findViewById(R.id.instanceCookieRetry);
        instanceIdRetryText     = (TextView)findViewById(R.id.instanceIdRetry);
        regQueueStatusText      = (TextView)findViewById(R.id.regQueueStatus);
        fetchStatusText         = (TextView)findViewById(R.id.fetchStatus);
        tokenCheckStatusText    = (TextView)findViewById(R.id.tokenCheckStatus);

        // registrations and queue should scroll
        regQueueText.setMovementMethod(new ScrollingMovementMethod());
        regsText.setMovementMethod(new ScrollingMovementMethod());
        fetchStatusText.setMovementMethod(new ScrollingMovementMethod());
        instanceIdText.setMovementMethod(new ScrollingMovementMethod());
        instanceCookieText.setMovementMethod(new ScrollingMovementMethod());
		
		thisApplication = (ANSApplication)getApplication();
		
		networkStatusText.setText(thisApplication.isNetworkConnected() ? "connected" : "disconnected");
		
		serverList = thisApplication.getAllRegistered().serverUrls;
		cookieText = "";
		idText = "";
		if (serverList != null) {
			for (String url : serverList) {
				idText     += url + "-" + (("".equals(thisApplication.getInstanceId(url)))     ? "id missing"     : "id present") + "\n";
				cookieText += url + "-" + (("".equals(thisApplication.getInstanceCookie(url))) ? "cookie missing" : "cookie present") + "\n";
			}
		}
		if ("".equals(idText)) {
			idText = "no servers found";
		}
		if ("".equals(cookieText)) {
			cookieText = "no servers found";
		}
		instanceIdText.setText(idText);
		instanceCookieText.setText(cookieText);
		instanceCookieRetryText.setText("");
		instanceIdRetryText.setText("");
		
		Integer numRegistered = new Integer(thisApplication.numRegistered());
		numRegisteredText.setText(numRegistered.toString());	
		regQueueText.setText(thisApplication.mRegistrationQueue.toString());
		regsText.setText(thisApplication.getAllRegistered().regsString);
		regRetryText.setText("");
		regQueueStatusText.setText(thisApplication.mRegistrationQueue.mRegQueueStatus);

		fetchStatusText.setText(thisApplication.mConnectionHandler.getConnectionStatus());
		
		tokenCheckStatusText.setText(thisApplication.mTokenChecker.mCheckStatus + 
				                     ", T=" + thisApplication.mTokenChecker.mTotalToCheck + 
				                     ",N=" + thisApplication.mTokenChecker.mCheckCount +
				                     ",I=" + thisApplication.mTokenChecker.mIncorrectCount +
				                     ",R=" + thisApplication.mTokenChecker.mRetryCount);
	}
}
