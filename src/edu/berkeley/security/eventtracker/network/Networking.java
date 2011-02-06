package edu.berkeley.security.eventtracker.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.content.Context;
import android.content.Intent;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventDataSerializer;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * This is the networking class. We send your packets
 */

enum PostRequestResponse {
	Success, Error
}

public class Networking {
	
	public static void sendAllEvents(Context context){
		if (Settings.isSychronizationEnabled()) {
			 EventCursor theCursor=EventActivity.mEventManager.fetchPhoneOnlyEvents();
			 //send them all! LEAVE NO EVENT BEHIND
				EventEntry nextEvent;
				while (theCursor.moveToNext()) {
					nextEvent = theCursor.getEvent();
					if(nextEvent != null){
						Networking.sendToServer(ServerRequest.SENDDATA, nextEvent, context);
				}
			}
		}
	}
	
	
	/**
	 * Sends intents to a service that sends the actual post requests
	 * Only does this if permission to sychronize with web server is given
	 * @param request
	 *            - the type of post request to send(i.e., register, send data,
	 *            update some event, delete some event)
	 * @param data
	 *            - the event to be send to the server
	 * @param context
	 *            - dont't worry about this.
	 */
	public static void sendToServer(ServerRequest request, EventEntry data,
			Context context) {
		// check to see if allowed to send data
		if (Settings.isSychronizationEnabled()) {
			Intent intent = new Intent(context, Synchronizer.class);
			intent.putExtra("EventData", data);
			intent.putExtra("Request", request);
			context.startService(intent);
		}
	}
	
	public static PostRequestResponse sendPostRequest(EventEntry data, ServerRequest request){
		DefaultHttpClient hc = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(request.getURL());
		
		List<NameValuePair> params = getPostParams(request, data);
		try {

			postMethod.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			String response = hc.execute(postMethod, res);
		} catch (ClientProtocolException e) {
			//
			return PostRequestResponse.Error;

		} catch (IOException e) {

			return PostRequestResponse.Error;
		}
		return PostRequestResponse.Success;
	}
	

	private static List<NameValuePair> getPostParams(ServerRequest request, EventEntry data) {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		if(request==ServerRequest.REGISTER){
			params.add(new BasicNameValuePair("PhoneNumber", Settings
					.getPhoneNumber()));
			params.add(new BasicNameValuePair("UUIDOfDevice", Settings
					.getDeviceUUID()));
			params.add(new BasicNameValuePair("HashedPasswd", Settings
					.getPassword()));
		}
		if(request==ServerRequest.SENDDATA){
			params.add(new BasicNameValuePair("UUIDOfDevice", Settings
					.getDeviceUUID()));
			params.add(new BasicNameValuePair("UUIDOfEvent", data.mUUID));
			params.add(new BasicNameValuePair("EventData", EventDataSerializer
					.toJSONObject(data).toString()));
		}
		if(request==ServerRequest.UPDATE){
			params.add(new BasicNameValuePair("UUIDOfDevice", Settings
					.getDeviceUUID()));
			params.add(new BasicNameValuePair("UUIDOfEvent", data.mUUID));
			params.add(new BasicNameValuePair("EventData", EventDataSerializer
					.toJSONObject(data).toString()));
		}
		if(request==ServerRequest.DELETE){
			
			
				params.add(new BasicNameValuePair("UUIDOfDevice", Settings
						.getDeviceUUID()));
				params.add(new BasicNameValuePair("UUIDOfEvent", data.mUUID));
			
		}
		return params;
	}



	public static String createUUID() {
		return UUID.randomUUID().toString();

	}
}
