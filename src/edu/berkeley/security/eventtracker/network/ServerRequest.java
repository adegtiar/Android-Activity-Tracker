package edu.berkeley.security.eventtracker.network;


public enum ServerRequest {
	REGISTER("http://eventtracker.dyndns-at-home.com:3001/users/init"), 
	SENDDATA("http://eventtracker.dyndns-at-home.com:3001/events/upload"), 
	UPDATE("http://eventtracker.dyndns-at-home.com:3001/events/upload"), //TODO fix this
	DELETE("http://eventtracker.dyndns-at-home.com:3001/events/delete"); //TODO fix this
	private String mUrl;

	private ServerRequest(String url) {
		this.mUrl = url;
	}

	public String getURL() {
		return mUrl;
	}
};