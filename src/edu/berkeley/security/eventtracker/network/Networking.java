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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventDataSerializer;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

public class Networking {

	/**
	 * Called when the user first enters their password Sends the post request
	 * that registers the user's phone with the web server
	 * TODO this should happen in a separate thread
	 */
	public static void sendRegistration() {
		DefaultHttpClient hc = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(
				"http://192.168.0.107:3000/users/init");

		// HttpParams params=new BasicHttpParams();
		// params.setParameter("PhoneNumber", Settings.getPhoneNumber());
		// params.setParameter("UUIDOfDevice", Settings.getDeviceUUID());
		// postMethod.setParams(params);

		List<NameValuePair> params = new LinkedList<NameValuePair>();

		params.add(new BasicNameValuePair("PhoneNumber", Settings
				.getPhoneNumber()));
		params.add(new BasicNameValuePair("UUIDOfDevice", Settings
				.getDeviceUUID()));

		try {
			postMethod.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			String response = hc.execute(postMethod, res);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	public static void sendData(EventEntry data) {
		DefaultHttpClient hc = new DefaultHttpClient();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpPost postMethod = new HttpPost(
				"http://192.168.0.105:3001/events/upload");
		HttpParams params = new BasicHttpParams();

		params.setParameter("UUIDOfDevice", Settings.getDeviceUUID());
		params.setParameter("UUIDOfEvent", data.mUUID);
		postMethod.setParams(params);

		try {
			postMethod.setEntity(new StringEntity(EventDataSerializer
					.toJSONObject(data).toString()));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			String response = hc.execute(postMethod, res);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String createUUID() {
		return UUID.randomUUID().toString();

	}
}
