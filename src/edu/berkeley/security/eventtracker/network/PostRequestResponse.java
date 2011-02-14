package edu.berkeley.security.eventtracker.network;

public class PostRequestResponse {
	private boolean isSuccess;
	private String mContent;

	PostRequestResponse(String content, boolean success) {
		isSuccess = success;
		mContent = content;
	}

	public static PostRequestResponse successResponse() {
		return new PostRequestResponse(null, true);
	}

	public static PostRequestResponse errorResponse() {
		return new PostRequestResponse(null, false);
	}

	/**
	 * @return whether or not the request was successful.
	 */
	public boolean isSuccess() {
		return isSuccess;
	}

	/**
	 * @return the content of the response, or null if none was returned;
	 */
	public String getContent() {
		return mContent;
	}

	@Override
	public String toString() {
		return String.format("{%s, %s}", isSuccess ? "SUCCESS" : "ERROR",
				mContent);
	}
}
