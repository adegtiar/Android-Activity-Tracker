package edu.berkeley.security.eventtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

public class Debugging extends Activity {
	private static final String TEST_DATA_PATH = "debug/event_test_data.txt";
	private static final String datePattern = "MM/dd/yyyy hh:mma";
	private static SimpleDateFormat dateFormatter;
	private static Calendar localCalendar;
	private TextView debugStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debugging);
		dateFormatter = new SimpleDateFormat(datePattern);
		localCalendar = Calendar.getInstance();
		debugStatus = ((TextView) findViewById(R.id.debuggingStatusText));
		((Button) findViewById(R.id.eventDataButton))
				.setOnClickListener(new OnClickListener() {

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

		((Button) findViewById(R.id.clearDatabaseButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						debugStatus.setText("clearing events...");
						EventManager.getManager().deleteAllEntries();
						debugStatus.setText("clearing events... Done");
					}
				});
		((Button) findViewById(R.id.tryRegisterButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Networking.registerIfNeeded(Debugging.this);
						debugStatus.setText("Registration attempted.");
					}
				});
		((Button) findViewById(R.id.forceRegisterButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						debugStatus.setText("Registration forced.");
						Debugging.this.forceRegister();
					}
				});
		((Button) findViewById(R.id.sendAllEventsButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						debugStatus.setText("Trying to send all events.");
						Networking.sendAllEvents(Debugging.this);
					}
				});
		((Button) findViewById(R.id.resetPasswordButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						debugStatus
								.setText("Password reset not yet implemented. Plz implement me, Kyle.");
					}
				});
	}

	private int importTestEvents() throws IOException, ParseException {
		AssetManager assetMgr = getAssets();

		BufferedReader streamReader = new BufferedReader(new InputStreamReader(
				assetMgr.open(TEST_DATA_PATH)));
		String eventLine;
		String[] eventParts;
		EventManager mgr = EventManager.getManager();
		int nSuccessful = 0;
		while ((eventLine = streamReader.readLine()) != null) {
			eventParts = eventLine.split("\t+");
			if (mgr.createEvent(eventParts[0], eventParts[1],
					parseDate(eventParts[2]), parseDate(eventParts[3])) != null)
				nSuccessful++;
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
