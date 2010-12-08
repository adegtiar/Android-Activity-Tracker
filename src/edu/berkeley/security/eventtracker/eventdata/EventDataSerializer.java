package edu.berkeley.security.eventtracker.eventdata;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

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
			serializeAllRowsJSONaData();
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

	private void serializeAllRowsJSONArray() throws JSONException {
		EventManager manager = EventManager.getManager(this);
		EventCursor cursor = manager.fetchSortedEvents();
		JSONArray jsonArray = new JSONArray();
		while (cursor.moveToNext()) {
			jsonArray.put(toJSONObject(cursor.getEvent()));
		}

		setDataResult(jsonArray.toString());
	}

	private void serializeAllRowsJSONaData() throws JSONException {
		setDataResult(getAllRowsSerializedJSONaData(this));
	}

	/**
	 * Serializes all rows in the <tt>EventManager</tt> into a JSON array.
	 * 
	 * @param dbContext the <tt>Context</tt> to use for the <tt>EventManager</tt>.
	 * @return the <tt>String</tt> JSON output.
	 * @throws JSONException
	 */
	public static String getAllRowsSerializedJSONaData(Context dbContext)
			throws JSONException {
		EventManager manager = EventManager.getManager(dbContext);
		EventCursor cursor = manager.fetchSortedEvents();

		JSONObject aData = new JSONObject();
		JSONArray aDataValue = new JSONArray();

		while (cursor.moveToNext()) {
			EventEntry event = cursor.getEvent();
			JSONArray eventRowArray = new JSONArray();
			eventRowArray.put(event.formatColumn(ColumnType.NAME));
			eventRowArray.put(event.formatColumn(ColumnType.START_TIME));
			eventRowArray.put(event.formatColumn(ColumnType.END_TIME));
			eventRowArray.put(event.formatColumn(ColumnType.NOTES));
			aDataValue.put(eventRowArray);
		}
		aData.put("aaData", aDataValue);
		
		return aData.toString();
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
