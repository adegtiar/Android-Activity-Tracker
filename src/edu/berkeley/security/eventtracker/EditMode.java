package edu.berkeley.security.eventtracker;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

public class EditMode extends AbstractEventEdit {
	protected static final int currentEventTextID = R.string.currentActivityText;
	protected static final int currentEventDefaultID = R.string.previousActivityDefault;
	
	private EventEntry editingEvent;
	private Button startTimeButton;
	private Button endTimeButton;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.berkeley.security.eventtracker.AbstractEventEdit#onCreate(android
	 * .os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = this.getIntent();
		long rowId = i.getLongExtra(ColumnType.ROWID.getColumnName(), -1);
		editingEvent = mEventManager.fetchEvent(rowId);
		if (editingEvent == null)
			this.finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		updateDatabase(editingEvent);
	}

	@Override
	protected boolean updateTrackingUI() {
		if (isTracking())
			bottomBar.setText(getPreviousEventString());
		return false;
	}

	@Override
	protected void initializeBottomBar() {
		bottomBar = (Button) findViewById(R.id.previous_activity_bar);
		bottomBar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	protected void initializeTimesUI() {
		startTimeButton = (Button) findViewById(R.id.startTimeButton);
		endTimeButton = (Button) findViewById(R.id.endTimeButton);
		
		startTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(EditMode.this, TimeDatePicker.class);
				i.putExtra("Time",editingEvent.mStartTime);
				startActivityForResult(i, ColumnType.START_TIME.ordinal());


			}
		});
		endTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(EditMode.this, TimeDatePicker.class);
				i.putExtra("Time",editingEvent.mEndTime);
				startActivityForResult(i, ColumnType.END_TIME.ordinal());
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, 
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Bundle extras = intent.getExtras();
		long time=extras.getLong("Time");
		ColumnType type = ColumnType.values()[requestCode];
		switch(type) {
		case START_TIME:
			editingEvent.mStartTime = time;
			break;
		case END_TIME:
			editingEvent.mEndTime = time;
		}
		fillViewWithEventInfo();
	}

	@Override
	protected void fillViewWithEventInfo() {
		editTextEventName.setText(editingEvent.mName);
		editTextEventNotes.setText(editingEvent.mNotes);
		startTimeButton.setText(editingEvent
				.formatColumn(ColumnType.START_TIME));
		startTimeButton.setText(editingEvent.formatColumn(ColumnType.END_TIME));
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.edit_event;
	}

	@Override
	protected void syncToEventFromUI() {
		editingEvent.mName = editTextEventName.getText().toString();
		editingEvent.mNotes = editTextEventNotes.getText().toString();
	}

}
