/**
 * 
 */
package com.symbol.rhoconnect.pushservice;

import org.json.JSONException;
import org.json.JSONObject;


public class RegQueueElement {
	// Queue element -- contains the reg/unreg information from the Intent
	
	public static final String MESSAGE_TYPE     = "messageType";
	public static final String APP_PACKAGE_NAME = "appPackageName";
	public static final String APP_PERMISSION   = "appPermission";
	public static final String APP_NAME         = "appName";
    public static final String APP_ACTION         = "appAction";
	public static final String USER_NAME        = "userName";
	public static final String USER_PASSWORD    = "userPassword";
	public static final String SERVER_URL       = "serverUrl";
	public static final String APP_COOKIE       = "appCookie";
	
	public static final int    REGISTRATION       = 0;
	public static final int    UNREGISTRATION     = 1;
	public static final int    CHECK_REGISTRATION = 2;
	
	public int     messageType;
	public String  appPackageName;
	public String  appPermission;
	public String  appName;
	public String  appAction;
	public String  userName;
	public String  userPassword;
	public String  serverUrl;
	public String  appSession;
	public RegQueueElement(int     type, 
			               String  pkg, 
			               String  permission, 
			               String  app,
			               String  action,
			               String  user,
			               String  password,
			               String  url,
			               String  session ) {
		messageType = type;
		appPackageName = pkg;
		appPermission = permission;
		appName = app;
		appAction = action;
		userName = user;
		userPassword = password;
		serverUrl = url;
		appSession = session;
	}
	public boolean equivalent(RegQueueElement other) {
		return (   this.messageType == other.messageType
				&& this.appPackageName.equals(other.appPackageName)
				&& this.appPermission.equals(other.appPermission)
				&& this.appName.equals(other.appName)
				&& this.userName.equals(other.userName)
				&& this.serverUrl.equals(other.serverUrl));
	}
	public boolean opposite(RegQueueElement other) {
		return (   this.messageType != other.messageType
				&& this.appPackageName.equals(other.appPackageName)
				&& this.appPermission.equals(other.appPermission)
				&& this.appName.equals(other.appName)
				&& this.userName.equals(other.userName)
				&& this.serverUrl.equals(other.serverUrl));
	}
	@Override
	public String toString() {
		String type = "";
		switch (messageType) {
		case REGISTRATION:
			type = "registration";
			break;
		case UNREGISTRATION:
			type = "unregistration";
			break;
		case CHECK_REGISTRATION:
			type = "check registration";
			break;
		}
		return (  type + " - "
//			    + this.appPackageName + ", " 
//			    + this.appPermission + ", "
			    + this.appName + ", "
			    + this.userName
			    + ", url=" + this.serverUrl
			    + ", session=" + this.appSession
			   );
	}
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put(MESSAGE_TYPE, this.messageType);
		json.put(APP_PACKAGE_NAME, this.appPackageName);
		json.put(APP_PERMISSION, this.appPermission);
		json.put(APP_NAME, this.appName);
		json.put(APP_ACTION, this.appAction);
		json.put(USER_NAME, this.userName);
		json.put(USER_PASSWORD, this.userPassword);
		json.put(SERVER_URL, this.serverUrl);
		return json;
	}
	public static RegQueueElement fromJson(JSONObject json) throws JSONException {
		int     messageType    = json.getInt(MESSAGE_TYPE);
		String  appPackageName = json.getString(APP_PACKAGE_NAME);
		String  appPermission  = json.getString(APP_PERMISSION);
		String  appName        = json.getString(APP_NAME);
		String  appAction      = json.getString(APP_ACTION);
		String  userName       = json.getString(USER_NAME);
		String  userPassword   = json.getString(USER_PASSWORD);
		String  serverUrl      = json.getString(SERVER_URL);
		String  appCookie      = json.getString(APP_COOKIE);
		RegQueueElement element = new RegQueueElement(messageType, 
				                                      appPackageName, 
				                                      appPermission, 
				                                      appName,
				                                      appAction,
				                                      userName,
				                                      userPassword,
				                                      serverUrl,
				                                      appCookie);
		return element;
	}
}
