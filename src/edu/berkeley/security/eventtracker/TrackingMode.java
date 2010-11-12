package edu.berkeley.security.eventtracker;

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
		updateDatabase(getCurrentEvent());
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
		EventEntry currentEvent = getCurrentEvent();
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
}
