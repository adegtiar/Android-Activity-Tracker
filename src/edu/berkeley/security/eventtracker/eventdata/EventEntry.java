package edu.berkeley.security.eventtracker.eventdata;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.network.Networking;

import android.database.Cursor;

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
	public String mUUID = "";
	public boolean mReceivedAtServer = false;

	/**
	 * An enumeration of column type names in the event table.
	 */
	public enum ColumnType {
		NAME(EventDbAdapter.KEY_NAME), NOTES(EventDbAdapter.KEY_NOTES), START_TIME(
				EventDbAdapter.KEY_START_TIME), END_TIME(
				EventDbAdapter.KEY_END_TIME), UUID(EventDbAdapter.KEY_UUID), RECEIVED_AT_SERVER(
				EventDbAdapter.KEY_RECEIVED_AT_SERVER), ROWID(
				EventDbAdapter.KEY_ROWID);
		private String columnName;

		private ColumnType(String columnName) {
			this.columnName = columnName;
		}

		static ColumnType fromColumnName(String columnName) {
			for (ColumnType colType : ColumnType.values())
				if (colType.columnName.equals(columnName))
					return colType;
			return null;
		}

		public String getColumnName() {
			return columnName;
		}
	};

	/**
	 * Creates a new EventEntry with a startTime of now and a new UUID. This is
	 * not yet written to the database.
	 */
	public EventEntry() {
		mStartTime = System.currentTimeMillis();
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
		this.mName = name;
		this.mNotes = notes;
		this.mStartTime = startTime;
		this.mEndTime = endTime;
		this.mUUID = Networking.createUUID();
	}

	/**
	 * Creates a new EventEntry. Only call after creating the event in the
	 * database.
	 * 
	 * @param dbRowID
	 *            the row id of the new EventEntry.
	 * 
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
			long endTime, String uuid, boolean receivedAtServer) {
		this.mDbRowID = dbRowID;
		this.mName = name;
		this.mNotes = notes;
		this.mStartTime = startTime;
		this.mEndTime = endTime;
		this.mUUID = uuid;
		this.mReceivedAtServer = receivedAtServer;
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
		long dbRowID = getLong(eventCursor, EventDbAdapter.KEY_ROWID);
		String name = getString(eventCursor, EventDbAdapter.KEY_NAME);
		String notes = getString(eventCursor, EventDbAdapter.KEY_NOTES);
		String uuid = getString(eventCursor, EventDbAdapter.KEY_UUID);
		int recievedAtServer = getInt(eventCursor,
				EventDbAdapter.KEY_RECEIVED_AT_SERVER);
		long startTime = getLong(eventCursor, EventDbAdapter.KEY_START_TIME);
		long endTime = getLong(eventCursor, EventDbAdapter.KEY_END_TIME);
		return new EventEntry(dbRowID, name, notes, startTime, endTime, uuid,
				recievedAtServer == 0 ? false : true);
	}

	@Override
	public String toString() {
		return "{" + mDbRowID + " : " + mName + ", ("
				+ formatColumn(ColumnType.START_TIME) + "->"
				+ formatColumn(ColumnType.END_TIME) + ")}";
	}

	/**
	 * Formats a column into a String.
	 * 
	 * @param colType
	 *            The column to format.
	 * @return A String depicting the column value.
	 */
	public String formatColumn(ColumnType colType) {
		switch (colType) {
		case NAME:
			return mName;
		case NOTES:
			return mNotes;
		case START_TIME:
			return getDateString(mStartTime);
		case END_TIME:
			return getDateString(mEndTime);
		case UUID:
			return mUUID;
		case RECEIVED_AT_SERVER:
			return String.valueOf(mReceivedAtServer);
		case ROWID:
			return Long.toString(mDbRowID);
		default:
			throw new IllegalArgumentException("Unknown ColumnType: " + colType);
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
	private static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
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
	private static int getInt(Cursor cursor, String columnName) {
		return cursor.getInt(cursor.getColumnIndex(columnName));
	}

	/**
	 * 
	 * @param cursor
	 *            A cursor at a particular row.
	 * @param columnName
	 *            The name of the column in the DB.
	 * @return The String at the column with the given name.
	 */
	private static String getString(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndex(columnName));
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