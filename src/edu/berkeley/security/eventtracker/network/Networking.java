package edu.berkeley.security.eventtracker.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.berkeley.security.eventtracker.Settings;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Networking {
	
	
	/**
	 * Called when the user first enters their password
	 * Sends the post request that registers the user's phone with the web server
	 */
	public static void sendRegistration(){
		sendData("");
	}
	
	/**
	 * Makes a post request to the web server
	 * @param data- a JSON object to be sent
	 * 
	 *TODO: If User doesn't get a response, add to some data structure. Try sending it every time app starts??
	 */
	public static void sendData(String data){
		DefaultHttpClient hc=new DefaultHttpClient();  
		ResponseHandler <String> res=new BasicResponseHandler();  
		HttpPost postMethod=new HttpPost("http://192.168.1.123:8080"); 
		postMethod.addHeader("PhoneNumber", Settings.getPhoneNumber());
		postMethod.addHeader("UUIDOfDevice", Settings.getDeviceUUID());
		
		try {
			postMethod.setEntity(new StringEntity(data));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		try {
			String response=hc.execute(postMethod,res);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
}
