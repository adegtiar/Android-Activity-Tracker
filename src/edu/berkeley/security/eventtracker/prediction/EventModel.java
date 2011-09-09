package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;

/**
 * A wrapper for the prediction <tt>Classifier</tt>.
 */
class EventModel {

	private final Instances mBlankInstances;
	private ArrayList<Attribute> mAttributes;
	private Collection<String> mEventNames;
	private DefaultClassifier mClassifier;
	private boolean isEmpty;

	EventModel(Collection<String> eventNames) {
		mAttributes = generateEventAttributes(eventNames);
		mEventNames = eventNames;
		Instances eventInstances = new Instances("EventData", mAttributes, 0);
		eventInstances.setClassIndex(mAttributes.size() - 1);
		mBlankInstances = new Instances(eventInstances, 0);
		mClassifier = new DefaultClassifier(eventInstances);
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability
	 */
	SortedSet<PredictedPair> getEventDistribution() {
		SortedSet<PredictedPair> predictionResults = new TreeSet<PredictedPair>(
				new PredictedPairComparator());
		if (!isEmpty()) {
			Instance newEventInstance = newInstance();
			double[] predictions;
			try {
				predictions = mClassifier.distributionForInstance(newEventInstance);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			for (int attributeIndex = 0; attributeIndex < predictions.length; attributeIndex++) {
				predictionResults.add(new PredictedPair(getEventName(attributeIndex),
						predictions[attributeIndex]));
			}
		}
		return predictionResults;
	}

	/**
	 * Incrementally updates the model with new event data.
	 * 
	 * @param newEvent
	 *            the event to update the model with
	 * @throws Exception
	 */
	void updateModel(EventEntry newEvent) {
		Instance eventInstance = newInstance(newEvent, true);
		if (eventInstance == null) {
			return;
		} else {
			try {
				mClassifier.updateClassifier(eventInstance);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			isEmpty = false;
		}
	}

	static enum DayOfWeek {
		SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	}

	/**
	 * Finds the <tt>DayOfWeek</tt> for the given <tt>Calendar</tt>.
	 * 
	 * @param calendar
	 *            the <tt>Calendar</tt> to extract the time from
	 * @return the <tt>DayOfWeek</tt> of the given time
	 */
	private static DayOfWeek getDay(Calendar calendar) {
		return DayOfWeek.values()[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	}

	/**
	 * Constructs an instance belonging to this model corresponding to an event
	 * that just started.
	 * 
	 * @return a new {@link Instance} corresponding to a new event
	 */
	private Instance newInstance() {
		return newInstance(new EventEntry(), false);
	}

	/**
	 * Whether the model has any instances classified.
	 * 
	 * @return true if the model has any instances classified, otherwise false
	 */
	private boolean isEmpty() {
		return isEmpty;
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
	private String getEventName(double attributeIndex) {
		return classAttribute().value((int) attributeIndex);
	}

	private Attribute classAttribute() {
		return mAttributes.get(mAttributes.size() - 1);
	}

	/**
	 * Extracts the relevant attributes from an <tt>EventEntry</tt> and
	 * constructing the corresponding <tt>Instance</tt>.
	 * 
	 * @param event
	 *            the <tt>EventEntry</tt> to convert
	 * @return the new {@link Instance}, or null if the event was invalid
	 */
	private Instance newInstance(EventEntry event, boolean checkValidEvent) {
		return eventToInstance(event, checkValidEvent);
	}

	/**
	 * A comparator that orders {@link PredictedPair} instances from high to low
	 * probability.
	 */
	private static class PredictedPairComparator implements Comparator<PredictedPair> {

		@Override
		public int compare(PredictedPair left, PredictedPair right) {
			return Double.compare(right.getLikelihood(), left.getLikelihood());
		}

	}

	/**
	 * Constructs a list of attributes to classify on.
	 * 
	 * @param eventNames
	 *            the event names to classify
	 * @return an <tt>ArrayList</tt> of event attributes
	 */
	private static ArrayList<Attribute> generateEventAttributes(Collection<String> eventNames) {
		// Declare a numeric hourOfDay
		Attribute attrHourOfDay = new Attribute("hourOfDay");
		// Declare a numeric Longitude
		Attribute attrLongitude = new Attribute("longitude");
		// Declare a numeric Longitude
		Attribute attrLatitude = new Attribute("latitude");
		// Declare a nominal dayOfWeek attribute along with its values
		ArrayList<String> daysOfWeekNominal = new ArrayList<String>(7);
		for (DayOfWeek day : DayOfWeek.values())
			daysOfWeekNominal.add(day.toString());
		Attribute attrDayOfWeek = new Attribute("dayOfWeek", daysOfWeekNominal);
		// Declare the event name attribute along with its values
		ArrayList<String> namesNominal = new ArrayList<String>(eventNames);
		Attribute attrNamesNominal = new Attribute("eventNames", namesNominal);
		// Declare the feature vector
		ArrayList<Attribute> eventAttributes = new ArrayList<Attribute>(5);
		eventAttributes.add(attrHourOfDay);
		eventAttributes.add(attrDayOfWeek);
		eventAttributes.add(attrLatitude);
		eventAttributes.add(attrLongitude);
		eventAttributes.add(attrNamesNominal);
		return eventAttributes;
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
	private Instance eventToInstance(EventEntry event, boolean checkValidEvent) {
		if (checkValidEvent) {
			// Validate event
			if (event.mName == null || event.mName.length() == 0
					|| !mEventNames.contains(event.mName)) {
				return null;
			}
		}
		Calendar localCal = Calendar.getInstance();
		// Create the instance
		Instance eventInstance = new DenseInstance(5);
		// Add start hour
		localCal.setTimeInMillis(event.mStartTime);
		eventInstance.setValue(mAttributes.get(0), localCal.get(Calendar.HOUR_OF_DAY));
		// Add start day of week
		eventInstance.setValue(mAttributes.get(1), getDay(localCal).toString());
		// Add starting position (if exists)
		List<GPSCoordinates> eventCoords = event.getGPSCoordinates();
		if (eventCoords.size() > 0) {
			GPSCoordinates startPos = eventCoords.get(0);
			eventInstance.setValue(mAttributes.get(2), startPos.getLatitude());
			eventInstance.setValue(mAttributes.get(3), startPos.getLongitude());
		}
		// Add name (if exists)
		if (event.mName != null && event.mName.length() != 0) {
			eventInstance.setValue(mAttributes.get(4), event.mName);
		}
		// Associate with this set of instances
		eventInstance.setDataset(mBlankInstances);
		return eventInstance;
	}

}
