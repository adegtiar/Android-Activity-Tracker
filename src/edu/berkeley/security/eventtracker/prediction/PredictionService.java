package edu.berkeley.security.eventtracker.prediction;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

/**
 * A service that can make predictions about events that are currently
 * occurring.
 */
public class PredictionService extends Service {

	private EventModel mEventModel;
	private Set<String> mCachedDistribution;
	private boolean mCacheIsValid;

	@Override
	public IBinder onBind(Intent intent) {
		return new PredictionBinder();
	}

	public class PredictionBinder extends Binder {
		public PredictionService getService() {
			return PredictionService.this;
		}
	}

	/**
	 * Retrieves an ordered set of all events in order of most likely to least
	 * likely that is the best prediction currently available. Any names not
	 * ready or ignored by {@link predictEventNames()} will be appended in an
	 * arbitrary order.
	 * 
	 * @return the best-effort ordered set of predicted event names
	 */
	public Set<String> getEventNamePredictions() {
		if (mCachedDistribution == null) {
			// mCachedDistribution = generateAllEventNames();
			mCachedDistribution = generateAllEventNamePredictions();
		}
		return mCachedDistribution;
	}

	/**
	 * Updates the model with a new event.
	 * 
	 * @param newEvent
	 *            the new event to add to the model
	 */
	public void addNewEvent(EventEntry newEvent) {
		try {
			getEventModel().updateModel(newEvent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Updates the model with the existing event.
	 * 
	 * @param event
	 *            the event to update
	 */
	public void updateEvent(EventEntry event) {
		invalidateModel();
	}

	/**
	 * Deletes the event with the given id from the model.
	 * 
	 * @param eventId
	 *            the id of event to delete
	 */
	public void deleteEvent(long eventId) {
		invalidateModel();
	}

	/**
	 * Persists the internal model to local storage.
	 */
	public void syncModelToStorage() {
		// TODO implement.
	}

	/**
	 * Requests that the cache be updated. If the cache is already valid, the
	 * request is ignored. This method is non-blocking.
	 */
	private void updateCacheAsync() {
		// TODO asynchronously update the cache
	}

	/**
	 * TODO implement
	 */
	private void updateCache() {
		if (!mCacheIsValid) {

		}
	}

	/**
	 * Marks the current model as invalid due to an event that could not
	 * incrementally update it.
	 */
	private void invalidateModel() {
		mCacheIsValid = true;
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @return an <tt>EventModel</tt> built using the event data
	 */
	private EventModel getEventModel() {
		if (mEventModel == null) {
			// Generate the model.
			EventModel eventModel = new EventModel(generateClassifiedEventNames());
			EventCursor events = EventManager.getManager().fetchAllEvents();
			while (events.moveToNext()) {
				eventModel.updateModel(events.getEvent());
			}
			mEventModel = eventModel;
		}
		return mEventModel;
	}

	private Set<String> generateAllEventNames() {
		Set<String> eventNames = new TreeSet<String>();
		EventCursor allEventsCursor = EventManager.getManager().fetchAllEvents();
		while (allEventsCursor.moveToNext()) {
			EventEntry currentEvent = allEventsCursor.getEvent();
			if (currentEvent.isNamed()) {
				eventNames.add(currentEvent.mName);
			}
		}
		return eventNames;
	}

	/**
	 * Generates an ordered set of all events in order of most likely to least
	 * likely. Any additional unclassified are appended in an arbitrary order.
	 * 
	 * @return the best-effort ordered set of predicted event names
	 */
	private LinkedHashSet<String> generateAllEventNamePredictions() {
		LinkedHashSet<String> predictedEvents = generateClassifiedEventNamePredictions();

		// Append the rest of the events.
		predictedEvents.addAll(generateAllEventNames());
		return predictedEvents;
	}

	/**
	 * Retrieves the event names to classify on. Currently, these are the names
	 * of events that have occurred more than once.
	 * 
	 * @return a set of event names
	 */
	private Set<String> generateClassifiedEventNames() {
		// Generate the event names.
		Set<String> names = new HashSet<String>();
		Set<String> repeatedNames = new HashSet<String>();
		EventCursor allEventsCursor = EventManager.getManager().fetchAllEvents();
		while (allEventsCursor.moveToNext()) {
			EventEntry currentEvent = allEventsCursor.getEvent();
			if (currentEvent.isNamed() && !names.add(currentEvent.mName)) {
				repeatedNames.add(currentEvent.mName);
			}
		}
		return repeatedNames;
	}

	/**
	 * Predicts the names of events that might be starting now, in order of
	 * likelihood.
	 * 
	 * @return an ordered set of predicted names
	 */
	private LinkedHashSet<String> generateClassifiedEventNamePredictions() {
		SortedSet<PredictedPair> predictionResults = getEventModel().getEventDistribution();

		LinkedHashSet<String> eventNames = new LinkedHashSet<String>(predictionResults.size());
		for (PredictedPair predictedResult : predictionResults) {
			eventNames.add(predictedResult.getName());
		}
		return eventNames;
	}

}
