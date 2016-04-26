/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import android.util.Base64;
import android.util.Log;

public class SharedNetworkInterface {
	
	private static final String TAG = "SharedNetworkInterface-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	/**************************************************************************
	 * SHARED NETWORK INTERFACE
	 *************************************************************************/
	//
	// Struct that holds the response from a network fetch operation
	//   httpResponse - contains the response
	//   exceptionCode - contains the error reason
	//
	public static final int NO_EXCEPTION = 0;
	public static final int CLIENT_PROTOCOL_EXCEPTION = 1;
	public static final int IO_EXCEPTION = 2;
	public static final int BAD_FUNCTION_INPUTS = 3;
	public static class NetworkResultStruct {
		public HttpResponse httpResponse;
		public int          exceptionCode;
		NetworkResultStruct(HttpResponse response, int error) {
			httpResponse = response;
			exceptionCode = error;
		}
		NetworkResultStruct() {
			httpResponse = null;
			exceptionCode = NO_EXCEPTION;
		}
	}
	public static NetworkResultStruct performNetworkOperation(HttpClient clientObject,
			                                                  HttpRequestBase requestObject, 
			                                                  String credentials, 
			                                                  String instanceCookie,
			                                                  String appCookie) {

		if (D) Log.d(TAG, "@@@ performNetworkOperation() @@@");
		if (D) Log.d(TAG, "@@@@@       - URL: " + requestObject.getURI().toString() + " @@@@@");

		HttpResponse httpResponse = null;
		
		String base64Credentials;
		
		if (requestObject == null || clientObject == null) {
			if (D) Log.d(TAG, "@@@@@ ERROR - one or more inputs null @@@@@");
			return new NetworkResultStruct(null, BAD_FUNCTION_INPUTS);
		}
		
		// Set keep-alive
		requestObject.setHeader("Connection", "keep-alive");
		
		// Set credentials if they exist
		if (credentials != null) {
			base64Credentials = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT);
			base64Credentials = base64Credentials.trim();
			requestObject.setHeader("Authorization", "Basic " + base64Credentials);
		}
		
		// Set cookie if it exists
		if (instanceCookie != null) {
			requestObject.addHeader("Cookie", "instance=" + instanceCookie);
		}
		
		// Set app cookie if it exists
		if ( appCookie != null ) {
			requestObject.addHeader("Cookie", appCookie);
		}
		
		// Send the message
		if ("HttpGet".equals(requestObject.getClass().getSimpleName())) {
			if (V) Log.d(TAG, "@@@@@       - GET @@@@@");
			try {
				httpResponse = clientObject.execute((HttpGet)requestObject);
			} catch (ClientProtocolException e) {
				return new NetworkResultStruct(null, CLIENT_PROTOCOL_EXCEPTION);
			} catch (IOException e) {
				if (D) Log.d(TAG, "@@@@@ WARNING - IO Exception @@@@@");
				if (D) Log.d(TAG, "@@@@@ " + e.toString() + " @@@@@");
				return new NetworkResultStruct(null, IO_EXCEPTION);
			}
		}
		else if ("HttpPut".equals(requestObject.getClass().getSimpleName())) {
			if (V) Log.d(TAG, "@@@@@       - PUT @@@@@");
			try {
				httpResponse = clientObject.execute((HttpPut)requestObject);
			} catch (ClientProtocolException e) {
				return new NetworkResultStruct(null, CLIENT_PROTOCOL_EXCEPTION);
			} catch (IOException e) {
				if (D) Log.d(TAG, "@@@@@ WARNING - IO Exception @@@@@");
				if (D) Log.d(TAG, "@@@@@ " + e.toString() + " @@@@@");
				return new NetworkResultStruct(null, IO_EXCEPTION);
			}
		}
		else if ("HttpPost".equals(requestObject.getClass().getSimpleName())) {
			if (V) Log.d(TAG, "@@@@@       - POST @@@@@");
			try {
				httpResponse = clientObject.execute((HttpPost)requestObject);
			} catch (ClientProtocolException e) {
				return new NetworkResultStruct(null, CLIENT_PROTOCOL_EXCEPTION);
			} catch (IOException e) {
				if (D) Log.d(TAG, "@@@@@ WARNING - IO Exception @@@@@");
				if (D) Log.d(TAG, "@@@@@ " + e.toString() + " @@@@@");
				return new NetworkResultStruct(null, IO_EXCEPTION);
			}
		}
		else if ("HttpDelete".equals(requestObject.getClass().getSimpleName())) {
			if (V) Log.d(TAG, "@@@@@       - DELETE @@@@@");
			try {
				httpResponse = clientObject.execute((HttpDelete)requestObject);
			} catch (ClientProtocolException e) {
				return new NetworkResultStruct(null, CLIENT_PROTOCOL_EXCEPTION);
			} catch (IOException e) {
				if (D) Log.d(TAG, "@@@@@ WARNING - IO Exception @@@@@");
				if (D) Log.d(TAG, "@@@@@ " + e.toString() + " @@@@@");
				return new NetworkResultStruct(null, IO_EXCEPTION);
			}
		}
		else {
			if (D) Log.d(TAG, "@@@@@ ERROR - request type " + requestObject.getClass().getSimpleName() + " not currently supported @@@@@");
			return new NetworkResultStruct(null, BAD_FUNCTION_INPUTS);
		}
		// If we made it here, there were no exceptions
		if (D)  Log.d(TAG, "@@@@@       - network operation success @@@@@");
		return new NetworkResultStruct(httpResponse, NO_EXCEPTION);
	}
	
	/**************************************************************************
	 * NETWORK HELPER METHODS
	 *************************************************************************/
	//
	// extract response text from incoming server responses
	//
	public static String getResponseText(HttpEntity response) throws IllegalStateException, IOException {
		
		if (V) Log.d(TAG, "@@@ getResponseText() @@@");
		
		InputStream iStream = response.getContent();
		Reader iStreamReader = new InputStreamReader(iStream, "UTF-8");
		StringBuilder buffer = new StringBuilder();
		
		char[] tmp = new char[1024];
		int len;
		
		while ((len = iStreamReader.read(tmp)) != -1) {
			buffer.append(tmp, 0, len);
		}
				
		return buffer.toString();
	}
	

}
