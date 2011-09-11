package edu.berkeley.security.eventtracker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

abstract public class AbstractEventEdit extends EventActivity {

	protected static final int previousEventTextID = R.string.previousActivityText;
	protected static final int previousEventDefaultID = R.string.previousActivityDefault;
	protected static final int currentEventTextID = R.string.currentActivityText;

	protected EventEntry currentEvent;
	protected EventEntry previousEvent;

	protected ArrayList<String> autoCompleteActivities = new ArrayList<String>();
	protected ArrayAdapter<String> adapterActivities;

	protected AutoCompleteTextView eventNameEditText;
	protected Button eventNotesButton;
	protected Button bottomBar;
	protected Button nextActivityButton;
	protected Button stopTrackingButton;
	protected Button newTagButton;
	protected ImageView viewMapButton;
	protected ImageButton eventVoiceButton;

	protected Spinner dropDown;
	protected LinkedHashSet<String> mTagSet;
	protected ArrayList<String> mTagList;

	protected boolean justResumed;

	private static final int VOICE_RECOGNITION_REQUEST_CODE_NAME = 1234;
	private static final int VOICE_RECOGNITION_REQUEST_CODE_NOTES = 5678;
	private static final int DIALOG_ENTER_TAG = 8;
	protected static final int DIALOG_NOTE_ENTRY = 9;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeEditTexts();
		initializeBottomBar();
		initializeActivityButtons();
		initializeTimesUI();
		eventNameEditText.setHint(getString(R.string.eventNameHint));
		initializeVoice();
	}

	/**
	 * Initializes the NextActivity and StopTracking buttons. Also, now it
	 * Initializes the viewMap Button and others!
	 */
	protected void initializeActivityButtons() {
		nextActivityButton = (Button) findViewById(R.id.NextActivityButton);
		stopTrackingButton = (Button) findViewById(R.id.StopTrackingButton);
		viewMapButton = (ImageView) findViewById(R.id.viewMapButton);
		newTagButton = (Button) findViewById(R.id.tag_button);
		dropDown = (Spinner) findViewById(R.id.tagSpinner);
		dropDown.setPrompt("Select a tag");
		eventNotesButton = (Button) findViewById(R.id.notes_button);
		eventNotesButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_NOTE_ENTRY, new Bundle());
			}
		});

	}

	/**
	 * Initializes the AutoCompleteTextViews and initializes references to
	 * related views.
	 */
	protected void initializeEditTexts() {
		eventNameEditText = (AutoCompleteTextView) findViewById(R.id.editEventName);
		// uncomment these to disable soft keyboard
		// editTextEventName.setInputType(0);
		// editTextEventNotes.setInputType(0);

		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteActivities);

		eventNameEditText.setAdapter(adapterActivities);

	}

	protected abstract void initializeTags();

	abstract protected void initializeBottomBar();

	abstract protected void initializeTimesUI();

	@Override
	protected void onPause() {
		super.onPause();
		syncToEventFromUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		justResumed = true;

		initializeTags();
		fillViewWithEventInfo();
		focusOnNothing();
		newTagButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_ENTER_TAG);
			}
		});
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

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// do nothing to not conflict with loading from the DB
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// do nothing - loading from the DB quick enough
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
	 * Updates the database with the given EventEntry. If an event is created,
	 * the event's rowID is updated with the new rowID.
	 * 
	 * @param event
	 *            The EventEntry to push to the database.
	 * @return Whether or not the update occured without error.
	 */
	protected boolean updateDatabase(EventEntry event) {
		if (event == null) {
			return true;
		}
		// Networking.sendData(event);
		return mEventManager.updateDatabase(event, false);
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

	@Override
	protected void onPredictionServiceConnected() {
		initializeAutoComplete();
	}

	/**
	 * Refreshes the AutoComplete adapters with all events from the database.
	 */
	private void initializeAutoComplete() {
		autoCompleteActivities.clear();
		// Add predicted events in order of likelihood
		autoCompleteActivities.addAll(mPredictionService.getEventNamePredictions());

		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteActivities);
		eventNameEditText.setAdapter(adapterActivities);
	}

	protected void initializeVoice() {
		eventVoiceButton = (ImageButton) findViewById(R.id.eventVoiceButton);

		// Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0) {
			eventVoiceButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startVoiceRecognitionActivity(VOICE_RECOGNITION_REQUEST_CODE_NAME);
				}
			});

		} else {
			eventVoiceButton.setEnabled(false);
			// speakButton.setText("Recognizer not present");
		}
	}

	/**
	 * Fire an intent to start the speech recognition activity.
	 */
	protected void startVoiceRecognitionActivity(int requestCode) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "");
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
		if (resultCode != RESULT_OK)
			return;
		ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE_NAME) {
			setNameText(matches.get(0));
		} else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE_NOTES) {
			setNotesText(matches.get(0));
		}

	}

	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {

		case DIALOG_ENTER_TAG:
			// This example shows how to add a custom layout to an AlertDialog
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.alert_dialog_tag_entry, null);
			return new AlertDialog.Builder(AbstractEventEdit.this)

					.setTitle(R.string.alert_dialog_tag_entry)
					.setView(textEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int whichButton) {

									EditText tagEditText = (EditText) textEntryView
											.findViewById(R.id.tag_edit);
									String tag = tagEditText.getText().toString();
									if (tag.length() > 0)
										EventActivity.mEventManager.addTag(tag);
									initializeTags();
									tagEditText.setText("");
									/* User entered in a new tag. Do stuff here */

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		case DIALOG_NOTE_ENTRY:
			// This example shows how to add a custom layout to an AlertDialog
			LayoutInflater ne_factory = LayoutInflater.from(this);
			final View noteEntryView = ne_factory.inflate(R.layout.alert_dialog_note_entry, null);
			final EditText noteEditText = (EditText) noteEntryView.findViewById(R.id.notes_edit);
			final EventEntry eventInFocus = getFocussedEvent();
			noteEditText.setText(eventInFocus.mNotes);
			return new AlertDialog.Builder(this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.alert_dialog_notes_title)
					.setView(noteEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int whichButton) {

									String notes = noteEditText.getText().toString();

									eventInFocus.mNotes = notes;
									syncToEventFromUI();
									updateDatabase(eventInFocus);
								}

							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		default:
			return super.onCreateDialog(id, bundle);
		}
	}

	/**
	 * Used to switch focus away from any particular UI element.
	 */
	protected void focusOnNothing() {
		LinearLayout dummy = (LinearLayout) findViewById(R.id.dummyLayout);
		eventNameEditText.clearFocus();
		dummy.requestFocus();
	}

	protected abstract EventEntry getFocussedEvent();

}