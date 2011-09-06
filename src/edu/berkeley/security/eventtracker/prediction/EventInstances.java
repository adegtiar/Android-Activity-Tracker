package edu.berkeley.security.eventtracker.prediction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;

public class EventInstances extends Instances {

	/** For serialization. */
	private static final long serialVersionUID = -6786266809708280069L;

	private final Collection<String> classifiedEventNames;

	public EventInstances(Collection<String> eventNames) {
		super("EventData", generateEventAttributes(eventNames), 10);
		this.classifiedEventNames = eventNames;
	}

	/**
	 * Adds an event as an instance of this set.
	 * 
	 * @param event
	 *            an event to add that is neither empty nor null
	 * @return true if it was added successfully
	 */
	public boolean add(EventEntry event) {
		Instance eventInstance = newInstance(event);
		return eventInstance == null ? false : this.add(eventInstance);
	}

	public List<Attribute> getAttributes() {
		return m_Attributes;
	}

	/**
	 * Extracts the relevant attributes from an <tt>EventEntry</tt> and
	 * constructing the corresponding <tt>Instance</tt>.
	 * 
	 * @param event
	 *            the <tt>EventEntry</tt> to convert
	 * @return the new {@link Instance}, or null if the event was invalid
	 */
	Instance newInstance(EventEntry event) {
		return eventToInstance(event, Calendar.getInstance(), getAttributes());
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
	private Instance eventToInstance(EventEntry event, Calendar localCal,
			List<Attribute> attributes) {
		// Validate event
		if (event.mName == null || event.mName.length() == 0) {
			throw new IllegalArgumentException("Unnamed event");
		}
		if (classifiedEventNames.contains(event.mName)) {
			return null;
		}
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
	 * Constructs a list of attributes to classify on.
	 * 
	 * @param eventNames
	 *            the event names to classify
	 * @return an <tt>ArrayList</tt> of event attributes
	 */
	private static ArrayList<Attribute> generateEventAttributes(
			Collection<String> eventNames) {
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

}
