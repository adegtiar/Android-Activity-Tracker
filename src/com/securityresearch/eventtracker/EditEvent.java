package com.securityresearch.eventtracker;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.securityresearch.eventtracker.EventEntry.ColumnType;

public class EditEvent extends Activity {
	private static final int trackingStringID = R.string.toolbarTracking;
	private static final int notTrackingStringID = R.string.toolbarNotTracking;
	private static final int previousEventTextID = R.string.previousActivityText;
	private static final int previousEventDefaultID = R.string.previousActivityDefault;

	private EventManager mEventManager;
	private EventEntry currentEvent;
	private EventEntry previousEvent;

	private ArrayList<String> autoCompleteActivities=new ArrayList<String>();
	private ArrayList<String> autoCompleteLocations=new ArrayList<String>();
	private Set<String> mActivityNames = new HashSet<String>();
	private Set<String> mActivityLocations = new HashSet<String>();

	private ArrayAdapter<String> adapterActivities;
	private ArrayAdapter<String> adapterLocations;
	private AutoCompleteTextView editTextEventName;
	private AutoCompleteTextView editTextEventLocation;
	private Button previousActivityBar;
	private Button nextActivityButton;
	private Button stopTrackingButton;
	private TextView textViewStartTime;
	private TextView textViewIsTracking;


	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ViewStub v =(ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(R.layout.edit_event);
		v.inflate();

		mEventManager = new EventManager(this).open();

		initializeToolbar();
		initializeEditTexts();
		initializeAutoComplete();

		textViewStartTime = (TextView) findViewById(R.id.startTime);
		previousActivityBar = (Button) findViewById(R.id.previous_activity_bar);
		previousActivityBar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startListEventsActivity();
			}
		});

		initializeActivityButtons();
		editTextEventName.setHint(getString(R.string.eventNameHint));
		editTextEventLocation.setHint(getString(R.string.eventLocationHint));
	}

	/**
	 * Initializes the NextActivity and StopTracking buttons.
	 */
	private void initializeActivityButtons() {
		nextActivityButton=(Button)findViewById(R.id.NextActivityButton);
		stopTrackingButton=(Button)findViewById(R.id.StopTrackingButton);

		nextActivityButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finishCurrentActivity(true);
				focusOnNothing();
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
	 * Finishes the currently running activity and start tracking
	 * a new activity, if specified.
	 * @param createNewActivity Whether or not to start tracking a new activity.
	 */
	private void finishCurrentActivity(boolean createNewActivity) {
		currentEvent.mEndTime = System.currentTimeMillis();
		updateAutoComplete();
		updateDatabase(currentEvent);
		previousEvent = currentEvent;
		currentEvent = createNewActivity ? new EventEntry() : null;
		updateUI();
	}

	/**
	 * Initializes the AutoCompleteTextViews and intializes references to related views.
	 */
	private void initializeEditTexts() {
		editTextEventName = (AutoCompleteTextView) findViewById(R.id.editEventName);
		editTextEventLocation = (AutoCompleteTextView) findViewById(R.id.editLocation);
		//		TODO uncomment these to disable soft keyboard
		editTextEventName.setInputType(0); 
		editTextEventLocation.setInputType(0);

		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteActivities);
		adapterLocations = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteLocations);

		editTextEventName.setAdapter(adapterActivities);
		editTextEventLocation.setAdapter(adapterLocations);


		editTextEventName.addTextChangedListener(new StartTrackingListener());
		editTextEventLocation.addTextChangedListener(new StartTrackingListener());
	}

	/**
	 * Listens for a text change and creates a new event if one doesn't exist.
	 * @author AlexD
	 *
	 */
	private class StartTrackingListener implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) { 
			if (s.length() != 0 && currentEvent == null) {
				currentEvent = new EventEntry();
				updateStartTime();
				updateTrackingUI();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {}
	}

	/**
	 * Initializes the toolbar onClickListeners and intializes references to toolbar views.
	 */
	private void initializeToolbar() {		
		textViewIsTracking = (TextView) findViewById(R.id.toolbar_center);
		((ImageView) findViewById(R.id.toolbar_left_option)).setImageResource(R.drawable.list_icon);

		findViewById(R.id.toolbar_right_option).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startSettingsActivity();
			}
		});

		findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startListEventsActivity();
			}
		});
	}

	/**
	 * Launches the Settings activity.
	 */
	private void startSettingsActivity() {
		Intent settingsIntent = new Intent(EditEvent.this, Settings.class);
		settingsIntent.putExtra(getString(R.string.isTracking), isTracking());
		startActivity(settingsIntent);
	}

	/**
	 * Launches the ListEvents activity.
	 */
	private void startListEventsActivity() {
		Intent listIntent = new Intent(EditEvent.this, ListEvents.class);
		listIntent.putExtra(getString(R.string.isTracking), isTracking());
		startActivity(listIntent);
	}

	/**
	 * Updates the UI using the currentEvent and previousEvent.
	 */
	private void updateUI() {
		updateTrackingUI();
		fillViewWithEventInfo();
	}

	@Override
	protected void onResume() {
		super.onResume();

		EventCursor events = mEventManager.fetchSortedEvents();
		if (events.moveToNext()) {
			EventEntry event = events.getEvent();
			if (event.mEndTime != 0) {
				// We aren't tracking
				currentEvent = null;
				previousEvent = event;
			} else {
				// We are tracking
				currentEvent = event;
				previousEvent = events.moveToNext() ? events.getEvent() : null;
			}
		} else {
			currentEvent = null;
			previousEvent = null;
		}
		initializeAutoComplete();
		updateUI();

		focusOnNothing();

	}

	/**
	 * Fills the text entries and views with the correct info based on the
	 * current/previous events.
	 */
	private void fillViewWithEventInfo() {
		if (currentEvent != null) {
			editTextEventName.setText(currentEvent.mName);
			editTextEventLocation.setText(currentEvent.mLocation);
			textViewStartTime.setText(currentEvent.formatColumn(ColumnType.START_TIME));
		} else {
			editTextEventName.setText("");
			editTextEventLocation.setText("");
			textViewStartTime.setText("");
		}
		previousActivityBar.setText(getPreviousEventString());
	}

	/**
	 * Updates the UI start time box with the start time of the current event.
	 */
	private void updateStartTime() {
		if (currentEvent != null)
			textViewStartTime.setText(currentEvent.formatColumn(ColumnType.START_TIME));
		else
			textViewStartTime.setText("");
	}

	/**
	 * @return The text that the previous event bar should have, based on the previousEvent.
	 */
	private String getPreviousEventString() {
		String previousActivityLabel = getString(previousEventTextID);
		String previousEventString = previousEvent != null ? previousEvent.mName : getString(previousEventDefaultID); 
		return previousActivityLabel + " " + previousEventString;
	}

	@Override
	protected void onPause() {
		super.onPause();
		updateAutoComplete();
		updateDatabase(currentEvent);
	}

	/**
	 * Changes the appearance of this activity to reflect the fact that this activity is now tracking
	 */
	private void updateTrackingUI(){
		boolean isTracking = isTracking();
		textViewIsTracking.setText(isTracking ? trackingStringID : notTrackingStringID);
		nextActivityButton.setEnabled(isTracking);
		stopTrackingButton.setEnabled(isTracking);	

	}

	private void updateAutoComplete(){
		String activityName=editTextEventName.getText().toString();
		String activityLocation=editTextEventLocation.getText().toString();
		if(mActivityNames.add(activityName))
			adapterActivities.add(activityName);
		if(mActivityLocations.add(activityLocation))
			adapterLocations.add(activityLocation);
	}
	
	/**
	 * Updates the database with the given EventEntry. If an event is
	 * created, the event's rowID is updated with the new rowID.
	 
	 * @param event The EventEntry to push to the database. 
	 * @return Whether or not the update occured without error.
	 */
	private boolean updateDatabase(EventEntry event) {
		if (event == null)
			return true;
		event.mName = editTextEventName.getText().toString();
		event.mLocation = editTextEventLocation.getText().toString();
		return mEventManager.updateDatabase(event);
	}

	/**
	 * @return Whether or not an activity is currently being tracked.
	 */
	private boolean isTracking() {
		return currentEvent != null;
	}

	private void initializeAutoComplete() {
		adapterActivities.clear();
		mActivityNames.clear();
		adapterLocations.clear();
		mActivityLocations.clear();
		EventCursor allEventsCursor = mEventManager.fetchAllEvents();
		EventEntry nextEvent;
		while(allEventsCursor.moveToNext()) {
			nextEvent = allEventsCursor.getEvent();
			if (mActivityNames.add(nextEvent.mName))
				adapterActivities.add(nextEvent.mName);
			if(mActivityLocations.add(nextEvent.mLocation))
				adapterLocations.add(nextEvent.mLocation);
		}
	}

	private void focusOnNothing(){
		LinearLayout dummy=(LinearLayout)findViewById(R.id.dummyLayout);
		editTextEventName.clearFocus();
		editTextEventLocation.clearFocus();
		dummy.requestFocus();
	}
}