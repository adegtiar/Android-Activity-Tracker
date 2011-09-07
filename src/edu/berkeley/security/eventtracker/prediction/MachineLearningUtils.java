package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import weka.core.Instance;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

/**
 * Utility class with core machine learning methods that act on
 * <tt>EventModel</tt>s.
 */
public class MachineLearningUtils {

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability.
	 */
	static SortedSet<PredictedPair> getEventDistribution(EventModel eModel) {
		SortedSet<PredictedPair> predictionResults = new TreeSet<PredictedPair>(
				new PredictedPairComparator());
		if (!eModel.isEmpty()) {
			Instance newEventInstance = eModel.newInstance();
			double[] predictions;
			try {
				predictions = eModel.getClassifer().distributionForInstance(
						newEventInstance);
			} catch (Exception e) {
				// Huh?
				throw new RuntimeException(e);
			}

			for (int attributeIndex = 0; attributeIndex < predictions.length; attributeIndex++) {
				predictionResults.add(new PredictedPair(eModel
						.getEventName(attributeIndex),
						predictions[attributeIndex]));
			}
		}
		return predictionResults;
	}

	/**
	 * Predicts the name of an event that might be starting now.
	 * 
	 * @param eventModel
	 *            the eventModel to use for predicting the eventNames.
	 * 
	 * @return the predicted name.
	 */
	static List<String> predictEventNames(EventModel eventModel) {
		SortedSet<PredictedPair> predictionResults = getEventDistribution(eventModel);

		List<String> eventNames = new ArrayList<String>(
				predictionResults.size());
		for (PredictedPair predictedResult : predictionResults) {
			eventNames.add(predictedResult.getName());
		}
		return eventNames;
	}

	/**
	 * Converts the given events to classifiable instances.
	 * 
	 * @param eventCursor
	 *            the events to convert to <tt>Instances</tt>.
	 * @param eventAttributes
	 *            the attributes/features to classify on.
	 * @return a collection of <tt>Instances</tt> to classify.
	 */
	static EventInstances eventsToInstances(EventCursor eventCursor) {
		EventInstances eventData = newBlankTrainingData();
		eventData.setClassIndex(eventData.numAttributes() - 1);

		while (eventCursor.moveToNext()) {
			eventData.add(eventCursor.getEvent());
		}
		return eventData;
	}

	static EventInstances newBlankTrainingData() {
		return new EventInstances(generateEventNames());
	}

	/**
	 * Retrieves the event names to classify on. Currently, these are the names
	 * of events that have occurred more than once.
	 * 
	 * @return a set of event names
	 */
	private static Set<String> generateEventNames() {
		Set<String> names = new HashSet<String>();
		Set<String> repeatedNames = new HashSet<String>();
		EventCursor allEventsCursor = EventManager.getManager()
				.fetchUndeletedEvents();
		EventEntry currentEvent;
		while (allEventsCursor.moveToNext()) {
			currentEvent = allEventsCursor.getEvent();
			if (!names.add(currentEvent.mName)) {
				repeatedNames.add(currentEvent.mName);
			}
		}
		return repeatedNames;
	}

	private static class PredictedPairComparator implements
			Comparator<PredictedPair> {

		@Override
		public int compare(PredictedPair left, PredictedPair right) {
			return Double.compare(left.getLikelihood(), right.getLikelihood());
		}

	}
}
