package edu.berkeley.security.eventtracker.eventdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.prediction.PredictionService;

/**
 * Manages the event data back-end and acts as a wrapper around a database
 * adapter. A manager should be shared across all event-related activities.
 */
public class EventManager {
	private TagsDBAdapter mTagHelper;
	private GPSDbAdapter mGPSHelper;
	private EventDbAdapter mDbHelper;
	private static EventManager mEventManager;

	private EventManager(Context context) {
		mDbHelper = new EventDbAdapter(context);
		mGPSHelper = new GPSDbAdapter(context);
		mTagHelper = new TagsDBAdapter(context);
	}

	/**
	 * Returns an instance of an EventManager. Only one may be active at a time.
	 * 
	 * @param mainActivity
	 *            the single activity the {@link EventManager} should start from
	 * @return the {@link EventManager} instance.
	 */
	public static EventManager getManager(Context context) {
		if (mEventManager == null)
			mEventManager = new EventManager(context).open();
		return mEventManager;
	}

	public static EventManager getManager() {
		return mEventManager;
	}

	/**
	 * Opens the database.
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 */
	public EventManager open() {
		mDbHelper.open();
		mGPSHelper.open();
		mTagHelper.open();
		return this;
	}

	/**
	 * Closes the database and shuts down the GPS service.
	 */
	public void close() {
		mDbHelper.close();
		mGPSHelper.close();
		mTagHelper.close();
	}

	/**
	 * Create a new entry in the database corresponding to the given eventName,
	 * notes, and startTime.
	 * 
	 * @param name
	 *            the name of the event
	 * @param notes
	 *            the notes associated with the event
	 * @param startTime
	 *            the long start time of the event
	 * @param endTime
	 *            the long end time of the event
	 * @return a new EventEntry corresponding to the database entry, or null
	 *         upon error
	 */
	public EventEntry createEvent(String name, String notes, long startTime, long endTime,
			boolean receivedAtServer, String tag) {
		EventEntry newEntry = new EventEntry(name, notes, startTime, endTime, tag);
		return updateDatabase(newEntry, receivedAtServer) ? newEntry : null;
	}

	/**
	 * Creates or updates the database with the given event.
	 * 
	 * @param event
	 *            the {@link EventEntry }to push to the database
	 * @return whether or not the database was successfully updated
	 */
	public boolean updateDatabase(EventEntry event, boolean receivedAtServer) {
		if (event == null)
			return false;
		if (event.mDbRowID == -1) {
			event.mDbRowID = mDbHelper.createEvent(event.mName, event.mNotes, event.mStartTime,
					event.mEndTime, event.mUUID, receivedAtServer, event.mTag);
			event.persisted = event.mDbRowID != -1;
			if (event.persisted)
				getPredictionService().addNewEvent(event);
			return event.persisted;
		} else {
			boolean updateSuccessful = mDbHelper.updateEvent(event.mDbRowID, event.mName,
					event.mNotes, event.mStartTime, event.mEndTime, event.mUUID, event.deleted,
					receivedAtServer, event.mTag);
			if (updateSuccessful) {
				event.persisted = true;
				getPredictionService().updateEvent(event);
			}
			return updateSuccessful;
		}
	}

	/**
	 * Creates or updates the database with the given list of events.
	 */
	public void updateDatabaseBulk(ArrayList<EventEntry> listOfEvents, boolean receivedAtServer) {
		if (listOfEvents == null) {
			return; // TODO remove this call?
		}
		for (EventEntry event : listOfEvents) {
			updateDatabase(event, receivedAtServer);
		}

	}

	/**
	 * Mark the event with the given rowId as deleted.
	 * 
	 * @param rowId
	 *            id of note to mark as deleted
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEvent(long rowId) {
		boolean successful = mDbHelper.markDeleted(rowId);
		if (successful) {
			getPredictionService().deleteEvent(rowId);
		}
		return successful;
	}

	/**
	 * Deletes all events and GPS entries in the database. TODO make more
	 * efficient.
	 * 
	 * @return the number of events deleted
	 */
	public int permanentlyDeleteAllEntries() {
		EventCursor cursor = actuallyFetchAllEvents();
		int nDeleted = 0;
		while (cursor.moveToNext()) {
			long rowId = cursor.getEvent().mDbRowID;
			permanentlyDeleteEvent(rowId);
			nDeleted++;
		}
		return nDeleted;
	}

