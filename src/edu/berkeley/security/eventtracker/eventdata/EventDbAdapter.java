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
		ROW_ID("_id", ColumnType.INTEGER, "primary key autoincrement"), NAME("name",
				ColumnType.TEXT), NOTES("notes", ColumnType.TEXT), START_TIME("startTime",
				ColumnType.LONG), END_TIME("endTime", ColumnType.LONG), UPDATE_TIME("updateTime",
				ColumnType.LONG), UUID("uuid", ColumnType.TEXT), IS_DELETED("isDeleted",
				ColumnType.INTEGER, "DEFAULT 0"), RECEIVED_AT_SERVER("receivedAtServer",
				ColumnType.INTEGER, "DEFAULT 0"), TAG("tag", ColumnType.TEXT);

		private String mColumnName;
		private ColumnType mColType;
		private String mExtraCreateText;

		private EventKey(String columnName, ColumnType type) {
			mColumnName = columnName;
			mColType = type;
			mExtraCreateText = " not null";
		}

		private EventKey(String columnName, ColumnType type, String extraOptions) {
			mColumnName = columnName;
			mColType = type;
			mExtraCreateText = " not null " + extraOptions;
		}

		public String columnName() {
			return mColumnName;
		}

		public String getRowCreateString() {
			return mColumnName + " " + mColType + mExtraCreateText;
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

	private enum ColumnType {
		INTEGER, TEXT, LONG;
	};

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
	 * notes, and startTime.
	 */
	public Long createEvent(String eventName, String notes, long startTime, long endTime,
			String uuid, boolean receivedAtServer, String tag) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(EventKey.NAME.columnName(), eventName);
		initialValues.put(EventKey.NOTES.columnName(), notes);
		initialValues.put(EventKey.START_TIME.columnName(), startTime);
		initialValues.put(EventKey.END_TIME.columnName(), endTime);
		initialValues.put(EventKey.UPDATE_TIME.columnName(), Calendar.getInstance()
				.getTimeInMillis());
		initialValues.put(EventKey.UUID.columnName(), uuid);
		initialValues.put(EventKey.RECEIVED_AT_SERVER.columnName(), receivedAtServer ? 1 : 0);
		initialValues.put(EventKey.TAG.columnName(), tag);
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the activity with the given rowId.
	 * 
	 * @param rowId
	 *            id of note to delete.
	 * @return true if deleted, false otherwise.
	 */
	public boolean deleteEvent(Long rowId) {
		return mDb.delete(DATABASE_TABLE, EventKey.ROW_ID.columnName() + "=" + rowId, null) > 0;
	}

	public boolean markDeleted(long rowId) {
		ContentValues args = new ContentValues();
		args.put(EventKey.IS_DELETED.columnName(), 1);
		args.put(EventKey.RECEIVED_AT_SERVER.columnName(), 0);
		return mDb.update(DATABASE_TABLE, args, EventKey.ROW_ID.columnName() + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all events in the database.
	 * 
	 * @return Cursor over all notes.
	 */
	public Cursor fetchAllEvents() {
		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), null, null, null, null, null);
	}

	/**
	 * Return a Cursor over the list of all events in the database.
	 * 
	 * @return Cursor over all notes.
	 */
	public Cursor fetchUndeletedEvents() {
		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), EventKey.IS_DELETED.columnName()
				+ " = 0", null, null, null, null);
	}

	/**
	 * Return a Cursor over the list of event that correspond to today. Sorted
	 * by end Time.
	 * 
	 * @return Cursor.
	 */
	public Cursor fetchSortedEvents() {
		String orderBy = EventKey.START_TIME.columnName() + " DESC";
		String limitClause = "20";
		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), EventKey.IS_DELETED.columnName()
				+ "=0", null, null, null, orderBy, limitClause);
	}

	/**
	 * Return a Cursor over the list of all events in the database that are not
	 * yet on the web server.
	 * 
	 * @return Cursor over all events.
	 */
	public Cursor fetchPhoneOnlyEvents() {
		return mDb.query(true, DATABASE_TABLE, EventKey.columnNames(), String.format(
				"%s = ? AND %s > ?", EventKey.RECEIVED_AT_SERVER.columnName(),
				EventKey.END_TIME.columnName()), new String[] { "0", "0" }, null, null, null, null);
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

		Cursor mCursor = mDb.query(true, DATABASE_TABLE, EventKey.columnNames(),
				EventKey.ROW_ID.columnName() + "=" + rowId, null, null, null, null, null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor;
	}

	public Cursor fetchEvent(String uuid) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, EventKey.columnNames(),
				EventKey.UUID.columnName() + "=?", new String[] { uuid }, null, null, null, null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor;
	}

	public Cursor fetchEvents(String name) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, EventKey.columnNames(),
				EventKey.NAME.columnName() + "=?", new String[] { name }, null, null, null, null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor;
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

	public boolean updateEvent(Long rowId, String title, String notes, Long startTime,
			Long endTime, String uuid, boolean isDeleted, boolean recievedAtServer, String tag) {
		ContentValues args = new ContentValues();
		args.put(EventKey.NAME.columnName(), title);
		args.put(EventKey.NOTES.columnName(), notes);
		args.put(EventKey.START_TIME.columnName(), startTime);
		args.put(EventKey.END_TIME.columnName(), endTime);
		args.put(EventKey.UPDATE_TIME.columnName(), Calendar.getInstance().getTimeInMillis());
		args.put(EventKey.UUID.columnName(), uuid);
		args.put(EventKey.IS_DELETED.columnName(), isDeleted ? 1 : 0);
		args.put(EventKey.RECEIVED_AT_SERVER.columnName(), recievedAtServer ? 1 : 0);
		args.put(EventKey.TAG.columnName(), tag);
		return mDb.update(DATABASE_TABLE, args, EventKey.ROW_ID.columnName() + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of events that begin after startTime and
	 * before endTime
	 * 
	 * @return Cursor.
	 */
	public Cursor fetchSortedEvents(long startTime, long endTime) {
		String orderBy = EventKey.START_TIME.columnName() + " DESC";

		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), EventKey.IS_DELETED.columnName()
				+ "=0" + " AND " + EventKey.START_TIME.columnName() + " > " + startTime + " AND "
				+ EventKey.START_TIME.columnName() + " < " + endTime, null, null, null, orderBy,
				null);
	}

	/**
	 * Return a Cursor over the list of events that begin before startTime
	 * 
	 * @return Cursor.
	 */
	public Cursor fetchSortedEventsBeforeDate(long startTime) {
		String orderBy = EventKey.START_TIME.columnName() + " DESC";

		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), EventKey.IS_DELETED.columnName()
				+ "=0" + " AND " + EventKey.START_TIME.columnName() + " < " + startTime, null,
				null, null, orderBy, null);
	}

	/**
	 * Return a Cursor over the list of events that begin after startTime
	 * 
	 * @return Cursor.
	 */
	public Cursor fetchSortedEventsAfterDate(long startTime) {
		String orderBy = EventKey.START_TIME.columnName() + " ASC";

		return mDb.query(DATABASE_TABLE, EventKey.columnNames(), EventKey.IS_DELETED.columnName()
				+ "=0" + " AND " + EventKey.START_TIME.columnName() + " > " + startTime, null,
				null, null, orderBy, null);
	}

}
