package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

/**
 * Provides some public methods to predict which event might be starting.
 */
public class WekaInterface {
	private enum DayOfWeek {
		SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	}

	/**
	 * Scans through the database of events and outputs classifiable WEKA
	 * instances.
	 * 
	 * @param eventAttributes
	 *            the attributes/features to classify on.
	 * @return a collection of WEKA <tt>Instances</tt> to classify.
	 */
	public static Instances dumpDbWeka(ArrayList<Attribute> eventAttributes) {
		EventCursor eventsCursor = EventManager.getManager()
				.fetchUndeletedEvents();
		Calendar calendar = Calendar.getInstance();

		Instances eventData = new Instances("EventData", eventAttributes, 0);
		eventData.setClassIndex(eventData.numAttributes() - 1);
		Set<String> classifiedNames = getClassifiedNames(eventAttributes);

		while (eventsCursor.moveToNext()) {
			EventEntry nextEvent = eventsCursor.getEvent();
			if (classifiedNames.contains(nextEvent.mName))
				eventData.add(eventToInstance(nextEvent, calendar,
						eventAttributes));
		}
		return eventData;
	}

	/**
	 * Retrieves the event names to classify on from the given list of
	 * attributes.
	 * 
	 * @param eventAttributes
	 *            the attributes to classify on.
	 * @return a <tt>Set</tt> of names to classify on.
	 */
	private static Set<String> getClassifiedNames(
			ArrayList<Attribute> eventAttributes) {
		Enumeration<String> eventNames = eventAttributes.get(
				eventAttributes.size() - 1).enumerateValues();
		HashSet<String> classifiedNames = new HashSet<String>();
		while (eventNames.hasMoreElements())
			classifiedNames.add(eventNames.nextElement());
		return classifiedNames;
	}