	public EventCursor fetchAllEvents() {
		return new EventCursor(mDbHelper.fetchUndeletedEvents(), this);
	}

	/**
	 * @return a cursor over all events in descending endTime order
	 */
	public EventCursor fetchSortedEvents() {
		return new EventCursor(mDbHelper.fetchSortedEvents(), this);
	}

	/**
	 * @return the date of the latest event that took place
	 */
	public Date fetchDateOfLatestEvent() {
		EventCursor mCursor = fetchSortedEvents();
		if (mCursor.moveToFirst()) {
			EventEntry latestEvent = mCursor.getEvent();
			return new Date(latestEvent.mStartTime);
		} else {
			return Calendar.getInstance().getTime();
		}

	}

	/**
	 * @return the date of the event that comes before date
	 */
	public Date fetchDateBefore(Date date) {
		EventCursor cursor = new EventCursor(mDbHelper.fetchSortedEventsBeforeDate(earliestTime(
				date).getTime()), this);
		return getFirstDate(cursor);
	}

	/**
	 * @return the date of the event that comes after after date
	 */
	public Date fetchDateAfter(Date date) {
		EventCursor cursor = new EventCursor(mDbHelper.fetchSortedEventsAfterDate(latestTime(date)
				.getTime()), this);
		return getFirstDate(cursor);
	}

	/**
	 * @return a cursor over all events in descending endTime order on this date
	 */
	public EventCursor fetchSortedEvents(Date date) {
		Date startDate = earliestTime(date);
		Date endDate = latestTime(date);
		return new EventCursor(mDbHelper.fetchSortedEvents(startDate.getTime(), endDate.getTime()),
				this);
	}

	/**
	 * @return a cursor over the list of all events in the database that are not
	 *         yet on the web server
	 */
	public EventCursor fetchPhoneOnlyEvents() {
		return new EventCursor(mDbHelper.fetchPhoneOnlyEvents(), this);
	}

	/**
	 * Retrieves the event corresponding to the given row id.
	 * 
	 * @param rowId
	 *            id of the event to retrieve
	 * @return an {@link EventEntry} for the event, or null if not found
	 */
	public EventEntry fetchEvent(long rowId) throws SQLException {
		EventCursor events = new EventCursor(mDbHelper.fetchEvent(rowId), this);
		return events.getCount() > 0 ? events.getEvent() : null;
	}

	/**
	 * Retrieves an event with the given name.
	 * 
	 * @param name
	 *            the name of the event to find
	 * @return the first event with the given name
	 */
	public EventEntry fetchEvents(String name) throws SQLException {
		EventCursor events = new EventCursor(mDbHelper.fetchEvents(name), this);
		return events.getCount() > 0 ? events.getEvent() : null;
	}

	/**
	 * Either finds the given event in the database, or a creates a new
	 * (unsaved) event entry
	 * 
	 * @param uuid
	 *            the UUID to find or create by
	 * @return the new or found event
	 */
	public EventEntry findOrCreateByUUID(String uuid) {
		EventCursor events = new EventCursor(mDbHelper.fetchEvent(uuid), this);
		EventEntry event = events.getCount() > 0 ? events.getEvent() : new EventEntry();
		event.mUUID = uuid;
		return event;
	}

	/**
	 * @return true if we are still tracking an activity, otherwise false.
	 */
	public boolean isTracking() {
		return getCurrentEvent() != null;
	}

