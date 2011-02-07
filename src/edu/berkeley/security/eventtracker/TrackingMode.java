package edu.berkeley.security.eventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;
import edu.berkeley.security.eventtracker.network.Synchronizer;
import edu.berkeley.security.eventtracker.webserver.WebServerService;

public class TrackingMode extends AbstractEventEdit {
	private TextView textViewStartTime;
	private ProgressIndicatorSpinner myProgressTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		gpsServiceIntent = new Intent(this, GPSLoggerService.class);
		serverServiceIntent = new Intent(this, WebServerService.class);
		SynchronizerIntent = new Intent(this, Synchronizer.class);
		// Set up preferences
		settings = getSharedPreferences(Settings.PREFERENCE_FILENAME,
				MODE_PRIVATE);
		serverSettings = getSharedPreferences(
				ServerActivity.PREFERENCE_FILENAME, MODE_PRIVATE);

		// starts the web server if it happened to be destroyed
		if (ServerActivity.isServerRunning()) {
			startService(serverServiceIntent);
		}

		myProgressTimer = new ProgressIndicatorSpinner(1000);
		// if (!Settings.isPasswordSet()) {
		// Bundle bundle = new Bundle();
		// bundle.putBoolean("Settings", false);
		// showDialog(DIALOG_TEXT_ENTRY, bundle);
		// }
		Settings.setPhoneNumber(this);
		Networking.registerIfNeeded(this);
		// Attempts to send all the requests that are suppose to be sent but for
		// some reason
		// did not make it to the web server
		Networking.sendAllEvents(this);

	}

	@Override
	protected void onPause() {
		super.onPause();
		updateDatabase(currentEvent);
	}

	@Override
	protected void initializeBottomBar() {
		bottomBar = (Button) findViewById(R.id.previous_activity_bar);
		bottomBar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startListEventsActivity();
			}
		});
	}

	@Override
	protected void initializeTimesUI() {
		textViewStartTime = (TextView) findViewById(R.id.startTime);
	}

	@Override
	protected void fillViewWithEventInfo() {
		if (currentEvent != null) {
			editTextEventName.setText(currentEvent.mName);
			editTextEventNotes.setText(currentEvent.mNotes);
			textViewStartTime.setText(currentEvent
					.formatColumn(ColumnType.START_TIME));
		} else {
			editTextEventName.setText("");
			editTextEventNotes.setText("");
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
	}

	@Override
	protected void syncToEventFromUI() {
		if (currentEvent != null) {
			currentEvent.mName = editTextEventName.getText().toString();
			currentEvent.mNotes = editTextEventNotes.getText().toString();
		}
	}

	@Override
	protected void initializeEditTexts() {
		super.initializeEditTexts();
		editTextEventName.addTextChangedListener(new StartTrackingListener());
		editTextEventNotes.addTextChangedListener(new StartTrackingListener());
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

			myProgressTimer.spin();
			if (currentEvent != null) {
				currentEvent.mName = s.toString();
				updateTrackingNotification();
			}
			if (s.length() != 0 && currentEvent == null) {
				currentEvent = new EventEntry();
				updateDatabase(currentEvent);
				updateStartTimeUI();
				updateTrackingStatus();
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
				.formatColumn(ColumnType.START_TIME));
	}

	@Override
	protected void initializeActivityButtons() {
		super.initializeActivityButtons();

		nextActivityButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finishCurrentActivity(true);
				editTextEventName.requestFocus();
			}
		});

		stopTrackingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finishCurrentActivity(false);
				focusOnNothing();

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
		enableTrackingNotification(this, getCurrentEvent());
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
				setSpinning(false);
				resetTimer();
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// do nothing
			}

		}
	}

	@Override
	protected void startServerActivity() {
		super.startServerActivity();
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
	}

	@Override
	protected void startListEventsActivity() {
		super.startListEventsActivity();
		overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (velocityX < 0) {// going to right screen
			startServerActivity();
		} else { // going to the left screen
			startListEventsActivity();
		}
		return true;
	}
}
