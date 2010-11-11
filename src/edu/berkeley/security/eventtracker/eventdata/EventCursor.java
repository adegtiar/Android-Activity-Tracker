package edu.berkeley.security.eventtracker.eventdata;

import android.database.Cursor;
import android.database.CursorWrapper;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

/**
 * A cursor that maps rows of a database to EventEntry objects.
 */
public class EventCursor extends CursorWrapper {
	private EventManager mManager;

	public EventCursor(Cursor eventCursor, EventManager manager) {
		super(eventCursor);
		mManager = manager;
	}

	/**
	 * @return The ColumnType at the given index.
	 */
	public ColumnType getColumnType(int columnIndex) {
		return ColumnType.fromColumnName(getColumnName(columnIndex));
	}

	/**
	 * @return The index of the given ColumnType.
	 */
	public int getColumnIndex(ColumnType colType) {
		return getColumnIndex(colType.getColumnName());
	}

	/**
	 * @return The EventEntry at the cursor position.
	 */
	public EventEntry getEvent() {
		return EventEntry.fromCursor(this, mManager);
	}
}