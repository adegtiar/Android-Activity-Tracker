package edu.berkeley.security.eventtracker.prediction;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.prediction.EventModel.NoAttributeValueException;

/**
 * A service that can make predictions about events that are currently
 * occurring.
 */
public class PredictionService extends Service {

	private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private EventManager mManager = EventManager.getManager();
	private EventModel mEventModel;
	private Set<String> mCachedDistribution;

	@Override
	public IBinder onBind(Intent intent) {
		if (mEventModel == null) {
			regenerateAllAsync();
		}
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
	synchronized public Set<String> getEventNamePredictions() {
		if (mCachedDistribution == null) {
			mCachedDistribution = generateAllEventNames();
		}
		return mCachedDistribution;
	}

	/**
	 * Updates the model with a new event.
	 * 
	 * @param event
	 *            the new event to add to the model
	 */
	public void addNewEvent(final EventEntry event) {
		if (!event.isNamed()) {
			return;
		}
		Runnable updateModel = new Runnable() {
			@Override
			public void run() {
				try {
					getEventModel().updateModel(event);
				} catch (NoAttributeValueException e) {
					regenerateModel(); // TODO check if necessary
					regenerateCache();
				}
			}
		};
		mExecutor.execute(updateModel);
	}

	/**
	 * Updates the model with the existing event.
	 * 
	 * @param event
	 *            the event to update
	 */
	public void updateEvent(EventEntry event) {
		if (event.isNamed()) {
			regenerateAllAsync();
		}
	}

	/**
	 * Deletes the event with the given id from the model.
	 * 
	 * @param eventId
	 *            the id of event to delete
	 */
	public void deleteEvent(EventEntry event) {
		updateEvent(event);
	}

	/**
	 * Requests that the cache be updated. If the cache is already valid, the
	 * request is ignored. This method is non-blocking.
	 */
	private void regenerateAllAsync() {
		Runnable regenerate = new Runnable() {
			@Override
			public void run() {
				regenerateModel();
				regenerateCache();
			}
		};
		mExecutor.execute(regenerate);
	}

	/**
	 * Marks the current model as invalid due to an event that could not
	 * incrementally update it.
	 */
	private void regenerateModel() {
		EventModel newModel = generateEventModel();
		synchronized (this) {
			mEventModel = newModel;
		}
	}

	private void regenerateCache() {
		Set<String> newPredictions = generateAllEventNamePredictions();
		synchronized (this) {
			mCachedDistribution = newPredictions;
		}
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @return an <tt>EventModel</tt> built using the event data
	 */
	private EventModel getEventModel() {
		if (mEventModel == null) {
			regenerateModel();
		}
		return mEventModel;
	}

	private EventModel generateEventModel() {
		// Generate the model.
		EventModel eventModel = new EventModel(generateClassifiedEventNames());
		EventCursor events = mManager.fetchAllEvents();
		while (events.moveToNext()) {
			try {
				eventModel.updateModel(events.getEvent());
			} catch (NoAttributeValueException e) {
				// Do nothing. Only use classified events.
			}
		}
		return eventModel;
	}

	private Set<String> generateAllEventNames() {
		Set<String> eventNames = new TreeSet<String>();
		EventCursor allEventsCursor = mManager.fetchAllEvents();
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
		EventCursor allEventsCursor = mManager.fetchAllEvents();
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
