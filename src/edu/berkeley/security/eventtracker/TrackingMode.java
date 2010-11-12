package edu.berkeley.security.eventtracker;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

public class TrackingMode extends AbstractEventEdit {
	private TextView textViewStartTime;

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
	protected boolean updateTrackingUI() {
		boolean isTracking = super.updateTrackingUI();
		nextActivityButton.setEnabled(isTracking);
		stopTrackingButton.setEnabled(isTracking);
		return isTracking;
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

	/**
	 * Listens for a text change and creates a new event if one doesn't exist.
	 */
	private class StartTrackingListener implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() != 0 && currentEvent == null) {
				currentEvent = new EventEntry();
				updateDatabase(currentEvent);
				updateStartTimeUI();
				updateTrackingUI();
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
}
