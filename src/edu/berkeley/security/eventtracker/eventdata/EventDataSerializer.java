package edu.berkeley.security.eventtracker.eventdata;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;

public class EventDataSerializer {
	private static final EventKey[] keysToSerialize = new EventKey[] {
			EventKey.NAME, EventKey.NOTES, EventKey.START_TIME,
			EventKey.END_TIME, EventKey.UPDATE_TIME, EventKey.IS_DELETED };

	public static JSONObject toJSONObject(EventEntry event) {
		JSONObject json = new JSONObject();
		try {
			for (EventKey key : keysToSerialize)
				json.accumulate(key.columnName(), event.getValue(key));
			json.accumulate("gpsCoordinates",
					toJSONArray(event.getGPSCoordinates()));
		} catch (JSONException e) {
			json = null;
		}
		return json;
	}

	private static JSONArray toJSONArray(List<GPSCoordinates> coordsList) {
		JSONArray gpsArray = new JSONArray();
		for (GPSCoordinates coords : coordsList)
			try {
				gpsArray.put(toJSONObject(coords));
			} catch (JSONException e) {
				return null;
			}
		return gpsArray;
	}

	private static JSONObject toJSONObject(GPSCoordinates coords)
			throws JSONException {
		JSONObject jsonCoords = new JSONObject();
		jsonCoords.put("latitude", coords.getLatitude());
		jsonCoords.put("longitude", coords.getLongitude());
		jsonCoords.put("time", coords.getTime());
		return jsonCoords;
	}
}
