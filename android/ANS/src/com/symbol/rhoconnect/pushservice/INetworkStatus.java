/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import android.net.NetworkInfo;

public interface INetworkStatus {
	
	/**
	 * These methods act as signals for when the network goes up
	 * and when it goes down.
	 */
	public void networkDown();
	public void networkUp();
	
	/**
	 * This method is called when the network information has changed
	 * with the new NetworkInfo object being passed.
	 * 
	 * @param info New network information object
	 */
	public void newNetworkInfo(NetworkInfo info);

}
