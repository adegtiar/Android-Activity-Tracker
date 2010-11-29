package edu.berkeley.security.eventtracker;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;

public class EventDataSerializer extends Activity {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			serializeAllRowsJSON();
		} catch (Exception e) {

		}
		finish();
	}

	private void serializeAllRowsString() {
		EventManager manager = EventManager.getManager(this);
		EventCursor cursor = manager.fetchSortedEvents();
		EventEntry nextEvent;
		StringBuilder stringData = new StringBuilder();
		while (cursor.moveToNext()) {
			nextEvent = cursor.getEvent();
			stringData.append(nextEvent.toString());
			stringData.append('\n');
		}
		setDataResult(stringData.toString());
	}

	private void serializeAllRowsJSON() throws JSONException {
		EventManager manager = EventManager.getManager(this);
		EventCursor cursor = manager.fetchSortedEvents();
		JSONArray jsonArray = new JSONArray();
		while (cursor.moveToNext()) {
			jsonArray.put(toJSONObject(cursor.getEvent()));
		}
		// this is the production json output
		setDataResult(jsonArray.toString());

		// this is the debugging jsonArray output
		// setDataResult(jsonArray.toString(5));
	}

	private JSONObject toJSONObject(EventEntry event) {
		JSONObject json = new JSONObject();
		try {
			json.accumulate("name", event.mName);
			json.accumulate("notes", event.mNotes);
			json.accumulate("startTime", event.mStartTime);
			json.accumulate("endTime", event.mEndTime);
			json.accumulate("gpsCoordinates", toJSONArray(event
					.getGPSCoordinates()));
		} catch (JSONException e) {
			json = null;
		}
		return json;
	}

	private JSONArray toJSONArray(List<GPSCoordinates> coordsList) {
		JSONArray gpsArray = new JSONArray();
		for (GPSCoordinates coords : coordsList)
			try {
				gpsArray.put(toJSONObject(coords));
			} catch (JSONException e) {
				return null;
			}
		return gpsArray;
	}

	private JSONObject toJSONObject(GPSCoordinates coords) throws JSONException {
		JSONObject jsonCoords = new JSONObject();
		jsonCoords.put("latitude", coords.getLongitude());
		jsonCoords.put("longitude", coords.getLongitude());
		return jsonCoords;
	}

	private void setDataResult(String data) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("mSerializedData", data);
		resultIntent.putExtra("maybeLaunchedExternally", true);
		setResult(RESULT_OK, resultIntent);
	}
}
