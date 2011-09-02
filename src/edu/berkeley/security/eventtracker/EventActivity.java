package edu.berkeley.security.eventtracker;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.prediction.PredictionService;

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
	
	protected TextView textViewIsTracking;

	// Machine learning variables
	// public static boolean isDbUpdated;

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
		//TODO remove this debugging statement once everything works.
		EventCursor events = EventManager.getManager()
		.fetchUndeletedEvents();
		int count = events.getCount();
		// Attach fling listeners to the views in the top toolbar
		findViewById(R.id.toolbar_center).setOnClickListener(detector);
		int[] viewsToAttachListener = new int[] { R.id.toolbar_left_option,
				R.id.toolbar_right_option, R.id.toolbar_center };
		for (int view_id : viewsToAttachListener) {
			findViewById(view_id).setOnTouchListener(flingListener);
		}

		// Set up preferences
		settings = getSharedPreferences(Settings.PREFERENCE_FILENAME,
				MODE_PRIVATE);

		startTrackingDuration();
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
		TextView centerText = (TextView) findViewById(R.id.toolbar_center);
		
		// In order to center the the toolbar text, padding is dynamically added/removed.
		if (getRightActivityClass() == null){
			centerText.setPadding(20, 0, 0, 0);
		}
		if (getLeftActivityClass() == null) {
			centerText.setPadding(0, 0, 20, 0);
		}
		
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
	protected void onPause() {
		super.onPause();
		PredictionService.syncModelToStorage();
		mHandlerDuration.removeCallbacks(mUpdateTimeTaskDuration);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshState();
		updateTrackingStatus();
		// updateToolbarGUI();
		Networking.pollServerIfAllowed(this);
		startTrackingDuration();
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
		startTrackingDuration();
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
		if (!isTracking) {
			textViewIsTracking.setText(notTrackingStringID);
		}
	
//		if (isTracking && getCurrentEvent() != null) {
//			textViewIsTracking.append(getCurrentEvent().mName);
//		}
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
		public boolean onDown(MotionEvent e) {
			return super.onDown(e);
		}

		@Override
		public void onClick(View v) {
		}

	}

	private Handler mHandlerDuration = new Handler();
	private Runnable mUpdateTimeTaskDuration = new Runnable() {
		public void run() {
			updateToolbarMessage();
			mHandlerDuration.postDelayed(this, 60000);
		}
	};

	/*
	 * Updates the toolbar message using the latest duration and name
	 */
	public void updateToolbarMessage() {
		EventEntry thisCurrentEvent = getCurrentEvent();
		if (isTracking() && thisCurrentEvent != null) {
		    String durationString = calculateDurationString();
		    // Event just started so there is no duration yet
		    if (durationString.length() == 0) {
		    	textViewIsTracking.setText("Just started tracking");
		    } else{
		    	textViewIsTracking.setText("Tracking for ");
		    	textViewIsTracking.append(durationString);
		    }
		}
	}

	/*
	 * Returns a string representation of the duration(given in ms) ex: sec ago,
	 * 6 min, 1.5 hr
	 */
	protected String calculateDurationString() {

		if (getCurrentEvent() == null) {
			return "";
		}
		long currentTime = System.currentTimeMillis();
		long duration = currentTime - getCurrentEvent().mStartTime;
		long durationInSeconds = duration / 1000;

		// between 0 and 60
		long numOfMinutes = (durationInSeconds / 60) % 60;
		// no limit on the number of hours
		long numOfHours = durationInSeconds / 3600;

		String durationString = "";
		// duration is between 0 and 60 seconds
		if (numOfHours == 0 && numOfMinutes == 0) {
			return "";

		}
		
		// between 0 and 1 hour
		else if (numOfHours == 0) {
			durationString = Long.toString(numOfMinutes) + " min";
			// making the duration string plural
			if (numOfMinutes > 1) {
				durationString += "s";
			}
		} else if (numOfHours < 10) {
			// returns the number of hours rounded to one decimal place
			double hoursInDecimal = durationInSeconds / 3600.0;
			DecimalFormat df = new DecimalFormat("#.#");
			durationString = df.format(hoursInDecimal) + " hr";
			// making the duration string plural
			if (hoursInDecimal > 1) {
				durationString += "s";
			}
		} else if (numOfHours >= 10 && numOfHours < 24){
			// greater than 10 hours. so don't display any decimals
			durationString = Long.toString(numOfHours) + " hr";
			// making the duration string plural
			if (numOfHours > 1) {
				durationString += "s";
			}
		} else {
			// Its more than 24 hours. Show the duration in terms of days
			durationString = Long.toString(numOfHours/24) + " day";
			if (numOfHours/24 > 1) {
				durationString += "s";
			}
		}
		return durationString;

	}

	/*
	 * If tracking, then start modifying the toolbar to keep track of the
	 * duration of the activity
	 */
	private void startTrackingDuration() {
		mHandlerDuration.removeCallbacks(mUpdateTimeTaskDuration);
		if (isTracking()) {
			// if Tracking, then start tracking the duration in the toolbar
			mHandlerDuration.postDelayed(mUpdateTimeTaskDuration, 100);
		}

	}

}
