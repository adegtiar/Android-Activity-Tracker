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
public class EventEntry implements Serializable{
	
	private static final long serialVersionUID = 1644264527726846951L;
	public long mDbRowID = -1;
	public String mName = "";
	public String mNotes = "";
	public long mStartTime;
	public long mEndTime;
	public String mUUID="";

	/**
	 * An enumeration of column type names in the event table.
	 */
	public enum ColumnType {
		NAME(EventDbAdapter.KEY_NAME), NOTES(EventDbAdapter.KEY_NOTES), START_TIME(
				EventDbAdapter.KEY_START_TIME), END_TIME(
				EventDbAdapter.KEY_END_TIME),  UUID(EventDbAdapter.KEY_UUID), ROWID(EventDbAdapter.KEY_ROWID);
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

	public EventEntry() {
		mStartTime = System.currentTimeMillis();
		mUUID= Networking.createUUID();
		
	}

	public EventEntry(long dbRowID, String name, String notes, long startTime,
			long endTime, String uuid/*, EventManager manager*/) {
		this.mDbRowID = dbRowID;
		this.mName = name;
		this.mNotes = notes;
		this.mStartTime = startTime;
		this.mEndTime = endTime;
		this.mUUID=uuid;
//		this.mManager = manager; //TODO uh oh
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
		String uuid=getString(eventCursor, EventDbAdapter.KEY_UUID);
		long startTime = getLong(eventCursor, EventDbAdapter.KEY_START_TIME);
		long endTime = getLong(eventCursor, EventDbAdapter.KEY_END_TIME);
		return new EventEntry(dbRowID, name, notes, startTime, endTime, uuid/*, manager*/);
	}

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
	 * @param cursor
	 *            A cursor at a particular row.
	 * @param columnName
	 *            The name of the column in the DB.
	 * @return The Long at the column with the given name, or null it doesn't
	 *         exist.
	 */
	private static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
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

	public List<GPSCoordinates> getGPSCoordinates() {
		return EventActivity.mEventManager.getGPSCoordinates(mDbRowID);
	}

}