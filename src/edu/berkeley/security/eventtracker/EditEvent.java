package edu.berkeley.security.eventtracker;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

public class EditEvent extends EventActivity {
	private static final int previousEventTextID = R.string.previousActivityText;
	private static final int previousEventDefaultID = R.string.previousActivityDefault;

	private EventEntry currentEvent;
	private EventEntry previousEvent;

	private ArrayList<String> autoCompleteActivities=new ArrayList<String>();
	private ArrayList<String> autoCompleteNotes=new ArrayList<String>();
	private Set<String> mActivityNames = new HashSet<String>();
	private Set<String> mActivityNotes = new HashSet<String>();
	private ArrayAdapter<String> adapterActivities;
	private ArrayAdapter<String> adapterNotes;
	
	private AutoCompleteTextView editTextEventName;
	private AutoCompleteTextView editTextEventNotes;
	private Button previousActivityBar;
	private Button nextActivityButton;
	private Button stopTrackingButton;
	private TextView textViewStartTime;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		editTextEventNotes.setHint(getString(R.string.eventNotesHint));
	}
	
	@Override
	protected int getLayoutResource() {
		return R.layout.edit_event;
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
		editTextEventNotes = (AutoCompleteTextView) findViewById(R.id.editNotes);
		//		TODO uncomment these to disable soft keyboard
		editTextEventName.setInputType(0); 
		editTextEventNotes.setInputType(0);

		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteActivities);
		adapterNotes = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteNotes);

		editTextEventName.setAdapter(adapterActivities);
		editTextEventNotes.setAdapter(adapterNotes);

		editTextEventName.addTextChangedListener(new StartTrackingListener());
		editTextEventNotes.addTextChangedListener(new StartTrackingListener());
	}

	/**
	 * Listens for a text change and creates a new event if one doesn't exist.
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
	protected void initializeToolbar() {
		super.initializeToolbar();
		ImageView toolbarLeftOption = ((ImageView) findViewById(R.id.toolbar_left_option)); 
		toolbarLeftOption.setImageResource(R.drawable.list_icon);
		findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startListEventsActivity();
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		updateAutoComplete();
		updateDatabase(currentEvent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		initializeAutoComplete();
		fillViewWithEventInfo();
		focusOnNothing();
	}
	
	@Override
	protected void refreshState() {
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
	}

	/**
	 * Fills the text entries and views with the correct info based on the
	 * current/previous events.
	 */
	private void fillViewWithEventInfo() {
		if (currentEvent != null) {
			editTextEventName.setText(currentEvent.mName);
			editTextEventNotes.setText(currentEvent.mNotes);
			textViewStartTime.setText(currentEvent.formatColumn(ColumnType.START_TIME));
		} else {
			editTextEventName.setText("");
			editTextEventNotes.setText("");
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

	/**
	 * Changes the appearance of this activity to reflect the fact that this activity is now tracking
	 */
	protected boolean updateTrackingUI(){
		boolean isTracking = super.updateTrackingUI();
		nextActivityButton.setEnabled(isTracking);
		stopTrackingButton.setEnabled(isTracking);
		return isTracking;
	}
	
	/**
	 * Updates the UI using the currentEvent and previousEvent.
	 */
	private void updateUI() {
		updateTrackingUI();
		fillViewWithEventInfo();
	}

	/**
	 * Updates the the AutoComplete adapter with the current name/notes.
	 */
	private void updateAutoComplete(){
		String activityName=editTextEventName.getText().toString();
		String activityNotes=editTextEventNotes.getText().toString();
		if(mActivityNames.add(activityName))
			adapterActivities.add(activityName);
		if(mActivityNotes.add(activityNotes))
			adapterNotes.add(activityNotes);
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
		event.mNotes = editTextEventNotes.getText().toString();
		return mEventManager.updateDatabase(event);
	}

	/**
	 * @return Whether or not an activity is currently being tracked.
	 */
	protected boolean isTracking() {
		return currentEvent != null;
	}

	/**
	 * Refreshes the AutoComplete adapters with all events from the database.
	 */
	private void initializeAutoComplete() {
		adapterActivities.clear();
		mActivityNames.clear();
		adapterNotes.clear();
		mActivityNotes.clear();
		EventCursor allEventsCursor = mEventManager.fetchAllEvents();
		EventEntry nextEvent;
		while(allEventsCursor.moveToNext()) {
			nextEvent = allEventsCursor.getEvent();
			if (mActivityNames.add(nextEvent.mName))
				adapterActivities.add(nextEvent.mName);
			if(mActivityNotes.add(nextEvent.mNotes))
				adapterNotes.add(nextEvent.mNotes);
		}
	}

	/**
	 * Used to switch focus away from any particular UI element.
	 */
	private void focusOnNothing(){
		LinearLayout dummy=(LinearLayout)findViewById(R.id.dummyLayout);
		editTextEventName.clearFocus();
		editTextEventNotes.clearFocus();
		dummy.requestFocus();
	}
}