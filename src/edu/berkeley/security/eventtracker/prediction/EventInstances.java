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

class EventInstances extends Instances {

	/** For serialization. */
	private static final long serialVersionUID = -6786266809708280069L;

	private final Collection<String> classifiedEventNames;

	EventInstances(Collection<String> eventNames) {
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
	boolean add(EventEntry event) {
		Instance eventInstance = newInstance(event);
		return eventInstance == null ? false : this.add(eventInstance);
	}

	List<Attribute> getAttributes() {
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
	Instance newInstance() {
		return newInstance(new EventEntry(), false);
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
		return newInstance(event, true);
	}

	



}
