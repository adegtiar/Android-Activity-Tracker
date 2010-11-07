package com.securityresearch.eventtracker;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class ListEvents extends Activity {
	private EventDbAdapter mDbHelper;
	private boolean isTracking;
	// Create an array to specify the fields we want to display in the list
	// (only TITLE)
	private String[] from = new String[] { EventDbAdapter.KEY_NAME,
			EventDbAdapter.KEY_START_TIME, EventDbAdapter.KEY_END_TIME , EventDbAdapter.KEY_ROWID};

	private int[] to = new int[] { R.id.row_event_title,
			R.id.row_event_start_time, R.id.row_event_end_time, R.id.row_event_delete_button };
	private Cursor mEventsCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ViewStub v = (ViewStub) findViewById(R.id.content_view);
		v.setLayoutResource(R.layout.events_list);
		v.inflate();

		mDbHelper = new EventDbAdapter(this);
		mDbHelper.open();
		fillData();

		initializeToolbars();
		
		isTracking = this.getIntent().getBooleanExtra(getString(R.string.isTracking), false);
        ((TextView) findViewById(R.id.toolbar_center)).setText(
        		isTracking ? R.string.toolbarTracking : R.string.toolbarNotTracking);
	}
	
	/**
	 * Sets the onClickListeners for the toolbar content.
	 */
	private void initializeToolbars() {
		findViewById(R.id.toolbar_right_option).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent settingsIntent = new Intent(ListEvents.this,
								Settings.class);
						settingsIntent.putExtra(getString(R.string.isTracking), isTracking);
						startActivity(settingsIntent);
					}
				});

		findViewById(R.id.toolbar_left_option).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent editIntent = new Intent(ListEvents.this,
								EditEvent.class);
						editIntent.putExtra(getString(R.string.isTracking), isTracking);
						startActivity(editIntent);
					}
				});
	}

	/**
	 * Sets the adapter to fill the rows of the ListView from the database rows.
	 */
	private void fillData() {
		// Get all of the rows from the database and create the item list
		mEventsCursor = mDbHelper.fetchSortedEvents();
		startManagingCursor(mEventsCursor);
		
		ListView eventList = (ListView) findViewById(R.id.events_list_view);
		
		SimpleCursorAdapter eventsCursor = new SimpleCursorAdapter(this,
				R.layout.events_row, mEventsCursor, from, to);
		eventsCursor.setViewBinder(new EventRowViewBinder());
		
		initializeHeaders(eventList);
		
		eventList.setEmptyView(findViewById(R.id.empty_list_view));
		eventList.setAdapter(eventsCursor);
		
		// onClick, edit the event
		eventList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				if (position < 2) return;
//				TextView t = (TextView) view.findViewById(R.id.row_event_title);
//				t.setText(t.getText() + "+");
			}
		});
	}
	
	/**
	 * Initializes the headers for the given list.
	 * @param list The list to add headers to.
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

	@Override
	protected void onResume() {
		super.onResume();
		mEventsCursor.requery();
	}

	/**
	 * The listener associated with a delete button. Deletes the event
	 * corresponding to the row the button is in.
	 * @author AlexD
	 *
	 */
	private class DeleteRowListener implements OnClickListener {
		private long rowId;
		private boolean canDelete; 
		
		private DeleteRowListener(long rowId, boolean canDelete) {
			this.rowId = rowId;
			this.canDelete = canDelete;
		}
		@Override
		public void onClick(View v) {
			if (canDelete) {
				mDbHelper.deleteEvent(rowId);
				mEventsCursor.requery();
			}
		}
		
	}
	
	/**
	 * Helps interface the Cursor with the view, updating the views of a row
	 * with values in the DB.
	 * 
	 * @author AlexD
	 *
	 */
	private class EventRowViewBinder implements ViewBinder {

		@Override
		public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
			if (columnIndex == cursor.getColumnIndex(EventDbAdapter.KEY_ROWID)) {
				long rowId = cursor.getLong(columnIndex);
				boolean canDelete = cursor.getLong(cursor.getColumnIndex(EventDbAdapter.KEY_END_TIME)) != 0;
				view.setOnClickListener(new DeleteRowListener(rowId, canDelete));
				return true;
			}
				
			if (columnIndex == cursor.getColumnIndex(EventDbAdapter.KEY_START_TIME)
					|| columnIndex == cursor.getColumnIndex(EventDbAdapter.KEY_END_TIME)) {
				long dateLong = cursor.getLong(columnIndex);
				String dateString;
				if (dateLong == 0 && columnIndex == cursor.getColumnIndex(EventDbAdapter.KEY_END_TIME))
					dateString = "In Progress";
				else
					dateString = getDateString(dateLong);
				((TextView) view).setText(dateString);
				return true;
			}
			return false;
		}
	}
	

	/**
	 * Converts the given date long into the default date String.
	 * @param dateLong The long time.
	 * @return The default string representation of the date.
	 */
	static String getDateString(long dateLong) {
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		return dateFormat.format(new Date(dateLong));
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		isTracking = savedInstanceState.getBoolean(getString(R.string.isTracking));
        ((TextView) findViewById(R.id.toolbar_center)).setText(
        		isTracking ? R.string.toolbarTracking : R.string.toolbarNotTracking);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(getString(R.string.isTracking), isTracking);
	}
}
