package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"),
	SENDDATA("events/upload"),
	UPDATE("events/upload"), // TODO fix this
	DELETE("events/delete"); // TODO fix this
	private String mUrl;
	
	private static final String SERVER_ROOT = "http://eventtracker.dyndns-at-home.com:3001/";/*"http://192.168.0.106:3000/";*/

	private ServerRequest(String url) {
		this.mUrl = SERVER_ROOT + url;
	}

	public String getURL() {
		return mUrl;
	}
};