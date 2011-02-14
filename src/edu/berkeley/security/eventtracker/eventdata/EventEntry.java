package edu.berkeley.security.eventtracker.eventdata;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.database.Cursor;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.network.Networking;

/**
 * A local, in-memory version of a Event database entry. This is pushed and
 * pulled from the database when necessary.
 */
public class EventEntry implements Serializable {

	private static final long serialVersionUID = 1644264527726846951L;
	public long mDbRowID = -1;
	public String mName = "";
	public String mNotes = "";
	public long mStartTime;
	public long mEndTime;
	public long mUpdateTime;
	public String mUUID = "";
	public boolean mReceivedAtServer = false;
	public boolean mDeleted = false;

	/**
	 * Creates a new EventEntry with a startTime of now and a new UUID. This is
	 * not yet written to the database.
	 */
	public EventEntry() {
		mUpdateTime = mStartTime = System.currentTimeMillis();
		mUUID = Networking.createUUID();
	}

	/**
	 * Creates an EventEntry with a new UUID. This is not yet written to the
	 * database.
	 * 
	 * @param name
	 *            the name of the event.
	 * @param notes
	 *            the notes associated with the event.
	 * @param startTime
	 *            the long start time of the event.
	 * @param endTime
	 *            the long end time of the event.
	 */
	public EventEntry(String name, String notes, long startTime, long endTime) {
		mName = name;
		mNotes = notes;
		mUpdateTime = mStartTime = startTime;
		mEndTime = endTime;
		mUUID = Networking.createUUID();
	}

	/**
	 * Creates a new EventEntry. Only call after creating the event in the
	 * database.
	 * 
	 * @param dbRowID
	 *            the row id of the new EventEntry.
	 * @param name
	 *            the name of the event.
	 * @param notes
	 *            the notes associated with the event.
	 * @param startTime
	 *            the long start time of the event.
	 * @param endTime
	 *            the long end time of the event.
	 * @param uuid
	 *            the UUID of the EventEntry.
	 * @param receivedAtServer
	 *            whether or not the event has been received at the server.
	 */
	EventEntry(long dbRowID, String name, String notes, long startTime,
			long endTime, long updateTime, String uuid, boolean isDeleted,
			boolean receivedAtServer) {
		mDbRowID = dbRowID;
		mName = name;
		mNotes = notes;
		mStartTime = startTime;
		mEndTime = endTime;
		mUpdateTime = updateTime;
		mUUID = uuid;
		mDeleted = isDeleted;
		mReceivedAtServer = receivedAtServer;
	}

	/**
	 * Creates an EventEntry corresponding to the event row at the given cursor.
	 * Assumes the given cursor is at the correct entry.
	 * 
	 * @param eventCursor
	 *            The cursor at the event to convert to an EventEntry.
	 * @return The EventEntry corresponding to the cursor event row.
	 */
	public static EventEntry fromCursor(Cursor eventCursor, EventManager manager) {
		if (eventCursor == null || eventCursor.isClosed()
				|| eventCursor.isBeforeFirst() || eventCursor.isAfterLast())
			return null;
		long dbRowID = getLong(eventCursor, EventKey.ROW_ID);
		String name = getString(eventCursor, EventKey.NAME);
		String notes = getString(eventCursor, EventKey.NOTES);
		long startTime = getLong(eventCursor, EventKey.START_TIME);
		long endTime = getLong(eventCursor, EventKey.END_TIME);
		long updateTime = getLong(eventCursor, EventKey.UPDATE_TIME);
		String uuid = getString(eventCursor, EventKey.UUID);
		boolean recievedAtServer = getBoolean(eventCursor,
				EventKey.RECEIVED_AT_SERVER);
		boolean isDeleted = getBoolean(eventCursor, EventKey.IS_DELETED);
		return new EventEntry(dbRowID, name, notes, startTime, endTime,
				updateTime, uuid, isDeleted, recievedAtServer);
	}

	@Override
	public String toString() {
		return "{" + mDbRowID + " : " + mName + ", ("
				+ formatColumn(EventKey.START_TIME) + "->"
				+ formatColumn(EventKey.END_TIME) + ")}";
	}

	/**
	 * Formats a column into a String.
	 * 
	 * @param colType
	 *            The column to format.
	 * @return A String depicting the column value.
	 */
	public String formatColumn(EventKey colType) {
		switch (colType) {
		case START_TIME:
		case END_TIME:
		case UPDATE_TIME:
			return getDateString((Long) getValue(colType));
		default:
			return String.valueOf(getValue(colType));
		}
	}

	public Object getValue(EventKey colKey) {
		switch (colKey) {
		case NAME:
			return mName;
		case NOTES:
			return mNotes;
		case START_TIME:
			return mStartTime;
		case END_TIME:
			return mEndTime;
		case UPDATE_TIME:
			return mUpdateTime;
		case UUID:
			return mUUID;
		case IS_DELETED:
			return mDeleted;
		case RECEIVED_AT_SERVER:
			return mReceivedAtServer;
		case ROW_ID:
			return mDbRowID;
		default:
			throw new IllegalArgumentException("Unknown ColumnType: " + colKey);
		}
	}

	/**
	 * Formats a long date in a standard date format.
	 */
	private static String getDateString(long dateLong) {
		if (dateLong == 0)
			return "In Progress";
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		return dateFormat.format(new Date(dateLong));
	}

	/**
	 * Gets the value of the long column with the given columnName.
	 * 
	 * @param cursor
	 *            a cursor at a particular row.
	 * @param columnName
	 *            the name of the column in the DB.
	 * @return the long at the column with the given name, or 0 it doesn't
	 *         exist.
	 */
	private static long getLong(Cursor cursor, EventKey columnType) {
		return cursor.getLong(cursor.getColumnIndex(columnType.columnName()));
	}

	/**
	 * Gets the value of the integer column with the given columnName.
	 * 
	 * @param cursor
	 *            a cursor at a particular row.
	 * @param columnName
	 *            the name of the column in the DB.
	 * @return the integer at the column with the given name, or 0 it doesn't
	 *         exist.
	 */
	private static int getInt(Cursor cursor, EventKey columnType) {
		return cursor.getInt(cursor.getColumnIndex(columnType.columnName()));
	}

	/**
	 * 
	 * @param cursor
	 *            A cursor at a particular row.
	 * @param columnName
	 *            The name of the column in the DB.
	 * @return The String at the column with the given name.
	 */
	private static String getString(Cursor cursor, EventKey columnType) {
		return cursor.getString(cursor.getColumnIndex(columnType.columnName()));
	}

	/**
	 * 
	 * @param cursor
	 *            A cursor at a particular row.
	 * @param columnName
	 *            The name of the column in the DB.
	 * @return The boolean at the column with the given name.
	 */
	private static boolean getBoolean(Cursor cursor, EventKey columnType) {
		long dbValue = cursor.getInt(cursor.getColumnIndex(columnType
				.columnName()));
		return dbValue == 0 ? false : true;
	}

	/**
	 * Queries the databae for the list of GPSCoordinates associated with this
	 * event.
	 * 
	 * @return a list of GPSCoordinates.
	 */
	public List<GPSCoordinates> getGPSCoordinates() {
		return EventActivity.mEventManager.getGPSCoordinates(mDbRowID);
	}

}