	public List<GPSCoordinates> getGPSCoordinates(Long rowID) {
		ArrayList<GPSCoordinates> toBeReturned = new ArrayList<GPSCoordinates>();

		Cursor cursor = null;
		try {
			cursor = mGPSHelper.getGPSCoordinates(rowID);
		} catch (Exception e) {
			Log.e(EventActivity.LOG_TAG, "Failed to get GPS coordinates.", e);
		}

		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				double latitude = cursor.getDouble(cursor
						.getColumnIndex((GPSDbAdapter.KEY_LATITUDE)));
				double longitude = cursor.getDouble(cursor
						.getColumnIndex((GPSDbAdapter.KEY_LONGITUDE)));
				long time = cursor.getLong(cursor.getColumnIndex((GPSDbAdapter.KEY_GPSTIME)));
				toBeReturned.add(new GPSCoordinates(latitude, longitude, time));

			}
		}
		cursor.close();
		return toBeReturned;

	}

	public LinkedHashSet<String> getTags() {
		LinkedHashSet<String> tagSet = new LinkedHashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mTagHelper.getTags();
		} catch (Exception e) {
			Log.e(EventActivity.LOG_TAG, "Failed to get tags.", e);
		}

		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				String tag = cursor.getString(cursor.getColumnIndex((TagsDBAdapter.KEY_TAG)));
				tagSet.add(tag);

			}
		}
		cursor.close();
		return tagSet;

	}

	/**
	 * Retrieves the event that is currently in progress.
	 * 
	 * @return the current event
	 */
	public EventEntry getCurrentEvent() {
		EventCursor events = new EventCursor(mDbHelper.fetchSortedEvents(), this);
		if (!events.moveToFirst())
			return null; // no events, so can't be tracking
		// if end time is 0(initial value), we are still tracking.
		EventEntry currentEvent = events.getEvent();
		return currentEvent.mEndTime == 0 ? currentEvent : null;
	}

	public void addGPSCoordinates(GPSCoordinates coord, long eventRowID) {
		mGPSHelper.createGPSEntry(eventRowID, coord.getLatitude(), coord.getLongitude(),
				coord.getTime());

	}

	/**
	 * Adds a tag to the database
	 * 
	 * @param string
	 *            - the tag to be added
	 */
	public void addTag(String tag) {
		mTagHelper.createTagEntry(tag);
	}

	/**
	 * Gets the earliest time corresponding to the same day as date
	 * 
	 * @param dateToReturn
	 * @return a date object which represents 12am of that same day
	 */
	private static Date earliestTime(Date date) {
		Date dateToReturn = (Date) date.clone();
		dateToReturn.setHours(0);
		dateToReturn.setMinutes(0);
		dateToReturn.setSeconds(0);
		return dateToReturn;
	}

	/**
	 * Gets the latest time corresponding to the same day as date
	 * 
	 * @param date
	 * @return a date object which represents midnight of that same day
	 */
	private static Date latestTime(Date date) {
		Date dateToReturn = (Date) date.clone();
		dateToReturn.setHours(23);
		dateToReturn.setMinutes(59);
		dateToReturn.setSeconds(59);
		return dateToReturn;
	}

	/**
	 * Gets the date of the first event in the cursor.
	 * 
	 * @param cursor
	 *            a cursor of events
	 * @return the data of the first event, or null if the cursor is empty
	 */
	private static Date getFirstDate(EventCursor cursor) {
		if (cursor.moveToFirst()) {
			EventEntry latestEvent = cursor.getEvent();
			return new Date(latestEvent.mStartTime);
		} else {
			return null;
		}
	}

	/**
	 * @return a cursor over all events (including deleted) in the database
	 */
	private EventCursor actuallyFetchAllEvents() {
		return new EventCursor(mDbHelper.fetchAllEvents(), this);
	}

	/**
	 * Delete the event with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	private boolean permanentlyDeleteEvent(long rowId) {
		boolean successful = mDbHelper.deleteEvent(rowId);
		if (successful) {
			getPredictionService().deleteEvent(rowId);
			deleteGPSEntries(rowId);
		}
		return successful;
	}

	/**
	 * Delete all of the GPSEntrys with the given eventRowID.
	 * 
	 * @param eventRowID
	 *            id of gpsEntrys to delete
	 * @return true if deleted, false otherwise
	 */
	private boolean deleteGPSEntries(long eventRowID) {
		Cursor cursor = mGPSHelper.getGPSCoordinates(eventRowID);

		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				long rowID = cursor.getColumnIndex(GPSDbAdapter.KEY_ROWID);
				mGPSHelper.deleteEntry(rowID);
			}
		}
		cursor.close();
		return true;
	}

	private PredictionService getPredictionService() {
		return EventActivity.mPredictionService;
	}
}
