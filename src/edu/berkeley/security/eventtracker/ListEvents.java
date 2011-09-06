package edu.berkeley.security.eventtracker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

/**
 * Handles the event list view that displays all events from most recent to
 * least recent.
 */
public class ListEvents extends EventActivity implements OnGestureListener {

	SimpleCursorAdapter eventsCursorAdapter;

	private static Date dateListed;
	/**
	 * An array that specifies the fields we want to display in the list (only
	 * TITLE)
	 */
	private String[] from = new String[] { EventKey.NAME.columnName(),
			EventKey.START_TIME.columnName(), EventKey.END_TIME.columnName(),
			EventKey.ROW_ID.columnName() };

	/**
	 * An array that specifies the layout elements we want to map event fields
	 * to.
	 */
	private int[] to = new int[] { R.id.row_event_title,
			R.id.row_event_start_time, R.id.row_event_end_time,
			R.id.row_id_container };

	private EventCursor mEventsCursor;
	private ListView eventList;

	// Variables for the date picker
	private ImageView mPickDate;
	private int mYear;
	private int mMonth;
	private int mDay;

	private static final int DATE_DIALOG_ID = 0;
	private static final int DIALOG_DELETE_EVENT = 1;

	// Used for deleting events
	private static long deleteROWID;
	private static boolean deletedRowInProgress;

	// gesture stuff
	GestureDetector mDetector;

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			Calendar c = Calendar.getInstance();
			c.set(mYear, mMonth, mDay);
			dateListed = c.getTime();

			if (mEventsCursor != null) {

				stopManagingCursor(mEventsCursor);
				mEventsCursor.close();
				eventList.invalidate();

			}

			fillData();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDetector = new GestureDetector(this, this);

		// Decide which date to show
		dateListed = EventManager.getManager().fetchDateOfLatestEvent();

