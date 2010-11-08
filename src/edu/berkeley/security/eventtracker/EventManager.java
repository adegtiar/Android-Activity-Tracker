package edu.berkeley.security.eventtracker;

import android.content.Context;
import android.database.SQLException;

/**
 * Manages the event data back-end and acts as a wrapper around a database adapter.
 * A manager should be shared across all event-related activities.
 * 
 * @author AlexD
 *
 */
public class EventManager {
	private EventDbAdapter mDbHelper;
	private static EventManager mEventManager;
	
	
	private EventManager(Context context) {
		mDbHelper = new EventDbAdapter(context);
	}
	
	/**
	 * Returns an instance of an EventManager. Only one may be active at a time.
	 * @param mainActivity The single activity the EventManger should start from.
	 * @return The EventManger instance.
	 */
	public static EventManager getManager(EventActivity mainActivity) {
		if (mEventManager == null)
			mEventManager = new EventManager(mainActivity).open();
		return mEventManager;
	}
	
	/**
	 * Opens the database.
	 * @return this (self reference, allowing this to be chained
	 * 		in an initialization call)
	 */
	public EventManager open() {
		mDbHelper.open();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	/**
     * Create a new entry in the database corresponding to the given eventName, notes, and startTime
     * 
     * @return a new EventEntry corresponding to the database entry, or null upon error.
     */
    public EventEntry createEvent(String eventName, String notes, long startTime, long endTime) {
        long newRowID = mDbHelper.createEvent(eventName, notes, startTime, endTime);
        if (newRowID != -1)
        	return new EventEntry(newRowID, eventName, notes, startTime, endTime);
        else
        	return null;
    }
    
    /**
     * Creates or updates the database with the given event.
     * @param event The EventEntry to push to the database.
     * @return Whether or not the database was successfully updated.
     */
    public boolean updateDatabase(EventEntry event) {
    	if (event == null)
			return false;
    	if(event.mDbRowID==-1) {
			event.mDbRowID = mDbHelper.createEvent(event.mName, event.mNotes, event.mStartTime, event.mEndTime);
			return event.mDbRowID != -1;
		} else {
			return mDbHelper.updateEvent(event.mDbRowID, event.mName, 
					event.mNotes, event.mStartTime,event.mEndTime);
		}
    }
    
    /**
     * Delete the event with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteEvent(long rowId) {
        return mDbHelper.deleteEvent(rowId);
    }
    
    /**
     * @return An Iterator over all events in the database.
     */
    public EventCursor fetchAllEvents() {
    	return new EventCursor(mDbHelper.fetchAllEvents());
    }
    
    /**
     * @return An iterator over all events in descending endTime order.
     */
    public EventCursor fetchSortedEvents() {
    	return new EventCursor(mDbHelper.fetchSortedEvents());
    }
    
    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public EventEntry fetchEvent(long rowId) throws SQLException {
    	EventCursor events = new EventCursor(mDbHelper.fetchEvent(rowId));
        return events.getCount() > 0 ? events.getEvent() : null;
    }
    
    /**
     * @return true if we are still tracking an activity, otherwise false.
     */
    public boolean isTracking() {
    	EventCursor events = new EventCursor(mDbHelper.fetchSortedEvents());
    	if (!events.moveToFirst())
    		return false; // no events, so can't be tracking
    	// if end time is 0(initial value), we are still tracking.
        return events.getEvent().mEndTime == 0; 
    }
}
