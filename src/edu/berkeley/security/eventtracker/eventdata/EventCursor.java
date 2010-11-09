package edu.berkeley.security.eventtracker.eventdata;

import android.database.Cursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

/**
 * A cursor that maps rows of a database to EventEntry objects.
 * It is backed by a DB cursor of event rows and has most of
 * the methods a cursor has.
 */
public class EventCursor {
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
		return mCursor.getColumnIndex(colType.getColumnName());
	}
	
	/**
	 * @return The number of rows in the cursor. 
	 */
	public int getCount() {
		return mCursor == null ? 0 : mCursor.getCount();
	}
	
	/**
	 * Returns the current position of the cursor in the row set. The value
	 * is zero-based. When the row set is first returned the cursor will
	 * be at positon -1, which is before the first row. After the last row
	 * is returned another call to next() will leave the cursor past the
	 * last entry, at a position of count().
	 * @return The current cursor position. 
	 */
	public int getPosition() {
		return mCursor.getPosition();
	}
	
	/**
	 * @return The EventEntry at the cursor position.
	 */
	public EventEntry getEvent() {
		return EventEntry.fromCursor(mCursor);
	}
	
	/**
	 * Closes the Cursor, releasing all of its resources and making it 
	 * completely invalid. A call to requery() will not make the Cursor
	 * valid again. 
	 */
	public void close() {
		mCursor.close();
	}
	
	/**
	 * Performs the query that created the cursor again, refreshing its
	 * contents. This may be done at any time, including after a call to
	 * deactivate().
	 * @return true if the requery succeeded, false if not, in which case
	 * 		the cursor becomes invalid. 
	 */
	public boolean requery() {
		return mCursor.requery();
	}

	/**
	 * Deactivates the Cursor, making all calls on it fail until requery()
	 * is called. Inactive Cursors use fewer resources than active Cursors.
	 * Calling requery() will make the cursor active again. 
	 */
	public void deactivate() {
		mCursor.deactivate();
	}

	/**
	 * Returns whether the cursor is pointing to the position after the
	 * last row.
	 * @return whether the cursor is after the last result. 
	 */
	public boolean isAfterLast() {
		return mCursor.isAfterLast();
	}

	/**
	 * Returns whether the cursor is pointing to the position before
	 * the first row.
	 * @return whether the cursor is before the first result. 
	 */
	public boolean isBeforeFirst() {
		return mCursor.isBeforeFirst();
	}

	/**
	 * return true if the cursor is closed
	 * @return true if the cursor is closed. 
	 */
	public boolean isClosed() {
		return mCursor.isClosed();
	}

	/**
	 * Returns whether the cursor is pointing to the first row.
	 * @return whether the cursor is pointing at the first entry. 
	 */
	public boolean isFirst() {
		return mCursor.isFirst();
	}

	/**
	 * Returns whether the cursor is pointing to the last row.
	 * @return whether the cursor is pointing at the last entry. 
	 */
	public boolean isLast() {
		return mCursor.isLast();
	}

	/**
	 * Move the cursor by a relative amount, forward or backward
	 * from the current position. Positive offsets move forwards,
	 * negative offsets move backwards. If the final position is
	 * outside of the bounds of the result set then the resultant
	 * position will be pinned to -1 or count() depending on whether
	 * the value is off the front or end of the set, respectively. 
	 * This method will return true if the requested destination
	 * was reachable, otherwise, it returns false. For example,
	 * if the cursor is at currently on the second entry in the
	 * result set and move(-5) is called, the position will be
	 * pinned at -1, and false will be returned.
	 * @param offset the offset to be applied from the current position. 
	 * @return whether the requested move fully succeeded. 
	 */
	public boolean move(int offset) {
		return mCursor.move(offset);
	}

	/**
	 * Move the cursor to the first row. 
	 * This method will return false if the cursor is empty.
	 * @return whether the move succeeded.
	 */
	public boolean moveToFirst() {
		return mCursor.moveToFirst();
	}

	/**
	 * Move the cursor to the last row. 
	 * This method will return false if the cursor is empty.
	 * @return whether the move succeeded. 
	 */
	public boolean moveToLast() {
		return mCursor.moveToLast();
	}

	/**
	 * Move the cursor to the next row. 
	 * This method will return false if the cursor is already
	 * past the last entry in the result set.
	 * @return whether the move succeeded. 
	 */
	public boolean moveToNext() {
		return mCursor.moveToNext();
	}

	/**
	 * Move the cursor to an absolute position. The valid range
	 * of values is -1 <= position <= count. 
	 * This method will return true if the request destination was
	 * reachable, otherwise, it returns false.
	 * @param position the zero-based position to move to. 
	 * @return whether the requested move fully succeeded. 
	 */
	public boolean moveToPosition(int position) {
		return mCursor.moveToPosition(position);
	}

	/**
	 * Move the cursor to the previous row.
	 * This method will return false if the cursor is already before
	 * the first entry in the result set.
	 * @return whether the move succeeded. 
	 */
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
