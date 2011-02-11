package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"),
	SENDDATA("events/upload"),
	UPDATE("events/upload"), // TODO fix this
	DELETE("events/delete"); // TODO fix this
	private String mUrl;
	private static final String SERVER_ROOT = "http://10.10.64.40:3001/";

	private ServerRequest(String url) {
		this.mUrl = SERVER_ROOT + url;
	}

	public String getURL() {
		return mUrl;
	}
};