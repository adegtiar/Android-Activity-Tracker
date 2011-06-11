package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import weka.core.Attribute;
import weka.core.Instance;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * Utility class with core machine learning methods that act on
 * <tt>EventModel</tt>s.
 */
class MachineLearningUtils {

	/**
	 * Retrieves the event names to classify on from the given list of
	 * attributes.
	 * 
	 * @param eventAttributes
	 *            the attributes to classify on.
	 * @return a <tt>Set</tt> of names to classify on.
	 */
	static Set<String> getClassifiedNames(ArrayList<Attribute> eventAttributes) {
		@SuppressWarnings("unchecked")
		Enumeration<String> eventNames = eventAttributes.get(
				eventAttributes.size() - 1).enumerateValues();
		HashSet<String> classifiedNames = new HashSet<String>();
		while (eventNames.hasMoreElements())
			classifiedNames.add(eventNames.nextElement());
		return classifiedNames;
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability.
	 */
	static SortedMap<Double, String> getEventDistribution(EventModel eModel,
			List<Attribute> attributes) {
		Instance partialInstance = PredictionService.eventToInstance(
				new EventEntry(), attributes);
		TreeMap<Double, String> predictionResults = new TreeMap<Double, String>(
				new OppositeDoubleComparator());
		if (!eModel.isEmpty()) {
			double[] predictions;
			try {
				predictions = eModel.getModel().distributionForInstance(
						partialInstance);
			} catch (Exception e) {
				// Huh?
				throw new RuntimeException();
			}
	
			for (int attributeIndex = 0; attributeIndex < predictions.length; attributeIndex++) {
				predictionResults.put(predictions[attributeIndex],
						getEventName(attributeIndex, attributes));
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
	static List<String> predictEventNames(EventModel eventModel,
			List<Attribute> attributes) {
		Map<Double, String> predictionResults = getEventDistribution(
				eventModel, attributes);

		List<String> eventNames = new ArrayList<String>(
				predictionResults.size());
		for (Entry<Double, String> predictedResult : predictionResults
				.entrySet())
			eventNames.add(predictedResult.getValue());
		return eventNames;
	}

	/**
	 * A Double comparator that prioritizes higher values over lower ones.
	 */
	private static class OppositeDoubleComparator implements Comparator<Double> {

		@Override
		public int compare(Double arg0, Double arg1) {
			return arg1.compareTo(arg0);
		}

	}

	/**
	 * Gets the name of an event based on its index in the attributes.
	 * 
	 * @param attributeIndex
	 *            the index of the attribute to get the name of.
	 * @param attributes
	 *            the list of attributes.
	 * @return the name of the event the attribute at the index corresponds to.
	 */
	static String getEventName(double attributeIndex, List<Attribute> attributes) {
		Attribute classAttribute = attributes.get(attributes.size() - 1);
		return classAttribute.value((int) attributeIndex);
	}

}
