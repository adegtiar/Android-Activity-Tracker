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
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class EventDbAdapter extends AbstractDbAdapter {

	/**
	 * An enumeration of column key names in the event table.
	 */
	public enum EventKey {
		ROWID("_id"), NAME("eventname"), NOTES("notes"), START_TIME("startTime"), END_TIME(
				"endTime"), UPDATE_TIME("updateTime"), UUID("uuid"), RECEIVED_AT_SERVER(
				"receivedAtServer");

		private String mColumnName;

		private EventKey(String columnName) {
			mColumnName = columnName;
		}

		public String columnName() {
			return mColumnName;
		}

		public static String[] columnNames() {
			EventKey[] keys = EventKey.values();
			String[] columnNames = new String[keys.length];
			for (int i = 0; i < keys.length; i++)
				columnNames[i] = keys[i].columnName();
			return columnNames;
		}

		static EventKey fromColumnName(String columnName) {
			for (EventKey eventKey : EventKey.values())
				if (eventKey.mColumnName.equals(columnName))
					return eventKey;
			return null;
		}
	}

	private static final String DATABASE_TABLE = "eventData";

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public EventDbAdapter(Context ctx) {
		super(ctx);
	}

	/**
	 * Create a new entry in the database corresponding to the given eventName,
	 * notes, and startTime
	 */
	public Long createEvent(String eventName, String notes, long startTime,
			long endTime, String uuid, boolean receivedAtServer) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(EventKey.NAME.columnName(), eventName);
		initialValues.put(EventKey.NOTES.columnName(), notes);
		initialValues.put(EventKey.START_TIME.columnName(), startTime);
		initialValues.put(EventKey.END_TIME.columnName(), endTime);
		initialValues.put(EventKey.UPDATE_TIME.columnName(), Calendar
				.getInstance().getTimeInMillis());
		initialValues.put(EventKey.UUID.columnName(), uuid);
		initialValues.put(EventKey.RECEIVED_AT_SERVER.columnName(),
				receivedAtServer ? 1 : 0);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the activity with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEvent(Long rowId) {

		return mDb.delete(DATABASE_TABLE, EventKey.ROWID.columnName() + "="
				+ rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all events in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllEvents() {
		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), null, null,
				null, null, null);
	}

	/**
	 * Return a Cursor over the list of all events in the database that are not
	 * yet on the web server
	 * 
	 * @return Cursor over all events
	 */
	public Cursor fetchPhoneOnlyEvents() {

		return mDb.query(true, DATABASE_TABLE, EventKey.columnNames(),
				EventKey.RECEIVED_AT_SERVER.columnName() + "=" + 0, null, null,
				null, null, null);
	}

	/**
	 * Return a Cursor over the list of event that correspond to today. Sorted
	 * by end Time.
	 * 
	 * @return Cursor
	 */
	public Cursor fetchSortedEvents() {
		String orderBy = EventKey.START_TIME.columnName() + " DESC";
		String limitClause = "20";
		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), null, null,
				null, null, orderBy, limitClause);
	}

	/**
	 * Return a Cursor positioned at the note that matches the given rowId.
	 * 
	 * @param rowId
	 *            id of note to retrieve.
	 * @return Cursor positioned to matching note, if found.
	 * @throws SQLException
	 *             if note could not be found/retrieved.
	 */
	public Cursor fetchEvent(long rowId) throws SQLException {

		Cursor mCursor = mDb.query(true, DATABASE_TABLE,
				EventKey.columnNames(), EventKey.ROWID.columnName() + "="
						+ rowId, null, null, null, null, null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor;
	}

	public ArrayList<String> getEvents() {
		ArrayList<String> toReturn = new ArrayList<String>();
		Cursor cursor = this.fetchAllEvents();
		if (cursor == null) {
			return null;
		}
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				String activity = cursor.getString(cursor
						.getColumnIndex(EventKey.NAME.columnName()));
				if (!toReturn.contains(activity))
					toReturn.add(activity);
			}
		}

		cursor.close();
		return toReturn;
	}

	public ArrayList<String> getNotes() {
		ArrayList<String> toReturn = new ArrayList<String>();
		Cursor cursor = this.fetchAllEvents();
		if (cursor == null) {
			return null;
		}
		if (cursor.getCount() > 0) {

			while (cursor.moveToNext()) {
				String notes = cursor.getString(cursor
						.getColumnIndex(EventKey.NOTES.columnName()));
				if (!toReturn.contains(notes))
					toReturn.add(notes);
			}
		}

		cursor.close();
		return toReturn;
	}

	/**
	 * Update the event using the details provided.
	 * 
	 * @param rowId
	 *            id of event to update.
	 * @param title
	 *            value to set event title to.
	 * @param notes
	 *            value to set event notes to.
	 * @param startTime
	 *            value to set event start time to.
	 * @param endTime
	 *            value to set event end time to to.
	 * 
	 * @return true if the note was successfully updated, false otherwise.
	 */

	public boolean updateEvent(Long rowId, String title, String notes,
			Long startTime, Long endTime, String uuid, boolean recievedAtServer) {
		ContentValues args = new ContentValues();
		args.put(EventKey.NAME.columnName(), title);
		args.put(EventKey.NOTES.columnName(), notes);
		args.put(EventKey.START_TIME.columnName(), startTime);
		args.put(EventKey.END_TIME.columnName(), endTime);
		args.put(EventKey.UPDATE_TIME.columnName(), Calendar.getInstance()
				.getTimeInMillis());
		args.put(EventKey.UUID.columnName(), uuid);
		args.put(EventKey.RECEIVED_AT_SERVER.columnName(), recievedAtServer ? 1
				: 0);
		return mDb.update(DATABASE_TABLE, args, EventKey.ROWID.columnName()
				+ "=" + rowId, null) > 0;
	}

}
