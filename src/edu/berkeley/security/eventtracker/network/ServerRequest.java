package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"),
	SENDDATA("events/upload"),
	UPDATE("events/upload"), 
	DELETE("events/delete"), 
	POLL("events/poll");
	private String mUrl;
	
	private static final String SERVER_ROOT = "http://10.10.64.49:3001/";
//	private static final String SERVER_ROOT = "http://192.168.110.1:3001/";
//	private static final String SERVER_ROOT = "http://10.10.66.148:3001/";

//	private static final String SERVER_ROOT = "http://eventtracker.dyndns-at-home.com:3001/";

	private ServerRequest(String url) {
		this.mUrl = SERVER_ROOT + url;
	}

	public String getURL() {
		return mUrl;
	}
};