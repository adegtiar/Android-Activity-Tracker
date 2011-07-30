package edu.berkeley.security.eventtracker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.maps.HelloGoogleMaps;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

public class EditMode extends AbstractEventEdit {

	private EventEntry editingEvent;
	private Button startTimeButton;
	private Button endTimeButton;
	private boolean saveToDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = this.getIntent();

		long rowId = i.getLongExtra(EventKey.ROW_ID.columnName(), -1);
		editingEvent = mEventManager.fetchEvent(rowId);
		if (editingEvent == null)
			this.finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (saveToDB) {
			updateDatabase(editingEvent);
			Networking.sendToServer(ServerRequest.UPDATE, editingEvent,
					EditMode.this);
		}
	}

	@Override
	protected void updateTrackingUI(boolean isTracking) {
		bottomBar.setText(getCurrentEventString());
		textViewIsTracking.setText(R.string.editModeHeader);
	}

	@Override
	protected void initializeBottomBar() {
		bottomBar = (Button) findViewById(R.id.previous_activity_bar);
		bottomBar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
				EditMode.this.startActivityRight(TrackingMode.class);
			}
		});
	}

	@Override
	protected void initializeActivityButtons() {
		super.initializeActivityButtons();

		nextActivityButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				saveToDB = true;
				// propagate the update to the web server(if given permission by
				// user)

				finish();
			}
		});

		stopTrackingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				saveToDB = false;
				finish();
			}
		});
		viewMapButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (editingEvent.getGPSCoordinates().size() == 0) {
					Toast.makeText(getApplicationContext(), "No data yet",
							Toast.LENGTH_SHORT).show();
				} else {
					Intent myIntent = new Intent(EditMode.this,
							HelloGoogleMaps.class);
					myIntent.putExtra("EventData", editingEvent);
					startActivity(myIntent);
				}
			}
		});
		dropDown.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				String tagChosen = parent.getItemAtPosition(position)
						.toString();
				editingEvent.mTag = tagChosen;
				saveToDB = true;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}

	@Override
	protected void initializeTimesUI() {
		startTimeButton = (Button) findViewById(R.id.startTimeButton);
		endTimeButton = (Button) findViewById(R.id.endTimeButton);

		startTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Intent i = new Intent(EditMode.this, TimeDatePicker.class);
				// i.putExtra("Time", editingEvent.mStartTime);
				// startActivityForResult(i, EventKey.START_TIME.ordinal());
				Calendar startCalendar = Calendar.getInstance();
				startCalendar.setTimeInMillis(editingEvent.mStartTime);
				showDateTimeDialog(startCalendar, EventKey.START_TIME.ordinal());
			}
		});
		endTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Intent i = new Intent(EditMode.this, TimeDatePicker.class);
				// i.putExtra("Time", editingEvent.mEndTime);
				// startActivityForResult(i, EventKey.END_TIME.ordinal());
				Calendar endCalendar = Calendar.getInstance();
				endCalendar.setTimeInMillis(editingEvent.mEndTime);
				showDateTimeDialog(endCalendar, EventKey.END_TIME.ordinal());
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode != EventKey.START_TIME.ordinal()
				&& requestCode != EventKey.END_TIME.ordinal())
			return;
		if (intent == null) {
			return;
		}
		Bundle extras = intent.getExtras();
		Long time = extras.getLong("Time");
		if (time != null) {
			EventKey type = EventKey.values()[requestCode];
			switch (type) {
			case START_TIME:
				editingEvent.mStartTime = time;
				break;
			case END_TIME:
				editingEvent.mEndTime = time;
			}
			fillViewWithEventInfo();
		}
	}

	protected void setNameText(String name) {
		if (editingEvent != null) {
			editingEvent.mName = name;
			updateDatabase(editingEvent);

		}
	}

	protected void setNotesText(String notes) {
		if (editingEvent != null) {
			editingEvent.mNotes = notes;
			updateDatabase(editingEvent);
		}
	}

	@Override
	protected void fillViewWithEventInfo() {
		eventNameEditText.setText(editingEvent.mName);
		startTimeButton.setText(editingEvent.formatColumn(EventKey.START_TIME));
		endTimeButton.setText(editingEvent.formatColumn(EventKey.END_TIME));
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.edit_event;
	}

	@Override
	protected void syncToEventFromUI() {
		editingEvent.mName = eventNameEditText.getText().toString();
	}

	/**
	 * @return The text that the current event bar should have, based on the
	 *         currentEvent.
	 */
	protected String getCurrentEventString() {
		String previousActivityLabel = getString(currentEventTextID);
		String previousEventString = currentEvent != null ? currentEvent.mName
				: getString(previousEventDefaultID);
		return previousActivityLabel + " " + previousEventString;
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
		int position = mTagList.indexOf(editingEvent.mTag);
		dropDown.setSelection(position, true);
	}

	/**
	 * Copyright 2010 Lukasz Szmit <devmail@szmit.eu>
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */
	private void showDateTimeDialog(Calendar cal, int requestCode) {
		final int mRequestCode = requestCode;
		// Create the dialog
		final Dialog mDateTimeDialog = new Dialog(this);
		// Inflate the root layout
		final RelativeLayout mDateTimeDialogView = (RelativeLayout) getLayoutInflater()
				.inflate(R.layout.date_time_dialog, null);
		// Grab widget instance
		final DateTimePicker mDateTimePicker = (DateTimePicker) mDateTimeDialogView
				.findViewById(R.id.DateTimePicker);
		mDateTimePicker.datePicker.init(cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
				mDateTimePicker);
		mDateTimePicker.timePicker.setCurrentHour(cal.getTime().getHours());
		mDateTimePicker.timePicker.setCurrentMinute(cal.getTime().getMinutes());
		mDateTimePicker.mCalendar = cal;
		// Check is system is set to use 24h time (this doesn't seem to work as
		// expected though)
		final String timeS = android.provider.Settings.System.getString(
				getContentResolver(),
				android.provider.Settings.System.TIME_12_24);
		final boolean is24h = !(timeS == null || timeS.equals("12"));
		// Update demo TextViews when the "OK" button is clicked
		((Button) mDateTimeDialogView.findViewById(R.id.SetDateTime))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						if (mRequestCode == EventKey.START_TIME.ordinal()) {
							editingEvent.mStartTime = mDateTimePicker.mCalendar
									.getTimeInMillis();
							startTimeButton.setText(editingEvent
									.formatColumn(EventKey.START_TIME));
						}
						if (mRequestCode == EventKey.END_TIME.ordinal()) {
							editingEvent.mEndTime = mDateTimePicker.mCalendar
									.getTimeInMillis();
							endTimeButton.setText(editingEvent
									.formatColumn(EventKey.END_TIME));
						}

						mDateTimeDialog.dismiss();
					}
				});

		// Cancel the dialog when the "Cancel" button is clicked
		((Button) mDateTimeDialogView.findViewById(R.id.CancelDialog))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						// TODO Auto-generated method stub
						mDateTimeDialog.cancel();
					}
				});

		// Reset Date and Time pickers when the "Reset" button is clicked
		((Button) mDateTimeDialogView.findViewById(R.id.ResetDateTime))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						// TODO Auto-generated method stub
						mDateTimePicker.reset();
					}
				});

		// Setup TimePicker
		mDateTimePicker.setIs24HourView(is24h);
		// No title on the dialog window
		mDateTimeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Set the dialog content view
		mDateTimeDialog.setContentView(mDateTimeDialogView);
		// Display the dialog
		mDateTimeDialog.show();
	}

	@Override
	protected Class<?> getLeftActivityClass() {
		return null;
	}

	@Override
	protected Class<?> getRightActivityClass() {
		return ListEvents.class;
	}

	@Override
	protected EventEntry getFocussedEvent() {
		return editingEvent;
	}

}
