package edu.berkeley.security.eventtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventGenerator;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

public class Debugging extends Activity {

	private static final String TEST_DATA_PATH = "debug/event_test_data3.txt";
	private static final String datePattern = "MM/dd/yyyy hh:mma";
	private static SimpleDateFormat dateFormatter;
	private static Calendar localCalendar;
	private TextView debugStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debugging);
		dateFormatter = new SimpleDateFormat(datePattern);
		localCalendar = Calendar.getInstance();
		debugStatus = ((TextView) findViewById(R.id.debuggingStatusText));
		((Button) findViewById(R.id.eventDataButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					debugStatus.setText("importing events...");
					importTestEvents();
					debugStatus.setText("importing events... Done");
				} catch (Exception e) {
					debugStatus.setText("Error importing events: " + e);
				}
			}
		});

		((Button) findViewById(R.id.clearDatabaseButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				debugStatus.setText("clearing events...");
				EventManager.getManager().permanentlyDeleteAllEntries();
				debugStatus.setText("clearing events... Done");
			}
		});
		((Button) findViewById(R.id.doMLButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Set<String> eventNamePredictions = EventActivity.mPredictionService
						.getEventNamePredictions();
				String debugText = "Predicted events: ";
				for (String predictedName : eventNamePredictions)
					debugText += String.format('\n' + predictedName);
				debugStatus.setText(debugText);
			}
		});
		((Button) findViewById(R.id.forceRegisterButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				debugStatus.setText("Registration forced.");
				Debugging.this.forceRegister();
			}
		});
		((Button) findViewById(R.id.sendAllEventsButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				debugStatus.setText("Trying to send all events.");
				Networking.sendAllEvents(Debugging.this);
			}
		});
		((Button) findViewById(R.id.pollServerButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				debugStatus.setText("Polling server for Events.");
				Networking.pollServerIfAllowed(Debugging.this);
			}
		});
		((Button) findViewById(R.id.generateDataButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				debugStatus.setText("Generating random events...");
				// Generate events.
				int ONE_DAY = 1000 * 60 * 60 * 24;
				EventGenerator eg = new EventGenerator(ONE_DAY);
				EventManager manager = EventActivity.mEventManager;
				Set<String> tags = manager.getTags();
				EventEntry event;
				for (int eventIndex = 365; eventIndex > 0; eventIndex--) {
					event = eg.generateEvent();
					if (!tags.contains(event.mTag)) {
						manager.addTag(event.mTag);
					}
					manager.updateDatabase(event, false);
				}
				debugStatus.setText("Generating random events... Done");
			}
		});
	}

	private int importTestEvents() throws IOException, ParseException {
		AssetManager assetMgr = getAssets();

		BufferedReader streamReader = new BufferedReader(new InputStreamReader(
				assetMgr.open(TEST_DATA_PATH)));
		String eventLine, gpsLine;
		String[] eventParts, gpsParts;
		EventManager mgr = EventManager.getManager();
		int nSuccessful = 0;
		while ((eventLine = streamReader.readLine()) != null) {
			eventParts = eventLine.split("\t+");
			String tag = "";
			if (eventParts.length >= 5) {
				tag = eventParts[4];
				EventActivity.mEventManager.addTag(tag);
			}
			EventEntry event = mgr.createEvent(eventParts[0], eventParts[1],
					parseDate(eventParts[2]), parseDate(eventParts[3]), false, tag);
			if (event != null) {
				nSuccessful++;
			}

			try {
				gpsLine = streamReader.readLine();
				if (gpsLine.length() == 0)
					continue;

				// this is a String array. each element is in the form of
				// lat,long,time
				gpsParts = gpsLine.split("\t+");

				for (String gps : gpsParts) {
					String[] gpsData = gps.split(",");
					Double latitude = Double.valueOf(gpsData[0]);
					Double longitude = Double.valueOf(gpsData[1]);
					GPSCoordinates coord = new GPSCoordinates(latitude, longitude,
							parseDate(gpsData[2]));
					mgr.addGPSCoordinates(coord, event.mDbRowID);
				}
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			}
		}
		return nSuccessful;
	}

	private long parseDate(String dateTime) throws ParseException {
		localCalendar.setTime(dateFormatter.parse(dateTime));
		return localCalendar.getTimeInMillis();
	}

	private void forceRegister() {
		Networking.sendToServer(ServerRequest.REGISTER, null, this);
	}

}
