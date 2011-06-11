package edu.berkeley.security.eventtracker.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventDataSerializer;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * This is the networking class. Sends requests to the server.
 */

public class Networking {
	public static final String PHONE_NUMBER_PARAM = "phone_number";
	public static final String DEVICE_UUID_PARAM = "uuid";
	public static final String POLL_TIME_PARAM = "PollTime";
	public static final String PASSWORD_PARAM = "password";
	public static final String HASHED_PASSWORD_PARAM = "HashedPasswd";
	public static final String EVENT_UUID_PARAM = "UUIDOfEvent";
	public static final String EVENT_DATA_PARAM = "EventData";
	public static final String EVENT_UPDATED_AT_PARAM = "UpdatedAt";
	public static final String EVENT_DELETED_PARAM = "Deleted";

	public static void sendAllEvents(Context context) {
		if (Settings.isSychronizationEnabled()) {
			EventCursor theCursor = EventActivity.mEventManager
					.fetchPhoneOnlyEvents();
			// send them all! LEAVE NO EVENT BEHIND
			EventEntry nextEvent;
			ArrayList<EventEntry> listOfEvents= new ArrayList<EventEntry>();
			while (theCursor.moveToNext()) {
				nextEvent = theCursor.getEvent();
				if (nextEvent != null) {
					listOfEvents.add(nextEvent);
				}
			}
			if(!listOfEvents.isEmpty()){
				Networking.sendToServerBulk(ServerRequest.SENDDATA, listOfEvents,
					context);
			}
		}
	}

	/**
	 * Attempts to register with the remote server if preferences permit and the
	 * device has not yet been registered.
	 * 
	 * @param context
	 *            the context from which to send the request.
	 */
	public static void registerIfNeeded(Context context) {
		if (!Settings.registeredAlready() && Settings.isSychronizationEnabled()) {
			// attempt to register with the server
			Networking.sendToServer(ServerRequest.REGISTER, null, context);
		}
	}

	/**
	 * Attempts to poll the server for data, if preferences permit.
	 * 
	 * @param context
	 *            the context from which to send the request.
	 */
	public static void pollServerIfAllowed(Context context) {
		if (Settings.isSychronizationEnabled()) {
			// poll the server for event data
			Long lastTimePolled=Settings.getLastPolledTime();
			Long currentTime=Calendar.getInstance().getTimeInMillis();
			if(currentTime - lastTimePolled > 30000){
				Settings.setLastPolledTime(currentTime);
				Networking.sendToServer(ServerRequest.POLL, null, context);
			}
		}
	}

	/**
	 * Sends intents to a service that sends the actual post requests Only does
	 * this if permission to synchronize with web server is given
	 * 
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
			ArrayList<EventEntry> listOfEvents=null;
			if(data != null){
				listOfEvents= new ArrayList<EventEntry>();
				listOfEvents.add(data);
			}
			Intent intent = new Intent(context, Synchronizer.class);
			intent.putExtra(Synchronizer.EVENT_LIST_EXTRA, listOfEvents);
			intent.putExtra(Synchronizer.REQUEST_EXTRA, request);
			context.startService(intent);
		}
	}
	
	
	/**
	 * Sends intents to a service that sends the actual post requests Only does
	 * this if permission to synchronize with web server is given
	 * 
	 * @param request
	 *            - the type of post request to send(i.e., register, send data,
	 *            update some event, delete some event)
	 * @param data
	 *            - the event to be send to the server
	 * @param context
	 *            - dont't worry about this.
	 */
	public static void sendToServerBulk(ServerRequest request, ArrayList<EventEntry> listOfEvents,
			Context context) {
		// check to see if allowed to send data
		if (Settings.isSychronizationEnabled()) {
			
			Intent intent = new Intent(context, Synchronizer.class);
			intent.putExtra(Synchronizer.EVENT_LIST_EXTRA, listOfEvents);
			intent.putExtra(Synchronizer.REQUEST_EXTRA, request);
			context.startService(intent);
		}
	}
	


