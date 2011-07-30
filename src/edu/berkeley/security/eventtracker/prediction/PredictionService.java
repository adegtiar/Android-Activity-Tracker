package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;
import edu.berkeley.security.eventtracker.prediction.MachineLearningUtils.PredictedPair;

/**
 * Provides some public methods to predict which events may be starting.
 */
public class PredictionService {
	private enum DayOfWeek {
		SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	}

	/**
	 * The cached <tt>EventModel</tt>.
	 */
	private static EventModel mEventModel;

	private static ArrayList<Attribute> mAttributes;

	@SuppressWarnings("unused")
	private static boolean isDbUpdated;

	/**
	 * Predicts the names of events that might be starting now, in order of
	 * likelihood.
	 * 
	 * @return a list of predicted names.
	 */
	public static List<String> predictEventNames() {
		return MachineLearningUtils.predictEventNames(getEventModel(),
				getEventAttributes());
	}

	/**
	 * Calculates the distribution of probabilities over all predictable events.
	 * 
	 * @return a <tt>SortedMap</tt> mapping probabilities to events. In order
	 *         from highest to lowest probability.
	 */
	public static SortedSet<PredictedPair> getEventDistribution() {
		return MachineLearningUtils.getEventDistribution(getEventModel(),
				getEventAttributes());
	}

	/**
	 * Updates the model with a new event.
	 * 
	 * @param newEvent
	 *            the new event to add to the model.
	 */
	public static void updateEventModel(EventEntry newEvent) {
		try {
			if (mEventModel != null) {
				isDbUpdated = true;
				getEventModel().updateModel(newEvent);
			}
		} catch (Exception e) {
			// TODO make more graceful
			throw new RuntimeException(e);
		}
	}

	public static void syncModelToStorage() {
		// TODO implement
	}

	public static void markDbUnsupportedUpdated() {
		// TODO implement
		mEventModel = null;
		mAttributes = null;
	}

	/**
	 * Extracts the relevant attributes from an <tt>EventEntry</tt> and
	 * constructing the corresponding <tt>Instance</tt>
	 * 
	 * @param event
	 *            the <tt>EventEntry</tt> to convert.
	 * @return
	 */
	static Instance eventToInstance(EventEntry event) {
		return eventToInstance(event, Calendar.getInstance(),
				getEventAttributes());
	}

	/**
	 * Constructs a list of attributes to classify on.
	 * 
	 * @return an <tt>ArrayList</tt> of event attributes.
	 */
	static ArrayList<Attribute> getEventAttributes() {
		if (mAttributes == null) {
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
			Attribute attrDayOfWeek = new Attribute("dayOfWeek",
					daysOfWeekNominal);

			// Declare the event name attribute along with its values
			ArrayList<String> namesNominal = new ArrayList<String>(
					getEventNames());
			Attribute attrNamesNominal = new Attribute("eventNames",
					namesNominal);

			// Declare the feature vector
			ArrayList<Attribute> eventAttributes = new ArrayList<Attribute>(5);
			eventAttributes.add(attrHourOfDay);
			eventAttributes.add(attrDayOfWeek);
			eventAttributes.add(attrLatitude);
			eventAttributes.add(attrLongitude);
			eventAttributes.add(attrNamesNominal);
			mAttributes = eventAttributes;
		}
		return mAttributes;
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @return an <tt>EventModel</tt> built using the event data.
	 */
	private static EventModel getEventModel() {
		if (mEventModel == null) {
			EventCursor events = EventManager.getManager()
					.fetchUndeletedEvents();
			Instances eventInstances = eventsToInstances(events);
			return getEventModel(eventInstances);
		}
		return mEventModel;
	}

	/**
	 * Builds an <tt>EventModel</tt> over the event data.
	 * 
	 * @param instancesToClassify
	 *            the instances over which the build the classifier.
	 * @return a <tt>Classifier</tt> built using the event data.
	 */
	private static EventModel getEventModel(Instances instancesToClassify) {
		// Create a new model
		EventModel eModel = new EventModel();
		try {
			eModel.buildClassifier(instancesToClassify);
		} catch (Exception e) {
			// TODO make more graceful
			throw new RuntimeException();
		}
		return eModel;
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
	private static Instances eventsToInstances(EventCursor eventCursor) {
		Calendar calendar = Calendar.getInstance();

		Instances eventData = getBlankTrainingDataset();
		eventData.setClassIndex(eventData.numAttributes() - 1);
		Set<String> classifiedNames = MachineLearningUtils
				.getClassifiedNames(getEventAttributes());

		while (eventCursor.moveToNext()) {
			EventEntry nextEvent = eventCursor.getEvent();
			if (nextEvent.mName == null || nextEvent.mName.length() == 0) {
				continue;
			}
			if (classifiedNames.contains(nextEvent.mName)) {
				eventData.add(eventToInstance(nextEvent, calendar,
						getEventAttributes()));
			}
		}
		return eventData;
	}

	static Instances getTrainingDataset() {
		return eventsToInstances(EventManager.getManager()
				.fetchUndeletedEvents());
	}

	static Instances getBlankTrainingDataset() {
		return new Instances("EventData", getEventAttributes(), 0);
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
		Instance eventInstance = new DenseInstance(5);

		// Add start hour
		localCal.setTimeInMillis(event.mStartTime);
		eventInstance.setValue(attributes.get(0),
				localCal.get(Calendar.HOUR_OF_DAY));

		// Add start day of week
		eventInstance.setValue(attributes.get(1), getDay(localCal).toString());

		// Add starting position (if exists)
		List<GPSCoordinates> eventCoords = event.getGPSCoordinates();
		if (eventCoords.size() > 0) {
			GPSCoordinates startPos = eventCoords.get(0);
			eventInstance.setValue(attributes.get(2), startPos.getLatitude());
			eventInstance.setValue(attributes.get(3), startPos.getLongitude());
		}

		// Add name (if exists)
		if (event.mName != null && event.mName.length() != 0)
			eventInstance.setValue(attributes.get(4), event.mName);
		return eventInstance;
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
				.fetchUndeletedEvents();
		EventEntry currentEvent;
		while (allEventsCursor.moveToNext()) {
			currentEvent = allEventsCursor.getEvent();
			if (!names.add(currentEvent.mName))
				repeatedNames.add(currentEvent.mName);
		}
		return repeatedNames;
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

}
