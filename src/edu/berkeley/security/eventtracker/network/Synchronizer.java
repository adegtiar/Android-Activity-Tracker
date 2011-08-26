package edu.berkeley.security.eventtracker.network;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

public class Synchronizer extends IntentService {
	public static final String REQUEST_EXTRA = "Request";
	public static final String EVENT_LIST_EXTRA = "EventList";
	public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss z");

	private EventManager manager;

	public Synchronizer() {
		super("Synchronizer");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		manager = EventManager.getManager();
		Bundle bundle = intent.getExtras();

		ServerRequest request = (ServerRequest) bundle
				.getSerializable(REQUEST_EXTRA);

		ArrayList<EventEntry> listOfEvents = (ArrayList<EventEntry>) bundle
				.getSerializable(EVENT_LIST_EXTRA);
		if (listOfEvents != null) {
			for (EventEntry thisEvent : listOfEvents) {
				if (thisEvent != null) {
					if (thisEvent.mTag != null) {
						if (thisEvent.mTag.equals("Select a tag")) {
							thisEvent.mTag = "";
						}
					}
				}
			}
		}

		PostRequestResponse response;
		switch (request) {
		case CHECKACCOUNT:
			response = Networking.sendPostRequest(ServerRequest.CHECKACCOUNT);
		
		
			String message = response.getContent();
			if(message.equals("true")){
		      // An account with the provided phone number already exists
			  // The web server believes an account is registered. The phone does not.
			  // This method is called to tell the phone that the device is actually registered.
			  Settings.confirmRegistrationWithWebServer();
			}
			Settings.creatingAcctDialog.dismiss();

			break;
		case SENDDATA:
			response = Networking.sendPostRequest(listOfEvents, request);
			if (response.isSuccess()) {
				manager.updateDatabaseBulk(listOfEvents, true);
			}
			break;
		case REGISTER:
			response = Networking.sendPostRequest(ServerRequest.REGISTER);
			if (response.isSuccess()) {
				Settings.confirmRegistrationWithWebServer();
				Networking.sendAllEvents(this);
			}
			break;
		case UPDATE:
		case DELETE:
			response = Networking.sendPostRequest(listOfEvents, request);
			break;
		case POLL:
			response = Networking.sendPostRequest(listOfEvents, request);
			if (response.isSuccess())
				try {
					parseEventPollResponse(response.getContent());
				} catch (JSONException e) {
					Log.e(EventActivity.LOG_TAG,
							"Could not parse JSON response.", e);
				}
			break;
		}
	}

	/**
	 * Parses a poll response. Updates and creates relevant events and updates
	 * the pollTime.
	 * 
	 * @param jsonResponseString
	 *            the response from the server.
	 * @throws JSONException
	 *             if the response is poorly formatted.
	 */
	void parseEventPollResponse(String jsonResponseString) throws JSONException {
		EventManager manager = EventManager.getManager();
		JSONObject jsonResponse = new JSONObject(jsonResponseString);

		String pollTime = jsonResponse.getString("pollTime");
		JSONArray events = jsonResponse.getJSONArray("events");
		for (int eventIndex = 0; eventIndex < events.length(); eventIndex++) {
			JSONObject eventData = events.getJSONObject(eventIndex);
			String encryptedData = eventData.getString("content");
			String passwd = Settings.getPassword();
			String unencryptedData = null;
			try {
				unencryptedData = GibberishAESCrypto.decrypt(encryptedData,
						Encryption.hashPassword(passwd).toCharArray());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject eventContents = new JSONObject(unencryptedData);

			String uuid = eventData.getString("uuid");
			EventEntry event = manager.findOrCreateByUUID(uuid);
			String updated_at = eventData.getString("updated_at");
			if (!event.persisted || !event.newerThan(updated_at)) {
				event.mName = eventContents.getString("name");
				event.mNotes = eventContents.getString("notes");
				event.mStartTime = eventContents.getLong("startTime");
				event.mEndTime = eventContents.getLong("endTime");
				event.deleted = eventData.getBoolean("deleted");
				event.mTag = eventContents.getString("tag");
				if (event.mTag != null && event.mTag.length() != 0)
					EventActivity.mEventManager.addTag(event.mTag);
				if (event.deleted && !event.persisted)
					break; // trying to create a deleted event!
				manager.updateDatabase(event, true);
			}
		}
		Settings.setPollTime(pollTime);
	}
}