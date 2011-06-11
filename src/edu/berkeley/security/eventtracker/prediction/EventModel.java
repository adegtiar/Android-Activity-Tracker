package edu.berkeley.security.eventtracker.prediction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instance;
import weka.core.Instances;
import android.util.Base64;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;

/**
 * Wrapper for the Machine Learning <tt>Classifier</tt>.
 */
class EventModel {

	private NaiveBayesUpdateable mModel;
	private Instances mTrainingData;
	private boolean isEmpty;

	EventModel() {
		isEmpty = true;
	}

	private EventModel(NaiveBayesUpdateable model) {
		mModel = model;
	}

	/**
	 * Returns the wrapped <tt>Classifier</tt>.
	 * 
	 * @return the wrapped <tt>Classifier</tt>.
	 */
	NaiveBayesUpdateable getModel() {
		if (mModel == null)
			mModel = new NaiveBayesUpdateable();
		return mModel;
	}

	/**
	 * Updates the model with the given instances.
	 * 
	 * @param eventsToClassify
	 *            the events to classify.
	 * @throws Exception
	 */
	void buildClassifier(Instances eventsToClassify) throws Exception {
		if (eventsToClassify.size() > 0) {
			isEmpty = false;
			mTrainingData = eventsToClassify;
			getModel().buildClassifier(eventsToClassify);
		}

	}

	/**
	 * Incrementally updates the model with new event data.
	 * 
	 * @param newEvent
	 *            the event to update the model with.
	 * @throws Exception
	 */
	void updateModel(EventEntry newEvent) throws Exception {
		isEmpty = false;
		Instance newInstance = PredictionService.eventToInstance(newEvent);
//		if (mTrainingData != null)
//			mTrainingData.add(newInstance);
		getModel().updateClassifier(newInstance);
	}
	
	Instances getInstances() {
		return mTrainingData;
	}
	
	/**
	 * Whether the model has any instances classified.
	 * 
	 * @return true if the model has any instances classified, otherwise false;
	 */
	boolean isEmpty() {
		return isEmpty;
	}

	/**
	 * Constructs an <tt>EventModel</tt> that has been previously serialized
	 * using the <tt>serializeToString()</tt> method.
	 * 
	 * @return the de-serialized <tt>EventModel</tt>.
	 * @throws IOException
	 *             the string is not a valid serialized <tt>EventModel</tt>, or
	 *             an unexpected de-serialization error occurred.
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
	 * 
	 * @return the string-serialized form of the classifier.
	 * @throws IOException
	 *             the classifier is invalid, or an unexpected serialization
	 *             error occurred.
	 */
	String serializeToString() throws IOException {
		if (isEmpty)
			return null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);

		oos.writeObject(getModel());
		oos.flush();
		return Base64.encodeToString(os.toByteArray(), 0);
	}
}
