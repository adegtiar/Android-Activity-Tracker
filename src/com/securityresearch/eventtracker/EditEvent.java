package com.securityresearch.eventtracker;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class EditEvent extends Activity {
	static ArrayList<String> autoCompleteActivities=new ArrayList<String>();
	static ArrayList<String> autoCompleteLocations=new ArrayList<String>();
	static final int TIME_START_DIALOG_ID = 0;
	static final int TIME_END_DIALOG_ID = 1;

	private EventDbAdapter mDbHelper;
	private ArrayAdapter<String> adapterActivities;
	private ArrayAdapter<String> adapterLocations;
	private AutoCompleteTextView textViewEvent;
	private AutoCompleteTextView textViewLocation;

	private static final int TIME_START=0;
	private static final int TIME_END=1;
	private long startTime=0;
	private long endTime=Long.MAX_VALUE;

	private Button mPickStartTime;
	private Button mPickEndTime;


	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ViewStub v =(ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(R.layout.editevent);
		v.inflate();
		((ImageView) findViewById(R.id.toolbar_left_option)).setImageResource(R.drawable.list);

		findViewById(R.id.toolbar_right_option).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(EditEvent.this, Settings.class);
				startActivity(settingsIntent);
			}
		});

		findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(EditEvent.this, ListEvents.class);
				startActivity(settingsIntent);
			}
		});


		DateFormat dateFormat  = android.text.format.DateFormat.getDateFormat(this);
		((TextView) findViewById(R.id.toolbar_date)).setText(dateFormat.format(new Date()));

		initializeButtonTimes();
		setButtonTimes();
		mDbHelper = new EventDbAdapter(this);
		mDbHelper.open();

		textViewEvent = (AutoCompleteTextView) findViewById(R.id.editEvent);
		textViewEvent.setInputType(0);
		textViewLocation = (AutoCompleteTextView) findViewById(R.id.editLocation);
		textViewLocation.setInputType(0);
		adapterActivities = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteActivities);
		adapterLocations = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoCompleteLocations);

		textViewEvent.setAdapter(adapterActivities);
		textViewLocation.setAdapter(adapterLocations);


		initializeAutoComplete();
		Button confirmButton = (Button) findViewById(R.id.confirm);
		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {

				updateAutoComplete();
				updateDatabase();
				textViewEvent.setText("");
				textViewLocation.setText("");

				mDbHelper.fetchCurrentlyRunningEvents();
			}

		});
	}

	/*
	@Override
	protected void onResume() {
		super.onResume();
		initializeButtonTimes();
	}
	*/

	private void updateDatabase(){

		textViewEvent = (AutoCompleteTextView) findViewById(R.id.editEvent);
		textViewLocation = (AutoCompleteTextView) findViewById(R.id.editLocation);
		String activity=textViewEvent.getText().toString();
		String location=textViewLocation.getText().toString();
		mDbHelper.createEvent(activity, location, startTime, endTime);
		reinitializeTimes();
		initializeButtonTimes();
		
		

	}

	private void updateAutoComplete(){
		AutoCompleteTextView textViewActivity = (AutoCompleteTextView) findViewById(R.id.editEvent);
		AutoCompleteTextView textViewLocation = (AutoCompleteTextView) findViewById(R.id.editLocation);
		String activity=textViewActivity.getText().toString();
		String location=textViewLocation.getText().toString();
		if(!mDbHelper.getEvents().contains(activity))
			adapterActivities.add(activity);
		if(!mDbHelper.getLocations().contains(location))
			adapterLocations.add(location);
	}

	private void initializeAutoComplete(){

		for(String event: mDbHelper.getEvents()){
			adapterActivities.add(event);
		}
		for(String location: mDbHelper.getLocations()){
			adapterLocations.add(location);
		}

	}

	private void reinitializeTimes(){
		startTime=0;
		endTime=Long.MAX_VALUE;
	}
	private void initializeButtonTimes(){
		
		
		Button startTimeButton=(Button) findViewById(R.id.startTimeButton);
		Button endTimeButton=(Button) findViewById(R.id.endTimeButton);
		Calendar c=Calendar.getInstance();
		if(startTime ==0){
		startTime=c.getTimeInMillis();
		startTimeButton.setText(getTime(startTime));
		}
		if(endTime==Long.MAX_VALUE){
			endTimeButton.setText("Unspecified");
			
		}
	}

	private String getTime(long time){
		Calendar c=Calendar.getInstance();
		c.setTimeInMillis(time);
		Date myDate=c.getTime();

		return DateFormat.getDateTimeInstance().format(myDate);

	}

	private void setButtonTimes(){

		mPickStartTime = (Button) findViewById(R.id.startTimeButton);
		mPickEndTime = (Button) findViewById(R.id.endTimeButton);
		// add a click listener to the button
		mPickStartTime.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(EditEvent.this, TimeDatePicker.class);
				i.putExtra("Time",startTime);
				startActivityForResult(i, TIME_START);


			}
		});
		mPickEndTime.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(EditEvent.this, TimeDatePicker.class);
				i.putExtra("Time", endTime);
				startActivityForResult(i, TIME_END);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, 
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Bundle extras = intent.getExtras();
		long time=extras.getLong("Time");
		switch(requestCode) {
		case TIME_START:
			startTime=time;
			mPickStartTime.setText(getTime(startTime));
			break;
		case TIME_END:
			endTime=time;
			mPickEndTime.setText(getTime(endTime));
		}
	}
}