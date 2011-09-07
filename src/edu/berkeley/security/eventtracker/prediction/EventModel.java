package edu.berkeley.security.eventtracker.prediction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instance;
import android.util.Base64;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * Wrapper for the prediction <tt>Classifier</tt>.
 * <p>
 * TODO consider extending a classifier.
 */
class EventModel {

	private NaiveBayesUpdateable mClassifier;
	private EventInstances mTrainingData; // TODO remove this?

	/**
	 * Constructs an empty {@code EventModel}.
	 */
	EventModel(EventInstances trainingData) {
		mTrainingData = trainingData;
	}

	/**
	 * Constructs an {@code EventModel} from a {@link NaiveBayesUpdateable}.
	 * 
	 * @param model
	 *            a non-empty model
	 */
	private EventModel(NaiveBayesUpdateable model) {
		mClassifier = model;
		// TODO fix training data serialization
	}

	/**
	 * Constructs an <tt>EventModel</tt> that has been previously serialized
	 * using the <tt>serializeToString()</tt> method. TODO test this
	 * 
	 * @return the de-serialized <tt>EventModel</tt>
	 * @throws IOException
	 *             the string is not a valid serialized <tt>EventModel</tt>, or
	 *             an unexpected de-serialization error occurred
	 */
	static EventModel fromSerializedString(String serializedModel)
			throws IOException {
		byte[] serializedClassifier = Base64.decode(serializedModel, 0);
		ByteArrayInputStream is = new ByteArrayInputStream(serializedClassifier);
		ObjectInputStream ois = new ObjectInputStream(is);
		try {
			return new EventModel((NaiveBayesUpdateable) ois.readObject());
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Serializes the eventModel into a string.
	 * <p>
	 * TODO check for correctness
	 * 
	 * @return the string-serialized form of the classifier.
	 * @throws IOException
	 *             the classifier is invalid, or an unexpected serialization
	 *             error occurred
	 */
	String serializeToString() throws IOException {
		if (isEmpty())
			return "";
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);

		oos.writeObject(getClassifer());
		oos.flush();
		return Base64.encodeToString(os.toByteArray(), 0);
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
				predictions = getClassifer().distributionForInstance(
						newEventInstance);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			for (int attributeIndex = 0; attributeIndex < predictions.length; attributeIndex++) {
				predictionResults.add(new PredictedPair(
						getEventName(attributeIndex),
						predictions[attributeIndex]));
			}
		}
		return predictionResults;
	}

	/**
	 * Constructs an instance belonging to this model corresponding to an event
	 * that just started.
	 * 
	 * @return a new {@link Instance} corresponding to a new event
	 */
	Instance newInstance() {
		return mTrainingData.newInstance();
	}

	/**
	 * Incrementally updates the model with new event data.
	 * 
	 * @param newEvent
	 *            the event to update the model with
	 * @throws Exception
	 */
	void updateModel(EventEntry newEvent) throws Exception {
		Instance eventInstance = mTrainingData.newInstance(newEvent);
		if (eventInstance == null) {
			throw new IllegalArgumentException("Invalid event: " + newEvent);
		}
		if (mClassifier == null) {
			mTrainingData.add(eventInstance);
		} else {
			getClassifer().updateClassifier(eventInstance);
		}
		// TODO check if isEmpty works?
	}

	/**
	 * Whether the model has any instances classified.
	 * 
	 * @return true if the model has any instances classified, otherwise false
	 */
	boolean isEmpty() {
		return mTrainingData.isEmpty();
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
	String getEventName(double attributeIndex) {
		return mTrainingData.classAttribute().value((int) attributeIndex);
	}

	/**
	 * Returns the wrapped <tt>Classifier</tt>.
	 * 
	 * @return the wrapped <tt>Classifier</tt>
	 */
	private NaiveBayesUpdateable getClassifer() {
		if (mClassifier == null) {
			mClassifier = new NaiveBayesUpdateable();
			try {
				buildClassifier();
			} catch (Exception e) {
				throw new RuntimeException(e); // TODO make this more graceful
			}
		}
		return mClassifier;
	}

	/**
	 * Initializes the model with the given instances.
	 * <p>
	 * TODO check for correctness
	 * 
	 * @param eventsToClassify
	 *            the events to classify
	 * @throws Exception
	 */
	private void buildClassifier() throws Exception {
		if (mTrainingData.size() > 0) {
			getClassifer().buildClassifier(mTrainingData);
		}
	}

	private static class PredictedPairComparator implements
			Comparator<PredictedPair> {

		@Override
		public int compare(PredictedPair left, PredictedPair right) {
			return Double.compare(right.getLikelihood(), left.getLikelihood());
		}

	}
}
