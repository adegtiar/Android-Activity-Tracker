package edu.berkeley.security.eventtracker;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.maps.HelloGoogleMaps;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;
import edu.berkeley.security.eventtracker.network.Synchronizer;

public class TrackingMode extends AbstractEventEdit {
	/**
	 * The minimum time of an activity (used to prevent spamming the next
	 * activity button).
	 */
	public static final int MIN_ACTIVITY_DURATION = 5;
	private static final int MILLIS_PER_SECOND = 1000;

	private TextView textViewStartTime;
	private ProgressIndicatorSpinner myProgressTimer;

	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		gpsServiceIntent = new Intent(this, GPSLoggerService.class);
		SynchronizerIntent = new Intent(this, Synchronizer.class);

		myProgressTimer = new ProgressIndicatorSpinner(1000);

		Settings.setPhoneNumber(this);
		Networking.registerIfNeeded(this);

		
		// Attempts to send all the requests that are suppose to be sent
		// but for some reason did not make it to the web server.
		Networking.sendAllEvents(this);

	
		
		dropDown.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if (isTracking()) {
					String tagChosen = parent.getItemAtPosition(position)
							.toString();
					currentEvent.mTag = tagChosen;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		updateDatabase(currentEvent);
		updateTrackingNotification();
	}

	@Override
	protected void initializeBottomBar() {
		bottomBar = (Button) findViewById(R.id.previous_activity_bar);
		bottomBar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startEditEventActivity(previousEvent.mDbRowID);
			}
		});
	}

	protected void startEditEventActivity(long rowId) {
		Intent editIntent = new Intent(this, EditMode.class);
		editIntent.putExtra(EventKey.ROW_ID.columnName(), rowId);
		startActivity(editIntent);
	}

	@Override
	protected void initializeTimesUI() {
		textViewStartTime = (TextView) findViewById(R.id.startTime);
	}

	@Override
	protected void fillViewWithEventInfo() {
		if (currentEvent != null) {
			eventNameEditText.setText(currentEvent.mName);
			textViewStartTime.setText(currentEvent
					.formatColumn(EventKey.START_TIME));
		} else {
			eventNameEditText.setText("");
			textViewStartTime.setText("");
		}
		bottomBar.setText(getPreviousEventString());
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.tracking_event;
	}

	@Override
	protected void updateTrackingUI(boolean isTracking) {
		super.updateTrackingUI(isTracking);
		nextActivityButton.setEnabled(isTracking);
		stopTrackingButton.setEnabled(isTracking);
		eventNotesButton.setEnabled(isTracking);
		int image = isTracking ? R.drawable.maps_on : R.drawable.maps_off;
		viewMapButton.setImageResource(image);
		dropDown.setEnabled(isTracking);
		newTagButton.setEnabled(isTracking);
		dropDown.setSelection(0);
		

	}

	@Override
	protected void syncToEventFromUI() {
		if (currentEvent != null) {
			currentEvent.mName = eventNameEditText.getText().toString();
		}
	}

	
	private Handler mHandler = new Handler();
	private Runnable mUpdateTimeTask = new Runnable() {
		   public void run() {
		     eventNameEditText.showDropDown();
		   }
		};
	
	@Override
	protected void initializeEditTexts() {
		super.initializeEditTexts();
		eventNameEditText.addTextChangedListener(new StartTrackingListener());

		eventNameEditText.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
						if(event.getAction()==0){
							mHandler.removeCallbacks(mUpdateTimeTask);
				            mHandler.postDelayed(mUpdateTimeTask, 200);
						}				
						return false;
			}
		});
		eventNameEditText.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				String activityName= (String) ((TextView)arg1).getText();
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(eventNameEditText.getWindowToken(), 0);
				EventEntry thisEvent=EventManager.getManager().fetchEvents(activityName);
				currentEvent.mTag=thisEvent.mTag;
				initializeTags();
			}
		});
	}
	
	@Override
	protected void setNameText(String name) {
		if (currentEvent == null) {
			currentEvent = new EventEntry();
		}
		currentEvent.mName = name;
		updateDatabase(currentEvent);
	}

	@Override
	protected void setNotesText(String notes) {
		if (currentEvent == null) {
			currentEvent = new EventEntry();
		}
		currentEvent.mNotes = notes;
		updateDatabase(currentEvent);
	}

	/**
	 * Listens for a text change and creates a new event if one doesn't exist.
	 */
	private class StartTrackingListener implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {

			
			if (currentEvent != null) {
				updateTrackingNotification();
			}
			if (s.length() != 0 && currentEvent == null) {
				currentEvent = new EventEntry();
				updateDatabase(currentEvent);
				updateStartTimeUI();
				updateTrackingStatus();
				showStartingToastMessage();
			}
			if (justResumed) {
				justResumed = false;
			}
			else {
				myProgressTimer.spin();
			}
				
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			
		}
	}

	private void updateStartTimeUI() {
		textViewStartTime.setText(currentEvent
				.formatColumn(EventKey.START_TIME));
	}

	/**
	 * Checks whether the start time of an event is past the threshold for
	 * saving it (to prevent spamming the next activity button).
	 * 
	 * @return whether the current event has a long enough duration.
	 */
	private boolean timePassedThreshold() {
		return (System.currentTimeMillis() - currentEvent.mStartTime)
				/ MILLIS_PER_SECOND > MIN_ACTIVITY_DURATION;
	}

	/**
	 * Checks whether the current event (or the text field it corresponds to)
	 * has a name.
	 * 
	 * @return whether the current event has a name.
	 */
	private boolean currentEventHasName() {
		return currentEvent.mName.length() > 0
				|| eventNameEditText.getText().length() > 0;
	}

	@Override
	protected void initializeActivityButtons() {
		super.initializeActivityButtons();

		nextActivityButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentEventHasName() || timePassedThreshold()) {
					showStartingToastMessage();
					finishCurrentActivity(true);
				}
				
				eventNameEditText.requestFocus();
				
			}
		});

		stopTrackingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//no longer tracking
				showEndingToastMessage();
				finishCurrentActivity(false);
				focusOnNothing();
			}
		});
		viewMapButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (currentEvent.getGPSCoordinates().size() == 0) {
					Toast.makeText(getApplicationContext(), "No data yet",
							Toast.LENGTH_SHORT).show();
				} else {

					Intent myIntent = new Intent(TrackingMode.this,
							HelloGoogleMaps.class);
					myIntent.putExtra("EventData", currentEvent);
					startActivity(myIntent);
				}

			}
		});
		dropDown.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if (isTracking()) {
					String tagChosen = parent.getItemAtPosition(position)
							.toString();
					currentEvent.mTag = tagChosen;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}

	/**
	 * Finishes the currently running activity and start tracking a new
	 * activity, if specified.
	 * 
	 * @param createNewActivity
	 *            Whether or not to start tracking a new activity.
	 */
	private void finishCurrentActivity(boolean createNewActivity) {

		
		currentEvent.mEndTime = System.currentTimeMillis();
		updateAutoComplete();
		syncToEventFromUI();
		updateDatabase(currentEvent);

		// attempt to send data now
		Networking.sendToServer(ServerRequest.SENDDATA, currentEvent, this);

		previousEvent = currentEvent;
		if (createNewActivity)
			startNewActivity();
		else
			currentEvent = null;
		updateTrackingStatus();
		fillViewWithEventInfo();

	}

	private void startNewActivity() {
		currentEvent = new EventEntry();
		updateDatabase(currentEvent);
	}

	private void updateTrackingNotification() {
		if (Settings.areNotificationsEnabled() && isTracking()) {
			enableTrackingNotification(this, getCurrentEvent());
		} else {
			disableTrackingNotification(this);
		}
	}

	/**
	 * Controls the activity's progress indicator spinner. Sets it to spin for a
	 * set amount of time when the spin method is called.
	 */
	private class ProgressIndicatorSpinner {
		private CountDownTimer myTimer;
		private boolean isSpinning = false;
		private long spinTime;

		private ProgressIndicatorSpinner(long spinTime) {
			this.spinTime = spinTime;
			setProgressBarIndeterminateVisibility(isSpinning);
			resetTimer();
		}

		/**
		 * Starts the activity progress indicator spinner. If it is already
		 * spinning, it resets the time for the spinner to stop.
		 */
		private void spin() {
			synchronized (myTimer) {
				if (isSpinning)
					myTimer.cancel();
				setSpinning(true);
				resetTimer().start();
			}
		}

		/**
		 * Sets the spinner to start or stop spinning.
		 * 
		 * @param isSpinning
		 *            Whether the spinner should spin or not.
		 */
		private void setSpinning(boolean isSpinning) {
			synchronized (myTimer) {
				if (this.isSpinning == isSpinning)
					return;
				this.isSpinning = isSpinning;
				setProgressBarIndeterminateVisibility(isSpinning);
			}
		}

		/**
		 * Resets the timer to its initial value. Does not start the timer.
		 * 
		 * @return The timer it reset.
		 */
		private CountDownTimer resetTimer() {
			return myTimer = new SpinTimer(spinTime, Long.MAX_VALUE);
		}

		/**
		 * A timer that disables the spinner upon finish.
		 */
		private class SpinTimer extends CountDownTimer {

			public SpinTimer(long millisInFuture, long countDownInterval) {
				super(millisInFuture, countDownInterval);
			}

			@Override
			synchronized public void onFinish() {
				showToastStatusMessage();
				setSpinning(false);
				resetTimer();
				
				
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// do nothing
			}

		}
	}

	private void showStartingToastMessage() {
		displayToast("Starting a new activity");
	}
	/*
	 * Display the toast message saying that an activity is no longer being tracked
	 */
	private void showEndingToastMessage() {
		CharSequence toastMsg= "No longer tracking";
		displayToast(toastMsg);
	}
	
	/*
	 * Display the toast relating to the updating and starting of events
	 */
	private void showToastStatusMessage() {
		if (isTracking()) {
			String currentEventName = eventNameEditText.getText().toString();
			if(currentEventName.length() < 2){
				return;
			}
			String durationString = calculateDuration();
			CharSequence toastMsg = "Updating activity " + currentEventName +
			                    "\n" + "(started " + durationString + ")";
	
			displayToast(toastMsg);
		}
	}
	
	
	/*
	 * Displays the msg as a toast 
	 */
	private void displayToast(CharSequence msg) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, msg, duration);
		toast.setGravity(Gravity.CENTER, 0, -15);
		toast.show();	
	}
	
	/**
	 * Queries the tag database in order to populate the tag drop down menu.
	 */
	protected void initializeTags() {

		dropDown = (Spinner) findViewById(R.id.tagSpinner);

		LinkedHashSet<String> tagSet = EventActivity.mEventManager.getTags();
		ArrayList<String> mTagList = new ArrayList<String>();
		for (String tag : tagSet) {
			mTagList.add(tag);
		}
		mTagList.add(0, "Select a tag");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, mTagList);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dropDown.setAdapter(adapter);
		int position;
		if (isTracking()) {
			position = mTagList.indexOf(currentEvent.mTag);
		} else {
			position = mTagList.indexOf("Select a tag");
		}
		//
		dropDown.setSelection(position, true);
	}


	@Override
	protected Class<?> getLeftActivityClass() {
		return ListEvents.class;
	}

	@Override
	protected Class<?> getRightActivityClass() {
		return null;
	}
}