	/**
	 * Predicts the name of an event that might be starting now.
	 * 
	 * @return the predicted name.
	 */
	public static String predictEventName() {
		predictEventNames();
		ArrayList<Attribute> attributes = getEventAttributes();
		Instances eventInstances = dumpDbWeka(attributes);

		Instance partialInstance = eventToInstance(new EventEntry(),
				Calendar.getInstance(), attributes);
		partialInstance.setDataset(eventInstances);

		// Create a (naïve bayes) classifier
		Classifier cModel = (Classifier) new NaiveBayes();

		double prediction;
		try {
			cModel.buildClassifier(eventInstances);
			prediction = cModel.classifyInstance(partialInstance);
		} catch (Exception e) {
			// Huh?
			throw new RuntimeException();
		}

		return getEventName(prediction, attributes);
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability.
	 */
	public static SortedMap<Double, String> getEventDistribution() {
		ArrayList<Attribute> attributes = getEventAttributes();
		Instances eventInstances = dumpDbWeka(attributes);

		Instance partialInstance = eventToInstance(new EventEntry(),
				Calendar.getInstance(), attributes);
		partialInstance.setDataset(eventInstances);

		// Create a (naïve bayes) classifier
		Classifier cModel = (Classifier) new NaiveBayes();

		double[] predictions;
		try {
			cModel.buildClassifier(eventInstances);
			predictions = cModel.distributionForInstance(partialInstance);
		} catch (Exception e) {
			// Huh?
			throw new RuntimeException();
		}

		TreeMap<Double, String> predictionResults = new TreeMap<Double, String>(
				new OppositeDoubleComparator());
		for (int attributeIndex = 0; attributeIndex < predictions.length; attributeIndex++) {
			predictionResults.put(predictions[attributeIndex],
					getEventName(attributeIndex, attributes));
		}
		return predictionResults;
	}

	/**
	 * Predicts the name of an event that might be starting now.
	 * 
	 * @return the predicted name.
	 */
	public static List<String> predictEventNames() {
		Map<Double, String> predictionResults = getEventDistribution();

		List<String> eventNames = new ArrayList<String>(
				predictionResults.size());
		for (Entry<Double, String> predictedResult : predictionResults
				.entrySet())
			eventNames.add(predictedResult.getValue());
		return eventNames;
	}

	/**
	 * A comparator that prioritizes higher values over lower ones.
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
	public static String getEventName(double attributeIndex,
			List<Attribute> attributes) {
		Attribute classAttribute = attributes.get(attributes.size() - 1);
		return classAttribute.value((int) attributeIndex);
	}

	/**
	 * Extracts the relevant attributes from an <tt>EventEntry</tt> and
	 * constructing the corresponding <tt>Instance</tt>.
	 * 
	 * @param event
	 *            the <tt>EventEntry</tt> to convert.
	 * @param localCal
	 *            a calendar to use for calculating time-based attributes.
	 * @param attributes
	 *            the list of attributes to extract.
	 * @return the <tt>Instance</tt> corresponding to the <tt>EventEntry</tt>.
	 */
	private static Instance eventToInstance(EventEntry event,
			Calendar localCal, List<Attribute> attributes) {
		// Create the instance
		Instance eventInstance = new DenseInstance(3);
		localCal.setTimeInMillis(event.mStartTime);
		eventInstance.setValue(attributes.get(0),
				localCal.get(Calendar.HOUR_OF_DAY));
		eventInstance.setValue(attributes.get(1), getDay(localCal).toString());
		if (event.mName != null && event.mName.length() != 0)
			eventInstance.setValue(attributes.get(2), event.mName);
		return eventInstance;
	}

	/**
	 * Extracts the relevant attributes from an <tt>EventEntry</tt> and
	 * constructing the corresponding <tt>Instance</tt>.
	 * 
	 * @param event
	 *            the <tt>EventEntry</tt> to convert.
	 * @param attributes
	 *            the list of attributes to extract.
	 * @return the <tt>Instance</tt> corresponding to the <tt>EventEntry</tt>.
	 */
	private static Instance eventToInstance(EventEntry event,
			List<Attribute> attributes) {
		return eventToInstance(event, Calendar.getInstance(), attributes);
	}

	/**
	 * Finds the <tt>DayOfWeek</tt> for the given <tt>Calendar</tt>.
	 * 
	 * @param calendar
	 *            the <tt>Calendar</tt> to extract the time from.
	 * @return the <tt>DayOfWeek</tt> of the given time.
	 */
	private static DayOfWeek getDay(Calendar calendar) {
		return DayOfWeek.values()[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	}

	/**
	 * Constructs a list of attributes to classify on.
	 * 
	 * @return an <tt>ArrayList</tt> of event attributes.
	 */
	private static ArrayList<Attribute> getEventAttributes() {
		// Declare a numeric hourOfDay
		Attribute attrHourOfDay = new Attribute("hourOfDay");

		// Declare a nominal dayOfWeek attribute along with its values
		ArrayList<String> daysOfWeekNominal = new ArrayList<String>(7);
		for (DayOfWeek day : DayOfWeek.values())
			daysOfWeekNominal.add(day.toString());
		Attribute attrDayOfWeek = new Attribute("dayOfWeek", daysOfWeekNominal);

		// Declare the event name attribute along with its values
		ArrayList<String> namesNominal = new ArrayList<String>(getEventNames());
		Attribute attrNamesNominal = new Attribute("eventNames", namesNominal);

		// Declare the feature vector
		ArrayList<Attribute> eventAttributes = new ArrayList<Attribute>(3);
		eventAttributes.add(attrHourOfDay);
		eventAttributes.add(attrDayOfWeek);
		eventAttributes.add(attrNamesNominal);
		return eventAttributes;
	}

	/**
	 * Retrieves the event names to classify on. Currently, these are the names
	 * of events that have occurred more than once.
	 * 
	 * @return a set of event names.
	 */
	private static Set<String> getEventNames() {
		Set<String> names = new HashSet<String>();
		Set<String> repeatedNames = new HashSet<String>();
		EventCursor allEventsCursor = EventManager.getManager()
				.fetchAllEvents();
		EventEntry currentEvent;
		while (allEventsCursor.moveToNext()) {
			currentEvent = allEventsCursor.getEvent();
			if (!names.add(currentEvent.mName))
				repeatedNames.add(currentEvent.mName);
		}
		return repeatedNames;
	}
}
