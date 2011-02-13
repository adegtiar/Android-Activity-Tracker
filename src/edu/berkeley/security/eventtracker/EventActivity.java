package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.network.Networking;

/**
 * The main activity that all event-related activities extend. It houses a
 * toolbar and a global EventManager.
 */
abstract public class EventActivity extends Activity implements
		OnGestureListener {
	public static final String LOG_TAG = "ActivityTracker";
	protected static final int DIALOG_TEXT_ENTRY = 7;
	private static final int trackingStringID = R.string.toolbarTracking;
	private static final int notTrackingStringID = R.string.toolbarNotTracking;
	static final int TRACKING_NOTIFICATION = 1;
	protected TextView textViewIsTracking;
	public static EventManager mEventManager; // TODO not sure if this is right.
	// Variables for Services
	protected static Intent gpsServiceIntent;
	protected static Intent serverServiceIntent;
	public static Intent SynchronizerIntent;
	// Variables for preferences
	public static SharedPreferences settings;
	public static SharedPreferences serverSettings;
	GestureDetector mGestureScanner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ViewStub v = (ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(getLayoutResource());
		v.inflate();
		initializeToolbar();

		mEventManager = EventManager.getManager(this);
		mGestureScanner = new GestureDetector(this);

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
		case R.id.debug_option:
			startDebuggingActivity();
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
		ServerActivity.updateIpAdress(Networking.getIpAddress());

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
		Intent serverIntent = new Intent(this, ServerActivity.class);
		startActivity(serverIntent);
	}

	protected void startDebuggingActivity() {
		Intent debuggingIntent = new Intent(this, Debugging.class);
		startActivity(debuggingIntent);
	}

	/**
	 * Launches the AbstractEventEdit activity.
	 */
	protected void startTrackingActivity() {
		Intent trackingIntent = new Intent(this, TrackingMode.class);
		startActivity(trackingIntent);
	}

	protected void startEditEventActivity(long rowId) {
		Intent editIntent = new Intent(this, EditMode.class);
		editIntent.putExtra(EventKey.ROWID.columnName(), rowId);
		startActivity(editIntent);
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

	@Override
	public boolean onDown(MotionEvent arg0) {
		// do nothing
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// do nothing
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// do nothing
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// do nothing
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// do nothing
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// do nothing
		return false;
	}

	public boolean onTouchEvent(MotionEvent me) {
		return mGestureScanner.onTouchEvent(me);
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

	/*
	 * Dialog box for password entry
	 */
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {

		case DIALOG_TEXT_ENTRY:
			// This example shows how to add a custom layout to an AlertDialog
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(
					R.layout.alert_dialog_text_entry, null);
			return new AlertDialog.Builder(EventActivity.this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.alert_dialog_text_entry)
					.setView(textEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User entered a password and clicked OK */
									EditText passwdEditText = (EditText) textEntryView
											.findViewById(R.id.password_edit);
									String password = passwdEditText.getText()
											.toString();
									Settings.setPassword(password);
									if (bundle.getBoolean("Settings")) {
										Settings.updatePasswordSettings();
									}

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		}
		return null;
	}

}
