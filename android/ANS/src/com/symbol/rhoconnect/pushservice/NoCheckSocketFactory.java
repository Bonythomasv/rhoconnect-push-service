/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Log;

/**
 * This socket factory is used when peer checking is turned off. Basically,
 * any certificate will pass checks when this factory is used.
 *
 */
public class NoCheckSocketFactory extends SSLSocketFactory {
	
	private static final String TAG = "NoCheckSocketFactory-ANS";
	private static final boolean D = ANSApplication.LOGLEVEL > 0;
	private static final boolean V = ANSApplication.LOGLEVEL > 1;
	private static final boolean VV = ANSApplication.LOGLEVEL > 2;
	
	private SSLContext sslContext;
	private TrustManager trustManager;
	
	//
	// CONSTRUCTOR
	//
	public NoCheckSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException  {
		
		super(null);
		
		if (D) Log.d(TAG, "!!! NoCheckSocketFactory() - constructor !!!");
		
		sslContext = SSLContext.getInstance("TLS");
		trustManager = new NoCheckTrustManager();
		sslContext.init(null, new TrustManager[] { trustManager }, null);
	}
	

	/* (non-Javadoc)
	 * @see org.apache.http.conn.scheme.SocketFactory#createSocket()
	 */
	@Override
	public Socket createSocket() throws IOException {
		if (D) Log.d(TAG, "!!!!!       - creating no-check SSL socket (no args) !!!!!");
		return sslContext.getSocketFactory().createSocket();
	}

	/* (non-Javadoc)
	 * @see org.apache.http.conn.scheme.LayeredSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
	 */
	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
		if (D) Log.d(TAG, "!!!!!       - creating no-check SSL socket (with args) !!!!!");
		return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	}

}
