package edu.berkeley.security.eventtracker.network;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

public class Synchronizer extends IntentService {

	public Synchronizer() {
		super("Synchronizer");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		
		Bundle bundle=intent.getExtras();
		
		EventEntry event;
		
		event = (EventEntry) bundle.getSerializable("EventData");
		ServerRequest request=(ServerRequest) bundle.getSerializable("Request");
		if(request == ServerRequest.SENDDATA){
			PostRequestResponse response=Networking.sendPostRequest((EventEntry) event, ServerRequest.SENDDATA);
			//do something about sending data again later
		}
		if(request==ServerRequest.REGISTER){
			PostRequestResponse response=Networking.sendPostRequest(null, ServerRequest.REGISTER);
			if(response==PostRequestResponse.Success){
				Settings.confirmRegistrationWithWebServer();
			}
		}
		if(request==ServerRequest.UPDATE){
			PostRequestResponse response=Networking.sendPostRequest((EventEntry) event, ServerRequest.UPDATE);
		}
		if(request==ServerRequest.DELETE){
			PostRequestResponse response=Networking.sendPostRequest(null, ServerRequest.DELETE);
		}
	}
}