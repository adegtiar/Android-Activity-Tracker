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
import edu.berkeley.security.eventtracker.eventdata.EventManager;

public class Debugging extends Activity {
	private static final String TEST_DATA_PATH = "debug/event_test_data.txt";
	private static final String datePattern = "MM/dd/yyyy hh:mma";
	private static SimpleDateFormat dateFormatter;
	private static Calendar localCalendar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debugging);
		dateFormatter = new SimpleDateFormat(datePattern);
		localCalendar = Calendar.getInstance();
		((Button) findViewById(R.id.eventDataButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							importTestEvents();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});

		((Button) findViewById(R.id.clearDatabaseButton))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						EventManager.getManager().deleteAllEntries();
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

}
