package edu.berkeley.security.eventtracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;

public class GPSLoggerService extends Service {

	private LocationManager lm;
	private LocationListener locationListener;

	private static long minTimeMillis;
	private static long minDistanceMeters;
	private static float minAccuracyMeters = 35;
	private EventManager manager;
	private EventEntry currentEvent;

	/** Called when the activity is first created. */
	private void startLoggerService() {

		// use the LocationManager class to obtain GPS locations
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();

		minTimeMillis = Settings.getGPSUpdateTime() * 60000;
		minDistanceMeters = Settings.getGPSSensitivity();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMillis,
				minDistanceMeters, locationListener);

	}

	private void shutdownLoggerService() {
		lm.removeUpdates(locationListener);
	}

	public class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			if (loc == null) {
				return;
			}

			manager = EventManager.getManager();
			if (manager != null) {
				currentEvent = manager.getCurrentEvent();
				if (currentEvent != null) {
					manager.addGPSCoordinates(new GPSCoordinates(loc
							.getLatitude(), loc.getLongitude()),
							currentEvent.mDbRowID);
				}

			} else {
				manager = EventManager.getManager(GPSLoggerService.this);
				currentEvent = manager.getCurrentEvent();
				if (currentEvent != null) {
					manager.addGPSCoordinates(new GPSCoordinates(loc
							.getLatitude(), loc.getLongitude()),
							currentEvent.mDbRowID);
					manager.close();
				}
			}

		}

		public void onProviderDisabled(String provider) {

		}

		public void onProviderEnabled(String provider) {

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		boolean gpsEnabled = intent.getBooleanExtra("gps", false);
		if (gpsEnabled && locationListener == null) {
			startLoggerService();
		}
		if (!gpsEnabled && locationListener != null) {
			shutdownLoggerService();
			locationListener = null;
		}

		if (Settings.areNotificationsEnabled())
			EventActivity.enableTrackingNotification(this, null);
	}

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (lm != null)
			shutdownLoggerService();

	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static void setMinTimeMillis(long _minTimeMillis) {
		minTimeMillis = _minTimeMillis;
	}

	public static long getMinTimeMillis() {
		return minTimeMillis;
	}

	public static void setMinDistanceMeters(long _minDistanceMeters) {
		minDistanceMeters = _minDistanceMeters;
	}

	public static long getMinDistanceMeters() {
		return minDistanceMeters;
	}

	public static float getMinAccuracyMeters() {
		return minAccuracyMeters;
	}

	public static void setMinAccuracyMeters(float minAccuracyMeters) {
		GPSLoggerService.minAccuracyMeters = minAccuracyMeters;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GPSLoggerService getService() {
			return GPSLoggerService.this;
		}
	}

}
