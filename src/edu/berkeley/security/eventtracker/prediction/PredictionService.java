package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	private Set<String> mEventNames;

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
	 * Predicts the names of events that might be starting now, in order of
	 * likelihood.
	 * 
	 * @return a list of predicted names
	 */
	public List<String> predictEventNames() {
		SortedSet<PredictedPair> predictionResults = getEventDistribution();

		List<String> eventNames = new ArrayList<String>(predictionResults.size());
		for (PredictedPair predictedResult : predictionResults) {
			eventNames.add(predictedResult.getName());
		}
		return eventNames;
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
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
	public void updateEventModel(EventEntry newEvent) {
		try {
			if (mEventModel != null) {
				// isDbUpdated = true;
				getEventModel().updateModel(newEvent);
			}
		} catch (Exception e) {
			// TODO make more graceful
			throw new RuntimeException(e);
		}
	}

	public void syncModelToStorage() {
		// TODO implement
	}

	public void markDbUnsupportedUpdated() {
		// TODO implement
		mEventModel = null;
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @return an <tt>EventModel</tt> built using the event data
	 */
	private EventModel getEventModel() {
		if (mEventModel == null) {
			// Generate the model.
			EventInstances eventData = new EventInstances(getEventNames());
			EventCursor events = EventManager.getManager().fetchUndeletedEvents();
			while (events.moveToNext()) {
				eventData.add(events.getEvent());
			}
			mEventModel = new EventModel(eventData);
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
		if (mEventNames == null) {
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
			mEventNames = repeatedNames;
		}
		return mEventNames;
	}

}
