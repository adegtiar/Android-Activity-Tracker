package edu.berkeley.security.eventtracker.eventdata;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.database.Cursor;

/**
 * A local, in-memory version of a Event database entry. This is pushed and pulled
 * from the database when necessary.  
 * 
 * @author AlexD
 *
 */
public class EventEntry {
	public long mDbRowID=-1;
	public String mName="";
	public String mNotes="";
	public long mStartTime;
	public long mEndTime;
	
	/**
	 * An enumeration of column type names in the event table.
	 * @author AlexD
	 *
	 */
	public enum ColumnType {
		NAME(EventDbAdapter.KEY_NAME),
		NOTES(EventDbAdapter.KEY_NOTES),
		START_TIME(EventDbAdapter.KEY_START_TIME),
		END_TIME(EventDbAdapter.KEY_END_TIME),
		ROWID(EventDbAdapter.KEY_ROWID);
	String columnName;
	
	ColumnType(String columnName) {
		this.columnName = columnName;
	}
	
	static ColumnType fromColumnName(String columnName) {
		for (ColumnType colType : ColumnType.values())
			if (colType.columnName.equals(columnName))
				return colType;
		return null;
	}
	};
	
	public EventEntry() {
		mStartTime = System.currentTimeMillis();
	}

	public EventEntry(long dbRowID, String name, String notes, long startTime, long endTime){
		this.mDbRowID =		dbRowID;
		this.mName =		name;
		this.mNotes =		notes;
		this.mStartTime =	startTime;
		this.mEndTime =		endTime;
	}
	
	/**
     * Creates an EventEntry corresponding to the event row at the given
     * cursor. Assumes the given cursor is at the correct entry.
     * @param eventCursor The cursor at the event to convert to an EventEntry.
     * @return The EventEntry corresponding to the cursor event row.
     */
    public static EventEntry fromCursor(Cursor eventCursor) {
    	assert(!(eventCursor.isClosed() || eventCursor.isBeforeFirst()
    			|| eventCursor.isAfterLast()));
    	long dbRowID =		getLong(eventCursor, EventDbAdapter.KEY_ROWID);
		String name =		getString(eventCursor, EventDbAdapter.KEY_NAME);
		String notes =	getString(eventCursor, EventDbAdapter.KEY_NOTES);
		long startTime =	getLong(eventCursor, EventDbAdapter.KEY_START_TIME);
		long endTime =		getLong(eventCursor, EventDbAdapter.KEY_END_TIME);
		return new EventEntry(dbRowID, name, notes, startTime, endTime);
    }
	
	public String toString() {
		return "{" + mDbRowID + " : " + mName + ", (" + formatColumn(ColumnType.START_TIME)
					+ "->" + formatColumn(ColumnType.END_TIME) + ")";
	}
	
	/**
	 * Formats a column into a String.
	 * @param colType The column to format.
	 * @return A String depicting the column value.
	 */
	public String formatColumn(ColumnType colType) {
		switch(colType) {
		case NAME:
			return mName;
		case NOTES:
			return mNotes;
		case START_TIME:
			return getDateString(mStartTime);
		case END_TIME:
			return getDateString(mEndTime);
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
	 * @param cursor A cursor at a particular row.
	 * @param columnName The name of the column in the DB.
	 * @return The Long at the column with the given name, or null
	 * 		   it doesn't exist.
	 */
	private static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
	}

	/**
	 * 
	 * @param cursor A cursor at a particular row.
	 * @param columnName The name of the column in the DB.
	 * @return The String at the column with the given name.
	 */
	private static String getString(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndex(columnName));
	}
}