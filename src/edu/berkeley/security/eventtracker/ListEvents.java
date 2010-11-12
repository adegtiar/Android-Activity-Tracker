package edu.berkeley.security.eventtracker;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;

/**
 * Handles the event list view that displays all events from most recent to
 * least recent.
 */
public class ListEvents extends EventActivity {
	/**
	 * An array that specifies the fields we want to display in the list (only
	 * TITLE)
	 */
	private String[] from = new String[] { EventDbAdapter.KEY_NAME,
			EventDbAdapter.KEY_START_TIME, EventDbAdapter.KEY_END_TIME,
			EventDbAdapter.KEY_ROWID };

	/**
	 * An array that specifies the layout elements we want to map event fields
	 * to.
	 */
	private int[] to = new int[] { R.id.row_event_title,
			R.id.row_event_start_time, R.id.row_event_end_time,
			R.id.row_event_delete_button };

	private EventCursor mEventsCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fillData();
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.events_list;
	}

	/**
	 * Sets the adapter to fill the rows of the ListView from the database rows.
	 */
	private void fillData() {
		// Get all of the rows from the database and create the item list
		mEventsCursor = mEventManager.fetchSortedEvents();
		startManagingCursor(mEventsCursor);

		ListView eventList = (ListView) findViewById(R.id.events_list_view);

		SimpleCursorAdapter eventsCursorAdapter = new SimpleCursorAdapter(this,
				R.layout.events_row, mEventsCursor, from, to);
		eventsCursorAdapter.setViewBinder(new EventRowViewBinder());

		initializeHeaders(eventList);

		eventList.setEmptyView(findViewById(R.id.empty_list_view));

		eventList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position < 2)
					return;
				else if (position == 2 && isTracking())
					finish(); // trying to edit event in progress
				else
					startEditEventActivity(id);
//				TextView t = (TextView) view.findViewById(R.id.row_event_title);
//				t.setText(t.getText() + "+");
			}
		});

		eventList.setAdapter(eventsCursorAdapter);
	}

	/**
	 * Initializes the headers for the given list.
	 * 
	 * @param list
	 *            The list to add headers to.
	 */
	private void initializeHeaders(ListView list) {
		TextView textTitle = new TextView(this);
		textTitle.setText(R.string.activityListHeader);
		textTitle.setTextSize(20);
		textTitle.setGravity(Gravity.CENTER);
		list.addHeaderView(textTitle);
		View listHeader = View.inflate(this, R.layout.event_row_header, null);
		list.addHeaderView(listHeader);
	}

	/**
	 * The listener associated with a delete button. Deletes the event
	 * corresponding to the row the button is in.
	 */
	private class DeleteRowListener implements OnClickListener {
		private long rowId;
		private boolean isInProgress;

		private DeleteRowListener(long rowId, boolean isInProgress) {
			this.rowId = rowId;
			this.isInProgress = isInProgress;
		}

		@Override
		public void onClick(View v) {
			mEventManager.deleteEvent(rowId);
			mEventsCursor.requery();
			if (isInProgress) {
				updateTrackingUI(false);
			}
		}
	}

	/**
	 * Helps interface the Cursor with the view, updating the views of a row
	 * with values in the DB.
	 */
	private class EventRowViewBinder implements ViewBinder {

		@Override
		public boolean setViewValue(View view, final Cursor cursor,
				int columnIndex) {
			EventCursor eCursor = new EventCursor(cursor, mEventManager);
			ColumnType colType = eCursor.getColumnType(columnIndex);
			switch (colType) {
			case ROWID:
				// Initializing the delete button
				long rowId = cursor.getLong(columnIndex);
				boolean isInProgress = cursor.getLong(cursor
						.getColumnIndex(EventDbAdapter.KEY_END_TIME)) == 0;
				view.setOnClickListener(new DeleteRowListener(rowId,
						isInProgress));
				return true;
			case START_TIME:
			case END_TIME:
				EventEntry event = eCursor.getEvent();
				String dateString = event.formatColumn(eCursor
						.getColumnType(columnIndex));
				((TextView) view).setText(dateString);
				return true;
			default:
				return false;
			}
		}
	}
}
