/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

/**
 * This class contains all of the public definitions for constructing Intents
 * for communication with ANS -- ANS -> apps, and apps -> ANS. Definitions for
 * the creation of private Intents issued by ANS components and consumed only
 * by ANS components are defined in ANSService.
 *
 */
public class ANSConstants {
	
	// For incoming Intents (from apps to service)
	public static final String ANS_REGISTER_PERMISSION       = "com.symbol.rhoconnect.pushservice.permission.REGISTER";
	public static final String ANS_REGISTER_ACTION           = "com.symbol.rhoconnect.pushservice.action.REGISTER";
	public static final String ANS_UNREGISTER_ACTION         = "com.symbol.rhoconnect.pushservice.action.UNREGISTER";
	public static final String ANS_CHECK_REGISTRATION_ACTION = "com.symbol.rhoconnect.pushservice.action.CHECK_REGISTRATION";
	
	// For outgoing Intents (to apps)
	// registration result delivery
	public static final String ANS_REG_RESULT_ACTION         = "com.symbol.rhoconnect.pushservice.action.REGISTRATION";
    public static final String ANS_REGISTER_RESPONSE         = "register";
    public static final String ANS_UNREGISTER_RESPONSE       = "unregister";
    public static final String ANS_CHECK_REGISTER_RESPONSE   = "check";
	// message delivery
	public static final String ANS_RECEIVE_PERMISSION        = "com.symbol.rhoconnect.pushservice.permission.RECEIVE";
	public static final String ANS_RECEIVE_ACTION            = "com.symbol.rhoconnect.pushservice.action.RECEIVE";
	// extras
    public static final String ANS_EXTRA_RESPONSE_TYPE       = "response_type";
    public static final String ANS_EXTRA_ERROR               = "error";
    public static final String ANS_EXTRA_TOKEN               = "token";
    public static final String ANS_EXTRA_APP                 = "app";
    public static final String ANS_EXTRA_APP_NAME            = "app_name";
    public static final String ANS_EXTRA_USER_NAME           = "user_name";
    public static final String ANS_EXTRA_USER_PASSWORD       = "user_password";
    public static final String ANS_EXTRA_USER_SESSION        = "user_session";
    public static final String ANS_EXTRA_JSON_PAYLOAD        = "json_payload";
    public static final String ANS_EXTRA_SERVER_URL          = "server_url";
    
    // errors occuring in the client service
    public static final String ANS_DEVICE_ERROR              = "DEVICE_SERVICE_ERROR";  // error in Android service -- see device administrator
    // errors created by bad fields in incoming Intent
    public static final String ANS_BAD_APP_NAME              = "BAD_APP_NAME";          // app name could not be extracted from incoming Intent
    public static final String ANS_BAD_USER_NAME             = "BAD_USER_NAME";         // user name could not be extracted from incoming Intent
    public static final String ANS_BAD_USER_PASSWORD         = "BAD_USER_PASSWORD";     // user password could not be extracted from incoming Intent
    public static final String ANS_BAD_SERVER_URL            = "BAD_SERVER_URL";        // valid URL could not be extracted from incoming Intent
    public static final String ANS_BAD_USER_SESSION          = "BAD_USER_SESSION";		// valid session cookie could nod be extracted from incoming Intent
    // errors created by network transactions
    public static final String ANS_SERVICE_NOT_AVAILABLE     = "SERVICE_NOT_AVAILABLE"; // 500, 503, 400, 403, 404, 405 + NETWORK_ERROR
    public static final String ANS_UNAUTHORIZED              = "UNAUTHORIZED";          // 401
    // errors in server response content
    public static final String ANS_RESPONSE_ERROR            = "RESPONSE_ERROR";        // error between Android service and server or error in server response
    // non-error
    public static final String ANS_REG_NOT_FOUND             = "APP_NOT_REGISTERED";    // when doing a registration check, indicates that the app is unregistered

// Currently unused error codes
//	public static final String ANS_INTERNAL_SERVER_ERROR     = "INTERNAL_SERVER_ERROR"; // 500
//	public static final String ANS_BAD_REQUEST               = "BAD_REQUEST";           // 400
//	public static final String ANS_FORBIDDEN                 = "FORBIDDEN";             // 403
//	public static final String ANS_NOT_FOUND                 = "NOT_FOUND";             // 404
//	public static final String ANS_METHOD_NOT_ALLOWED        = "METHOD_NOT_ALLOWED";    // 405
//  public static final String ANS_NETWORK_ERROR             = "NETWORK_ERROR";         // error between Android service and server

}