	/**
	 * Sends a post request with the given event entry and request type.
	 * 
	 * @param data
	 *            the event to send. <tt>null</tt> if request does not require
	 *            an event.
	 * @param request
	 *            the type of request to send.
	 * @return the response to the request.
	 */
	public static PostRequestResponse sendPostRequest(ArrayList<EventEntry> listOfEvents,
			ServerRequest request) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(request.getURL());

		List<NameValuePair> params = getPostParams(request, listOfEvents);
		try {
			postMethod.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			Log.e(EventActivity.LOG_TAG, "Failed setting params.", e);
		}
		PostRequestResponse response;
		try {
			response = new PostRequestResponse(httpclient.execute(postMethod,
					res), true);
		} catch (ClientProtocolException e) {
			Log.e(EventActivity.LOG_TAG, "Failed sending post.", e);
			response = PostRequestResponse.errorResponse();
		} catch (IOException e) {
			Log.e(EventActivity.LOG_TAG, "Failed sending post.", e);
			response = PostRequestResponse.errorResponse();
		}
		return response;
	}

	/**
	 * Sends a post request of the given request type without an event.
	 * 
	 * @param request
	 *            the type of request to send. Should not require an event.
	 * @return the response to the request.
	 */
	public static PostRequestResponse sendPostRequest(ServerRequest request) {
		return sendPostRequest(null, request);
	}

	/**
	 * Returns the parameters associated with the particular request. These
	 * params can be used to set the content of a post request.
	 * 
	 * @param request
	 *            the type of request to get the parameters of.
	 * @param data
	 *            the event data of the request (null if not used).
	 * @return a List of parameters to send.
	 */
	private static List<NameValuePair> getPostParams(ServerRequest request,
			ArrayList<EventEntry> listOfEvents) {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		switch (request) {
		case REGISTER:
			params.add(new BasicNameValuePair(PHONE_NUMBER_PARAM, Settings
					.getPhoneNumber()));
			params.add(new BasicNameValuePair(PASSWORD_PARAM, Encryption.hashPassword(Settings
					.getPassword())));
			break;
		case SENDDATA:
		case UPDATE:
			try {
				JSONArray EventData = new JSONArray();
				for(EventEntry data: listOfEvents){
					JSONObject eventJSON= new JSONObject();
					eventJSON.accumulate(EVENT_UUID_PARAM, data.mUUID);
					eventJSON.accumulate(EVENT_DELETED_PARAM, String
							.valueOf(data.deleted));
					eventJSON.accumulate(EVENT_UPDATED_AT_PARAM,
							Synchronizer.dateFormatter.format(data.mUpdateTime));
					eventJSON.accumulate(EVENT_DATA_PARAM, 
							EventDataSerializer.encryptJSONObject(EventDataSerializer.toJSONObject(data)));
					EventData.put(eventJSON);
				}
				params.add(new BasicNameValuePair(EVENT_DATA_PARAM, EventData.toString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			break;
	
		case DELETE:
			//only delete one event at a time
			EventEntry event= listOfEvents.get(0);
			params.add(new BasicNameValuePair(EVENT_UUID_PARAM, event.mUUID));
			break;
		case POLL:
			String pollTime = Settings.getPollTime();
			if (pollTime != null)
				params.add(new BasicNameValuePair(POLL_TIME_PARAM, pollTime));
			break;
		}
		params.add(new BasicNameValuePair(DEVICE_UUID_PARAM, Settings
				.getDeviceUUID()));
		return params;
	}

	/**
	 * Generates a new random UUID.
	 * 
	 * @return the new String UUID.
	 */
	public static String createUUID() {
		return UUID.randomUUID().toString();

	}

	/**
	 * @return the IP address of the device.
	 */
	public static String getIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(EventActivity.LOG_TAG, "Failed getting the IP address.", ex);
		}
		return null;
	}


}