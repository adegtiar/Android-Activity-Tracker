package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"),
	SENDDATA("events/upload"),
	UPDATE("events/upload"), 
	DELETE("events/delete"), 
	POLL("events/poll");
	private String mUrl;
	

	private static final String SERVER_ROOT = "192.168.0.110";
	private static final String SERVER_PORT = "3000";

//	private static final String SERVER_ROOT = "eventtracker.dyndns-at-home.com";
//	private static final String SERVER_PORT = "3001";

	private ServerRequest(String url) {
		this.mUrl = "http://" + SERVER_ROOT + ':' + SERVER_PORT + '/' + url;
	}

	public String getURL() {
		return mUrl;
	}
};