package edu.berkeley.security.eventtracker.eventdata;

import android.database.Cursor;
import android.database.CursorWrapper;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;

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
	public EventKey getColumnType(int columnIndex) {
		return EventKey.fromColumnName(getColumnName(columnIndex));
	}

	/**
	 * @return The index of the given ColumnType.
	 */
	public int getColumnIndex(EventKey colType) {
		return getColumnIndex(colType.columnName());
	}

	/**
	 * @return The EventEntry at the cursor position.
	 */
	public EventEntry getEvent() {
		return EventEntry.fromCursor(this, mManager);
	}

	@Override
	public String toString() {
		return String.valueOf(getEvent());
	}
}