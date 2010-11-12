/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.berkeley.security.eventtracker.eventdata;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class GPSDbAdapter extends AbstractDbAdapter {
	public static final String KEY_EVENT_ROWID = "eventRowID";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ROWID = "_id";
	private static final String DATABASE_TABLE = "gpsData";

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public GPSDbAdapter(Context ctx) {
		super(ctx);
	}

	public long createGPSEntry(Long eventRowID, Double latitude,
			Double longitude) {
		
		
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_EVENT_ROWID, eventRowID);
			initialValues.put(KEY_LATITUDE, latitude);
			initialValues.put(KEY_LONGITUDE, longitude);
			long toBeReturned=mDb.insert(DATABASE_TABLE, null, initialValues);
			//TODO delete this later. just for debugging purposes
			 Cursor c=this.getGPSCoordinates(eventRowID);
			 ArrayList<GPSCoordinates> list = new ArrayList<GPSCoordinates>();
			

				if (c.getCount() > 0) {
					while (c.moveToNext()) {
						double alatitude = c.getDouble(c
								.getColumnIndex((GPSDbAdapter.KEY_LATITUDE)));
						double alongitude = c.getDouble(c
								.getColumnIndex((GPSDbAdapter.KEY_LONGITUDE)));
						list.add(new GPSCoordinates(alatitude, alongitude));

					}
				}
			 
			 
			 
			return toBeReturned;
		
	}

	/**
	 * Delete the activity with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(Long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor positioned at the note that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchGPS(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_EVENT_ROWID, KEY_LONGITUDE, KEY_LATITUDE }, KEY_ROWID + "="
				+ rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public boolean updateGPSEntry(long rowId, long eventRowID,
							double longitude, double latitude) {
		ContentValues args = new ContentValues();
		args.put(KEY_EVENT_ROWID, eventRowID);
		args.put(KEY_LONGITUDE, longitude);
		args.put(KEY_LATITUDE, latitude);
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor getGPSCoordinates(long eventRowID) {

		// String selection="SELECT * FROM "+ DATABASE_TABLE+ " WHERE "
		// +KEY_EVENT_ROWID +"=" + eventRowID;
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_EVENT_ROWID, KEY_LONGITUDE, KEY_LATITUDE }, KEY_EVENT_ROWID
				+ "=" + eventRowID, null, null, null, null);

	}

}