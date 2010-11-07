package edu.berkeley.security.eventtracker;

import android.database.Cursor;

import edu.berkeley.security.eventtracker.EventEntry.ColumnType;

/**
 * A cursor that maps rows of a database to EventEntry objects.
 * It is backed by a DB cursor of event rows and has most of
 * the methods a cursor has.
 * 
 * @author AlexD
 *
 */
class EventCursor {
	private Cursor mCursor;
	
	public EventCursor(Cursor eventCursor) {
		this.mCursor = eventCursor;
	}
	
	/**
	 * @return The ColumnType at the given index.
	 */
	public ColumnType getColumnType(int columnIndex) {
		return ColumnType.fromColumnName(mCursor.getColumnName(columnIndex));
	}
	
	/**
	 * @return The index of the given ColumnType.
	 */
	public int getColumnIndex(ColumnType colType) {
		return mCursor.getColumnIndex(colType.columnName);
	}
	
	public int getCount() {
		return mCursor == null ? 0 : mCursor.getCount();
	}
	
	public int getPosition() {
		return mCursor.getPosition();
	}
	
	public EventEntry getEvent() {
		return EventEntry.fromCursor(mCursor);
	}
	
	/**
	 * Closes the Cursor, releasing all of its resources and making it completely invalid.
	 * A call to requery() will not make the Cursor valid again. 
	 */
	public void close() {
		mCursor.close();
	}
	
	public boolean requery() {
		return mCursor.requery();
	}

	public void deactivate() {
		mCursor.deactivate();
	}

	public boolean isAfterLast() {
		return mCursor.isAfterLast();
	}

	public boolean isBeforeFirst() {
		return mCursor.isBeforeFirst();
	}

	public boolean isClosed() {
		return mCursor.isClosed();
	}

	public boolean isFirst() {
		return mCursor.isFirst();
	}

	public boolean isLast() {
		return mCursor.isLast();
	}

	public boolean move(int offset) {
		return mCursor.move(offset);
	}

	public boolean moveToFirst() {
		return mCursor.moveToFirst();
	}

	public boolean moveToLast() {
		return mCursor.moveToLast();
	}

	public boolean moveToNext() {
		return mCursor.moveToNext();
	}

	public boolean moveToPosition(int position) {
		return mCursor.moveToPosition(position);
	}

	public boolean moveToPrevious() {
		return mCursor.moveToPrevious();
	}
	
	/**
	 * @return The database cursor underlying the EventCursor.
	 */
	public Cursor getDBCursor() {
		return mCursor;
	}
}
