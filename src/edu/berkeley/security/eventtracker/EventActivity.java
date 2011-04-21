package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.network.Networking;

/**
 * The main activity that all event-related activities extend. It houses a
 * toolbar and a global EventManager.
 */

abstract public class EventActivity extends Activity {

	public static final String LOG_TAG = "ActivityTracker";

	public static Intent SynchronizerIntent;
	public static EventManager mEventManager; // TODO not sure if this is right.

	// Variables for preferences
	public static SharedPreferences settings;
	public static SharedPreferences serverSettings;

	protected static final int DIALOG_NOTE_ENTRY = 9;
	protected TextView textViewIsTracking;

	// Variables for Services
	protected static Intent gpsServiceIntent;
	protected static Intent serverServiceIntent;

	static final int TRACKING_NOTIFICATION = 1;

	private static final int trackingStringID = R.string.toolbarTracking;
	private static final int notTrackingStringID = R.string.toolbarNotTracking;

	private GestureDetector gestureDetector;
	private OnTouchListener flingListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ViewStub v = (ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(getLayoutResource());
		v.inflate();
		initializeToolbar();

		mEventManager = EventManager.getManager(this);
		FlingDetector detector = new FlingDetector(getLeftActivityClass(),
				getRightActivityClass());
		gestureDetector = new GestureDetector(detector);
		flingListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		findViewById(R.id.toolbar_center).setOnClickListener(detector);
		findViewById(R.id.toolbar_center).setOnTouchListener(flingListener);
	}

	/**
	 * Returns the activity that would be launched upon swiping left.
	 * 
	 * @return the class of the activity to launch.
	 */
	abstract protected Class<?> getLeftActivityClass();

	/**
	 * Returns the activity that would be launched upon swiping right.
	 * 
	 * @return the class of the activity to launch.
	 */
	abstract protected Class<?> getRightActivityClass();

	/**
	 * Initializes the toolbar onClickListeners and initializes references to
	 * toolbar views. The left button is initialized to the edit activity
	 * button.
	 */
	protected void initializeToolbar() {
		textViewIsTracking = (TextView) findViewById(R.id.toolbar_center);
		setToolbarButton((ImageView) findViewById(R.id.toolbar_right_option),
				false);
		setToolbarButton((ImageView) findViewById(R.id.toolbar_left_option),
				true);
	}

	/**
	 * Sets the onClickListener and visibility of the given left or right
	 * toolbar button.
	 * 
	 * @param button
	 *            the left or right button of the top toolbar.
	 * @param isLeft
	 *            whether or not it is the left button.
	 */
	private void setToolbarButton(ImageView button, final boolean isLeft) {
		final Class<?> activityClass = isLeft ? getLeftActivityClass()
				: getRightActivityClass();

		if (activityClass == null)
			button.setVisibility(View.INVISIBLE);
		else {
			button.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (isLeft)
						EventActivity.this.startActivityLeft(activityClass);
					else
						EventActivity.this.startActivityRight(activityClass);
				}
			});
			button.setImageResource(getImageIdForActivity(activityClass));
		}
	}

	private int getImageIdForActivity(Class<?> activityClass) {
		if (activityClass == TrackingMode.class)
			return R.drawable.edit_icon;
		else if (activityClass == ListEvents.class)
			return R.drawable.list_icon;
		else if (activityClass == Settings.class)
			return R.drawable.settings_icon;
		else
			throw new IllegalArgumentException(
					"Could not find icon for class: " + activityClass);
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
			startActivity(Settings.class);
			return true;
		case R.id.debug_option:
			startActivity(Debugging.class);
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
		// updateToolbarGUI();
		Networking.pollServerIfAllowed(this);
	}

	/**
	 * Refreshes the state from the database. Called prior to any calls to
	 * isTracking() and updateTrackingUI().
	 */
	protected void refreshState() {
	}

	protected void startActivity(Class<?> activityClass) {
		if (activityClass != null) {
			Intent debuggingIntent = new Intent(this, activityClass);
			startActivity(debuggingIntent);
		}
	}

	protected void startActivityLeft(Class<?> activityClass) {
		startActivity(activityClass);
		overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}

	protected void startActivityRight(Class<?> activityClass) {
		startActivity(activityClass);
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
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
		}
		if (!isTracking || !Settings.areNotificationsEnabled()) {
			disableTrackingNotification(this);
		}
	}

	protected void startUpService() {

	}

	protected static void disableTrackingNotification(Context mContext) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) mContext
				.getSystemService(ns);
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

	class FlingDetector extends SimpleOnGestureListener implements
			OnClickListener {
		protected static final int SWIPE_MIN_DISTANCE = 120;
		protected static final int SWIPE_MAX_OFF_PATH = 250;
		protected static final int SWIPE_THRESHOLD_VELOCITY = 200;
		private final Class<?> onLeftFling;
		private final Class<?> onRightFling;

		public FlingDetector(Class<?> onLeftFling, Class<?> onRightFling) {
			this.onLeftFling = onLeftFling;
			this.onRightFling = onRightFling;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// right to left swipe
					EventActivity.this.startActivityRight(onRightFling);
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// left to right swipe
					EventActivity.this.startActivityLeft(onLeftFling);
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}

		@Override
		public void onClick(View v) {
		}

	}

}
