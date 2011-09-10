package edu.berkeley.security.eventtracker.prediction;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

/**
 * Provides some public methods to predict which events may be starting.
 */
public class PredictionService extends Service {

	/** The cached <tt>EventModel</tt>. */
	private EventModel mEventModel;
	private Set<String> mClassifedEventNames;

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
	public Set<String> getAllEventNamePredictions() {
		LinkedHashSet<String> predictedEvents = predictEventNames();

		// Add the rest of the events
		EventCursor allEventsCursor = EventManager.getManager().fetchUndeletedEvents();
		while (allEventsCursor.moveToNext()) {
			EventEntry nextEvent = allEventsCursor.getEvent();
			if (nextEvent.isNamed()) {
				predictedEvents.add(nextEvent.mName);
			}
		}
		return predictedEvents;
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedSet</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability
	 */
	public SortedSet<PredictedPair> getEventDistribution() {
		return getEventModel().getEventDistribution();
	}

	/**
	 * Updates the model with a new event.
	 * 
	 * @param newEvent
	 *            the new event to add to the model
	 */
	public void addNewEvent(EventEntry newEvent) {
		try {
			// isDbUpdated = true;
			getEventModel().updateModel(newEvent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void invalidateModel() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void syncModelToStorage() {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Predicts the names of events that might be starting now, in order of
	 * likelihood.
	 * 
	 * @return an ordered set of predicted names
	 */
	private LinkedHashSet<String> predictEventNames() {
		SortedSet<PredictedPair> predictionResults = getEventDistribution();

		LinkedHashSet<String> eventNames = new LinkedHashSet<String>(predictionResults.size());
		for (PredictedPair predictedResult : predictionResults) {
			eventNames.add(predictedResult.getName());
		}
		return eventNames;
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @return an <tt>EventModel</tt> built using the event data
	 */
	private EventModel getEventModel() {
		if (mEventModel == null) {
			// Generate the model.
			EventModel eventModel = new EventModel(getEventNames());
			EventCursor events = EventManager.getManager().fetchUndeletedEvents();
			while (events.moveToNext()) {
				eventModel.updateModel(events.getEvent());
			}
			mEventModel = eventModel;
		}
		return mEventModel;
	}

	/**
	 * Retrieves the event names to classify on. Currently, these are the names
	 * of events that have occurred more than once.
	 * 
	 * @return a set of event names
	 */
	private Set<String> getEventNames() {
		if (mClassifedEventNames == null) {
			// Generate the event names.
			Set<String> names = new HashSet<String>();
			Set<String> repeatedNames = new HashSet<String>();
			EventCursor allEventsCursor = EventManager.getManager().fetchUndeletedEvents();
			while (allEventsCursor.moveToNext()) {
				EventEntry currentEvent = allEventsCursor.getEvent();
				if (!names.add(currentEvent.mName)) {
					repeatedNames.add(currentEvent.mName);
				}
			}
			mClassifedEventNames = repeatedNames;
		}
		return mClassifedEventNames;
	}

}
