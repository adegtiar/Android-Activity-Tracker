package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

/**
 * The main activity that all event-related activities extend. It houses a
 * toolbar and a global EventManager.
 */
abstract public class EventActivity extends Activity {
	private static final int trackingStringID = R.string.toolbarTracking;
	private static final int notTrackingStringID = R.string.toolbarNotTracking;
	static final int TRACKING_NOTIFICATION = 1;
	protected TextView textViewIsTracking;
	protected EventManager mEventManager;
	// Variables for Services
	protected static Intent gpsServiceIntent;
	protected static Intent serverServiceIntent;
	// Variables for preferences
	public static SharedPreferences settings;
	public static SharedPreferences serverSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ViewStub v = (ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(getLayoutResource());
		v.inflate();

		initializeToolbar();

		mEventManager = EventManager.getManager(this);

	}

	/**
	 * Initializes the toolbar onClickListeners and intializes references to
	 * toolbar views. The left button is initialized to the edit activity
	 * button.
	 */
	protected void initializeToolbar() {
		textViewIsTracking = (TextView) findViewById(R.id.toolbar_center);

		findViewById(R.id.toolbar_right_option).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						startServerActivity();
					}
				});
		findViewById(R.id.toolbar_left_option).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						startTrackingActivity();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.event_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settings_option:
			startSettingsActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshState();
		updateTrackingStatus();
		updateToolbarGUI();
	}

	/**
	 * Refreshes the state from the database. Called prior to any calls to
	 * isTracking() and updateTrackingUI().
	 */
	protected void refreshState() {
	}

	private void updateToolbarGUI() {
		((ImageView) findViewById(R.id.toolbar_right_option))
				.setImageResource(ServerActivity.isServerRunning() ? R.drawable.server_on_64
						: R.drawable.server_off_64);
	}

	/**
	 * Launches the ListEvents activity.
	 */
	protected void startListEventsActivity() {
		Intent listIntent = new Intent(this, ListEvents.class);
		startActivity(listIntent);
	}

	/**
	 * Launches the Settings activity.
	 */
	protected void startSettingsActivity() {
		Intent settingsIntent = new Intent(this, Settings.class);
		startActivity(settingsIntent);
	}

	protected void startServerActivity() {
		Intent settingsIntent = new Intent(this, ServerActivity.class);
		startActivity(settingsIntent);
	}

	/**
	 * Launches the AbstractEventEdit activity.
	 */
	protected void startTrackingActivity() {
		Intent settingsIntent = new Intent(this, TrackingMode.class);
		startActivity(settingsIntent);
	}

	protected void startEditEventActivity(long rowId) {
		Intent settingsIntent = new Intent(this, EditMode.class);
		settingsIntent.putExtra(ColumnType.ROWID.getColumnName(), rowId);
		startActivity(settingsIntent);
	}

	/**
	 * Calls isTracking() and updates the state accordingly.
	 * 
	 * @return True if it an event is tracked, false otherwise.
	 */
	protected boolean updateTrackingStatus() {
		boolean isTracking = isTracking();
		updateTrackingStatus(isTracking);
		return isTracking;
	}

	/**
	 * Updates the state according to whether or not it is being tracked.
	 */
	protected void updateTrackingStatus(boolean isTracking) {
		updateTrackingUI(isTracking);
		if (isTracking && Settings.isGPSEnabled()) {
			gpsServiceIntent.putExtra("gps", Settings.isGPSEnabled());
		}
		if (isTracking) {
			startService(gpsServiceIntent);
		} else {
			stopService(gpsServiceIntent);
			disableTrackingNotification();
		}
	}

	protected void startUpService() {

	}

	private void disableTrackingNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		mNotificationManager.cancel(TRACKING_NOTIFICATION);
	}

	/**
	 * Overrides calling isTracking() with the given boolean.
	 * 
	 * @param isTracking
	 *            Whether or not an event is being tracked.
	 */
	protected void updateTrackingUI(boolean isTracking) {
		textViewIsTracking.setText(isTracking ? trackingStringID
				: notTrackingStringID);
	}

	/**
	 * @return Whether or not an activity is being tracking. Should be preceded
	 *         by a call to refresh state, if not already refreshed.
	 */
	protected boolean isTracking() {
		return mEventManager.isTracking();

	}

	/**
	 * @return the current event
	 */
	public EventEntry getCurrentEvent() {
		return mEventManager.getCurrentEvent();
	}

	/**
	 * @return The layout resource to inflate in onCreate.
	 */
	abstract protected int getLayoutResource();

	static void enableTrackingNotification(Context mContext,
			EventEntry trackedEvent) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) mContext
				.getSystemService(ns);

		int icon = R.drawable.edit_icon;
		if (trackedEvent == null)
			trackedEvent = EventManager.getManager().getCurrentEvent();
		CharSequence tickerText = "Now tracking an event.";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = mContext.getApplicationContext();
		CharSequence contentTitle = "Event in progress";
		CharSequence contentText = "Event: " + trackedEvent.mName;
		Intent notificationIntent = new Intent(mContext, TrackingMode.class);
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				notificationIntent, 0);

		notification.flags |= Notification.FLAG_NO_CLEAR;

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		mNotificationManager.notify(EventActivity.TRACKING_NOTIFICATION,
				notification);
	}

	// GPS

	// private class SampleLocationListener implements LocationListener {
	// public void onLocationChanged(Location location) {
	// EventEntry currentEvent = getCurrentEvent();
	// if (location != null && currentEvent != null) {
	//
	// mEventManager.addGPSCoordinates(new GPSCoordinates(location
	// .getLatitude(), location.getLongitude()),
	// currentEvent.mDbRowID);
	//
	// }
	// }
	//
	// public void onProviderDisabled(String provider) {
	// // TODO Auto-generated method stub
	// Log.d("SampleLocationListener onProviderDisabled", provider);
	// }
	//
	// public void onProviderEnabled(String provider) {
	// // TODO Auto-generated method stub
	// Log.d("SampleLocationListener onProviderEnabled", provider);
	// }
	//
	// public void onStatusChanged(String provider, int status, Bundle extras) {
	// // TODO Auto-generated method stub
	// Log.d("SampleLocationListener onStatusChanged", provider);
	// }
	// }

}
