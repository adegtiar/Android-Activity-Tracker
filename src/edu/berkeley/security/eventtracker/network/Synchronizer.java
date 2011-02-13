package edu.berkeley.security.eventtracker.network;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

public class Synchronizer extends IntentService {
	public static final String EVENT_DATA_EXTRA = "EventData";
	public static final String REQUEST_EXTRA = "Request";

	private EventManager manager;

	public Synchronizer() {
		super("Synchronizer");
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		manager = EventManager.getManager();
		Bundle bundle = intent.getExtras();

		EventEntry event = (EventEntry) bundle
				.getSerializable(EVENT_DATA_EXTRA);
		ServerRequest request = (ServerRequest) bundle
				.getSerializable(REQUEST_EXTRA);

		PostRequestResponse response;
		switch (request) {
		case SENDDATA:
			response = Networking.sendPostRequest(event, request);
			if (response == PostRequestResponse.Success) {
				event.mReceivedAtServer = true;
				manager.updateDatabase(event);
			}
			break;
		case REGISTER:
			response = Networking.sendPostRequest(ServerRequest.REGISTER);
			if (response == PostRequestResponse.Success)
				Settings.confirmRegistrationWithWebServer();
			break;
		case UPDATE:
		case DELETE:
			response = Networking.sendPostRequest(event, request);
			break;
		}
	}
}