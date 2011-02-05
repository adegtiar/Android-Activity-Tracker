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
import edu.berkeley.security.eventtracker.eventdata.EventDataSerializer;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * This is the networking class. We send your packets
 */

enum PostRequestResponse{Success, Error}
public class Networking {

	/**
	 * 
	 * @param request- the type of post request to send(i.e., register, send data, update some event, delete some event)
	 * @param data- the event to be send to the server
	 * @param context- dont't worry about this.  
	 */
	public static void sendToServer(ServerRequest request, EventEntry data, Context context){
		Intent intent=new Intent(context, Synchronizer.class);
		intent.putExtra("EventData", data);
		intent.putExtra("Request", request);
		context.startService(intent);
	}
	
	/**
	 * Called when the user first enters their password Sends the post request
	 * that registers the user's phone with the web server TODO this should
	 * happen in a separate thread
	 */
	public static PostRequestResponse sendRegistration() {
		DefaultHttpClient hc = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(
				"http://eventtracker.dyndns-at-home.com:3001/users/init");

		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("PhoneNumber", Settings
				.getPhoneNumber()));
		params.add(new BasicNameValuePair("UUIDOfDevice", Settings
				.getDeviceUUID()));
		params.add(new BasicNameValuePair("HashedPasswd", Settings.getPassword()));
		try {

			postMethod.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			String response = hc.execute(postMethod, res);
		} catch (ClientProtocolException e) {
			return PostRequestResponse.Error;
		} catch (IOException e) {
			return PostRequestResponse.Error;
		}
		return PostRequestResponse.Success;

	}

	/**
	 * Makes a post request to the web server
	 * 
	 * @param data
	 *            - a JSON object to be sent
	 * 
	 *            TODO: If User doesn't get a response, add to some data
	 *            structure. Try sending it every time app starts??
	 */
	public static PostRequestResponse sendData(EventEntry data) {
		DefaultHttpClient hc = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(
				"http://eventtracker.dyndns-at-home.com:3001/events/upload");

		List<NameValuePair> params = new LinkedList<NameValuePair>();

		params.add(new BasicNameValuePair("UUIDOfDevice", Settings
				.getDeviceUUID()));
		params.add(new BasicNameValuePair("UUIDOfEvent", data.mUUID));
		params.add(new BasicNameValuePair("EventData", EventDataSerializer
				.toJSONObject(data).toString()));
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

	public static String createUUID() {
		return UUID.randomUUID().toString();

	}
}
