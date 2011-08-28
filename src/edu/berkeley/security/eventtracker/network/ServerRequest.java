package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"), SENDDATA("events/upload_bulk"), UPDATE(
			"events/upload_bulk"), DELETE("events/delete"), POLL("events/poll")
			,CHECKACCOUNT("users/check_phone_number"), VERIFYPASSWORD("users/verify_password");

	private String mUrl;

//	private static final String SERVER_ROOT = "192.168.0.108";
	private static final String SERVER_ROOT = "eventtracker.heroku.com";
//	private static final String SERVER_PORT = "3000";

	private ServerRequest(String url) {
//		this.mUrl = "http://" + SERVER_ROOT + ':' + SERVER_PORT + '/' + url;
		this.mUrl = "http://" + SERVER_ROOT + '/' + url;
	}

	public String getURL() {
		return mUrl;
	}
};