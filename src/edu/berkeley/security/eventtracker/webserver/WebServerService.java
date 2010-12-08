package edu.berkeley.security.eventtracker.webserver;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * A Service which is started when the user wants to start the web server
 * This service creates a httpServer.  When this Service is stopped, the httpServer 
 * is stopped as well 
 */
public class WebServerService extends Service {

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		try {
			httpServer = new EventDataServer(this);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		httpServer.stop();
	}

	// This is the object that receives interactions from clients.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		WebServerService getService() {
			return WebServerService.this;
		}
	}
	EventDataServer httpServer;
}