		eventList = (ListView) findViewById(R.id.events_list_view);
		eventList.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent e) {
				mDetector.onTouchEvent(e);
				return false;
			}
		});

		ImageView leftArrow = (ImageView) findViewById(R.id.leftArrow);
		ImageView rightArrow = (ImageView) findViewById(R.id.rightArrow);
		fillData();
		// left arrow click!
		leftArrow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				previousListOfEvents();

			}
		});

		rightArrow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				nextListOfEvents();
			}
		});
		mPickDate = (ImageView) findViewById(R.id.calendar);
		// get the current date
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		// add a click listener to the button
		mPickDate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

	}

	private void previousListOfEvents() {
		// dateListed.setDate(dateListed.getDate()-1);
		Date possibleDate = EventManager.getManager().fetchDateBefore(
				dateListed);
		if (possibleDate == null) {
			Toast.makeText(getApplicationContext(), "No further events",
					Toast.LENGTH_SHORT).show();
			return;
		} else {
			dateListed = possibleDate;
		}

		if (mEventsCursor != null) {

			stopManagingCursor(mEventsCursor);
			mEventsCursor.close();
			eventList.invalidate();

		}

		fillData();
	}

	private void nextListOfEvents() {

		Date possibleDate = EventManager.getManager()
				.fetchDateAfter(dateListed);
		if (possibleDate == null) {
			Toast.makeText(getApplicationContext(), "No further events",
					Toast.LENGTH_SHORT).show();
			return;
		} else {
			dateListed = possibleDate;
		}

		if (mEventsCursor != null) {
			eventList.invalidate();
			stopManagingCursor(mEventsCursor);
			mEventsCursor.close();

		}

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

		mEventsCursor = mEventManager.fetchSortedEvents(dateListed);

		// Get all of the rows from the database and create the item list
		// mEventsCursor = mEventManager.fetchSortedEvents();
		startManagingCursor(mEventsCursor);

		SimpleCursorAdapter eventsCursorAdapter = new SimpleCursorAdapter(
				ListEvents.this, R.layout.events_row, mEventsCursor, from, to);
		eventsCursorAdapter.setViewBinder(new EventRowViewBinder());

		initializeHeaders(eventList);

		eventList.setEmptyView(findViewById(R.id.empty_list_view));

		eventList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position < 1) {
					return;
				}
				boolean sameAsCurrentDate = isToday(dateListed);

				if (position == 1 && isTracking() && sameAsCurrentDate)
					finish(); // trying to edit event in progress
				else
					startEditEventActivity(id);
			}
		});
		eventList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub

				long rowId = (Long) view.findViewById(R.id.row_id_container)
						.getTag(R.string.rowIDContainer);
				boolean isInProgress = (Boolean) view.findViewById(
						R.id.row_id_container).getTag(
						R.string.isInProgressContainer);
				Bundle bundle = new Bundle();
				bundle.putString("nameOfEvent",
						mEventManager.fetchEvent(rowId).mName);
				bundle.putLong("rowId", rowId);
				bundle.putBoolean("isInProgress", isInProgress);
				showDialog(DIALOG_DELETE_EVENT, bundle);
				return true;
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
		int headerCount = list.getHeaderViewsCount();
		TextView titleHeader = (TextView) findViewById(R.id.titleHeader);

		String titleForList = "Activites for ";

		titleForList += (isToday(dateListed)) ? "Today" : DateFormat
				.getDateInstance().format(dateListed);

		titleHeader.setText(titleForList);
		// textTitle.setText(R.string.activityListHeader);
		titleHeader.setTextSize(25);
		titleHeader.setGravity(Gravity.CENTER);

		if (headerCount == 0) {
			// list.addHeaderView(titleHeader);
			View listHeader = View.inflate(this, R.layout.event_row_header,
					null);
			list.addHeaderView(listHeader);
		}

	}

	private boolean isToday(Date date) {
		String dateString = DateFormat.getDateInstance().format(date);
		String todayString = DateFormat.getDateInstance().format(new Date());
		return dateString.equals(todayString);
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
			EventKey colType = eCursor.getColumnType(columnIndex);

			switch (colType) {
			case ROW_ID:
				// Initializing the delete button

				long rowId = cursor.getLong(columnIndex);
				view.setTag(R.string.rowIDContainer, rowId);

				boolean selectedFirst = cursor.getLong(cursor
						.getColumnIndex(EventKey.END_TIME.columnName())) == 0; // TODO
																				// fix
																				// this
				boolean isInProgress = isToday(dateListed) && selectedFirst;
				view.setTag(R.string.isInProgressContainer, isInProgress);
				return true;
			case START_TIME:
			case END_TIME:
				EventEntry event = eCursor.getEvent();
				String timeString = event.getTimeString(colType);
				((TextView) view).setText(timeString);
				return true;
			default:
				return false;
			}
		}
	}

	protected void startEditEventActivity(long rowId) {
		Intent editIntent = new Intent(this, EditMode.class);
		editIntent.putExtra(EventKey.ROW_ID.columnName(), rowId);
		startActivity(editIntent);
		overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
					mDay);
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog,
			Bundle bundle) {
		switch (id) {
		case DIALOG_DELETE_EVENT:
			TextView deleteEvent = (TextView) dialog
					.findViewById(R.id.delete_description);
			deleteEvent.setText("Are you sure you want to delete the event "
					+ bundle.getString("nameOfEvent") + "?");
			deleteROWID = bundle.getLong("rowId");
			deletedRowInProgress = bundle.getBoolean("isInProgress");

			break;
		}
	}

	/*
	 * Dialog box for presenting a dialog box when a user tries to delete an
	 * event
	 */
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {

		case DIALOG_DELETE_EVENT:

			LayoutInflater te_factory = LayoutInflater.from(this);
			final View textEntryView = te_factory.inflate(
					R.layout.delete_dialog, null);
			return new AlertDialog.Builder(this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.delete_dialog_title)
					.setView(textEntryView)
					.setPositiveButton(R.string.delete_dialog_yes,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {

									mEventManager.markEventDeleted(deleteROWID);
									Networking.sendToServer(
											ServerRequest.DELETE, mEventManager
													.fetchEvent(deleteROWID),
											ListEvents.this);
									mEventsCursor.requery();
									if (deletedRowInProgress) {
										updateTrackingStatus(false);
									}

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		default:
			return super.onCreateDialog(id, bundle);
		}
	}

	@Override
	protected Class<?> getLeftActivityClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class<?> getRightActivityClass() {
		return TrackingMode.class;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		try {
			if (Math.abs(e1.getY() - e2.getY()) > FlingDetector.SWIPE_MAX_OFF_PATH)
				return false;
			if (e1.getX() - e2.getX() > FlingDetector.SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > FlingDetector.SWIPE_THRESHOLD_VELOCITY) {
				// right to left swipe
				nextListOfEvents();
			} else if (e2.getX() - e1.getX() > FlingDetector.SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > FlingDetector.SWIPE_THRESHOLD_VELOCITY) {
				// left to right swipe
				previousListOfEvents();

			}
		} catch (Exception e) {
			// nothing
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

}
