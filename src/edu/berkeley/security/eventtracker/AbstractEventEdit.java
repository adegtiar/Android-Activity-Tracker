package edu.berkeley.security.eventtracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

abstract public class AbstractEventEdit extends EventActivity {
	protected static final int previousEventTextID = R.string.previousActivityText;
	protected static final int previousEventDefaultID = R.string.previousActivityDefault;
	protected static final int currentEventTextID = R.string.currentActivityText;
	private static final int VOICE_RECOGNITION_REQUEST_CODE_NAME = 1234;
	private static final int VOICE_RECOGNITION_REQUEST_CODE_NOTES = 5678;
	protected EventEntry currentEvent;
	protected EventEntry previousEvent;

	protected ArrayList<String> autoCompleteActivities = new ArrayList<String>();
	protected ArrayList<String> autoCompleteNotes = new ArrayList<String>();
	protected Set<String> mActivityNames = new HashSet<String>();
	protected Set<String> mActivityNotes = new HashSet<String>();
	protected ArrayAdapter<String> adapterActivities;
	protected ArrayAdapter<String> adapterNotes;

	protected AutoCompleteTextView editTextEventName;
	protected AutoCompleteTextView editTextEventNotes;
	protected Button bottomBar;
	protected Button nextActivityButton;
	protected Button stopTrackingButton;
	protected ImageButton eventVoiceButton;
	protected ImageButton noteVoiceButton;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeEditTexts();
		initializeAutoComplete();
		initializeBottomBar();
		initializeActivityButtons();
		initializeTimesUI();
		editTextEventName.setHint(getString(R.string.eventNameHint));
		editTextEventNotes.setHint(getString(R.string.eventNotesHint));
		initializeVoice();
		
	}



	/**
	 * Initializes the NextActivity and StopTracking buttons.
	 */
	protected void initializeActivityButtons() {
		nextActivityButton = (Button) findViewById(R.id.NextActivityButton);
		stopTrackingButton = (Button) findViewById(R.id.StopTrackingButton);
	}

	/**
	 * Initializes the AutoCompleteTextViews and intializes references to
	 * related views.
	 */
	protected void initializeEditTexts() {
		editTextEventName = (AutoCompleteTextView) findViewById(R.id.editEventName);
		editTextEventNotes = (AutoCompleteTextView) findViewById(R.id.editNotes);
		// TODO uncomment these to disable soft keyboard
//		editTextEventName.setInputType(0);
//		editTextEventNotes.setInputType(0);

		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line,
				autoCompleteActivities);
		adapterNotes = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteNotes);

		editTextEventName.setAdapter(adapterActivities);
		editTextEventNotes.setAdapter(adapterNotes);
	}

	abstract protected void initializeBottomBar();

	abstract protected void initializeTimesUI();

	/**
	 * Initializes the toolbar onClickListeners and intializes references to
	 * toolbar views.
	 */
	protected void initializeToolbar() {
		super.initializeToolbar();
		ImageView toolbarLeftOption = ((ImageView) findViewById(R.id.toolbar_left_option));
		toolbarLeftOption.setImageResource(R.drawable.list_icon);
		findViewById(R.id.toolbar_left_option).setOnClickListener(
				new View.OnClickListener() {

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
		syncToEventFromUI();
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

	protected abstract void syncToEventFromUI();

	/**
	 * Fills the text entries and views with the correct info based on the
	 * current/previous events.
	 */
	protected abstract void fillViewWithEventInfo();

	/**
	 * @return The text that the previous event bar should have, based on the
	 *         previousEvent.
	 */
	protected String getPreviousEventString() {
		String previousActivityLabel = getString(previousEventTextID);
		String previousEventString = previousEvent != null ? previousEvent.mName
				: getString(previousEventDefaultID);
		return previousActivityLabel + " " + previousEventString;
	}

	/**
	 * Changes the appearance of this activity to reflect whether or not an
	 * event is in progress.
	 */
	protected boolean updateTrackingUI() {
		return super.updateTrackingUI();
	}

	/**
	 * Updates the the AutoComplete adapter with the current name/notes.
	 */
	protected void updateAutoComplete() {
		String activityName = editTextEventName.getText().toString();
		String activityNotes = editTextEventNotes.getText().toString();
		if (mActivityNames.add(activityName))
			adapterActivities.add(activityName);
		if (mActivityNotes.add(activityNotes))
			adapterNotes.add(activityNotes);
	}

	/**
	 * Updates the database with the given EventEntry. If an event is created,
	 * the event's rowID is updated with the new rowID.
	 * 
	 * @param event
	 *            The EventEntry to push to the database.
	 * @return Whether or not the update occured without error.
	 */
	protected boolean updateDatabase(EventEntry event) {
		if (event == null)
			return true;
		return mEventManager.updateDatabase(event);
	}

	/**
	 * @return Whether or not an activity is currently being tracked.
	 */
	protected boolean isTracking() {
		return currentEvent != null;
	}

	public EventEntry getCurrentEvent() {
		return currentEvent;
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
		while (allEventsCursor.moveToNext()) {
			nextEvent = allEventsCursor.getEvent();
			if (mActivityNames.add(nextEvent.mName))
				adapterActivities.add(nextEvent.mName);
			if (mActivityNotes.add(nextEvent.mNotes))
				adapterNotes.add(nextEvent.mNotes);
		}
	}
	protected void initializeVoice() {
		eventVoiceButton=(ImageButton)findViewById(R.id.eventVoiceButton);
		noteVoiceButton = (ImageButton) findViewById(R.id.noteVoiceButton);
		  // Check to see if a recognition activity is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0) {
            eventVoiceButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startVoiceRecognitionActivity(VOICE_RECOGNITION_REQUEST_CODE_NAME);
    			}
    		});
            noteVoiceButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startVoiceRecognitionActivity(VOICE_RECOGNITION_REQUEST_CODE_NOTES);
    			}
    		});
        } else {
            eventVoiceButton.setEnabled(false);
            noteVoiceButton.setEnabled(false);
//            speakButton.setText("Recognizer not present");
        }
	}
	 /**
     * Fire an intent to start the speech recognition activity.
     */
    protected void startVoiceRecognitionActivity(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
        startActivityForResult(intent, requestCode);
    }
    
    protected abstract void setNameText(String name);
    protected abstract void setNotesText(String name);
    
    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode != RESULT_OK)
    		 return;
    	  ArrayList<String> matches = data.getStringArrayListExtra(
                  RecognizerIntent.EXTRA_RESULTS);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE_NAME) {
      
            setNameText(matches.get(0));
        }else if(requestCode == VOICE_RECOGNITION_REQUEST_CODE_NOTES){
        	setNotesText(matches.get(0));
        }

        
    }
	/**
	 * Used to switch focus away from any particular UI element.
	 */
	protected void focusOnNothing() {
		LinearLayout dummy = (LinearLayout) findViewById(R.id.dummyLayout);
		editTextEventName.clearFocus();
		editTextEventNotes.clearFocus();
		dummy.requestFocus();
	}
}