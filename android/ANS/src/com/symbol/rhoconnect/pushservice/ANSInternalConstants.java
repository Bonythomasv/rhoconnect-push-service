package com.symbol.rhoconnect.pushservice;

/**
 * This class contains many internal definitions for settings such as 
 * network delays and timeouts, etc. These constants were spread around in 
 * the various classes that used them, but that made it more difficult to 
 * create a consistent policy where all of the relevant constants were 
 * viewable in one place.
 *
 */
public class ANSInternalConstants {
	
	// Instance ID and instance cookie retry constants
	// - starting point for exponentially increasing retry delays
	public  final static int INSTANCE_RETRY_DELAY_MIN    = 8000;   // 8 sec
	// - longest allowable instance retry delay
	public  final static int INSTANCE_RETRY_DELAY_MAX    = 120000; // 2 min
	
	// Reg queue retry constants
	// - timeout for reading the next queue element
	public  static final long QUEUE_PEEK_TIMEOUT         = 300000; // 5 min
	// - delay before next retry if instance ID/cookie missing
	public  static final long QUEUE_COOKIE_WAIT_DELAY    = 30000;  // 1/2 min
	// - timeout for waiting for the network to come back up
	public  static final long DOWN_WAIT_TIMEOUT          = 300000; // 5 min
	
	// Shared network interface constants (also shared with connection)
	// - timeout for initial connection attempt
	public  final static int CONNECT_ATTEMPT_TIMEOUT     = 60000;  // 1 min
	// - timeout for read operation to compete
	// - Since the server will be sending "keep-alive' messages
	//   every 5 minutes, the client timeout must be set so that
	//   there is no timeout before the next "keep-alive message
	//   arrives
	public  final static int CONNECT_READ_TIMEOUT        = 360000; // 6 min
	// - timeout for fetches of instance ID/cookie
	//   and registration requests
	public  final static int FETCH_AND_REGISTER_TIMEOUT  = 120000; // 2 min

	// Connection constants
	// - timeout while waiting for network to come back up
	public  final static int NETWORK_WAIT_TIMEOUT        = 300000; // 5 min
	// - timeout while waiting for attempt flag (apps registered) to be set
	public  final static int ATTEMPT_WAIT_TIMEOUT        = 300000; // 5 min
	// - number of retries before notification is displayed
	public  final static int CONNECT_RETRIES_TO_NOTIFY   = 32;
	// - starting point for exponentially increasing retry delays
	public  final static int CONNECT_RETRY_DELAY_MIN     = 8000;   // 8 sec
	// - longest allowable instance retry delay
	public  final static int CONNECT_RETRY_DELAY_MAX     = 120000; // 2 min

}
