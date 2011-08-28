package edu.berkeley.security.eventtracker.network;

public enum ServerRequest {
	REGISTER("users/init"), SENDDATA("events/upload_bulk"), UPDATE(
			"events/upload_bulk"), DELETE("events/delete"), POLL("events/poll"), CHECKACCOUNT(
			"users/check_phone_number"), VERIFYPASSWORD("users/verify_password");

	private String mUrl;

	private static final String SERVER_ROOT = "192.168.0.100";
	// private static final String SERVER_ROOT = "eventtracker.heroku.com";
	private static final String SERVER_PORT = "";

	private ServerRequest(String url) {
		String port = SERVER_PORT.length() == 0 ? "" : ":" + SERVER_PORT;
		this.mUrl = String.format("http://%s%s/%s", SERVER_ROOT, port, url);
	}

	public String getURL() {
		return mUrl;
	}
}
