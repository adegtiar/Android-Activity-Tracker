package edu.berkeley.security.eventtracker.prediction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import android.util.Base64;

/**
 * Wraps a default classifier. Until the classifier is built, it updates a local
 * set of instances instead.
 */
public class DefaultClassifier implements Classifier, UpdateableClassifier {
	private Classifier mClassifier;
	private Instances mUnbuiltInstances;

	/**
	 * Constructs the classifier from an existing classifier.
	 * 
	 * @param classifier
	 *            a classifier that has already been built
	 */
	private DefaultClassifier(Classifier classifier) {
		mClassifier = classifier;
	}

	public DefaultClassifier(Instances data) {
		this(new NaiveBayesUpdateable());// TODO remove dependency
		mUnbuiltInstances = data;
	}

	@Override
	public void updateClassifier(Instance instance) throws Exception {
		if (!isBuilt()) {
			mUnbuiltInstances.add(instance);
		} else if (mClassifier instanceof UpdateableClassifier) {
			((UpdateableClassifier) mClassifier).updateClassifier(instance);
		} else {
			throw new UnsupportedOperationException("The classifier cannot be dynamically updated");
		}
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		mClassifier.buildClassifier(data);
		mUnbuiltInstances = null;
	}

	public void buildClassifier() {
		if (isBuilt()) {
			throw new IllegalStateException("Classifier is already built.");
		}
		try {
			buildClassifier(mUnbuiltInstances);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double classifyInstance(Instance instance) throws Exception {
		if (!isBuilt()) {
			buildClassifier();
		}
		return mClassifier.classifyInstance(instance);
	}

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		if (!isBuilt()) {
			buildClassifier();
		}
		return mClassifier.distributionForInstance(instance);
	}

	@Override
	public Capabilities getCapabilities() {
		return mClassifier.getCapabilities();
	}

	private boolean isBuilt() {
		return mUnbuiltInstances == null;
	}

	/**
	 * Constructs an <tt>EventModel</tt> that has been previously serialized
	 * using the <tt>serializeToString()</tt> method.
	 * <p>
	 * TODO finish test this
	 * 
	 * @return the de-serialized <tt>EventModel</tt>
	 * @throws IOException
	 *             the string is not a valid serialized <tt>EventModel</tt>, or
	 *             an unexpected de-serialization error occurred
	 */
	static DefaultClassifier fromSerializedString(String serializedModel) throws IOException {
		byte[] serializedClassifier = Base64.decode(serializedModel, 0);
		ByteArrayInputStream is = new ByteArrayInputStream(serializedClassifier);
		ObjectInputStream ois = new ObjectInputStream(is);
		try {
			return new DefaultClassifier((NaiveBayesUpdateable) ois.readObject());
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Serializes the eventModel into a string.
	 * <p>
	 * TODO finish and check this for correctness
	 * 
	 * @return the string-serialized form of the classifier.
	 * @throws IOException
	 *             the classifier is invalid, or an unexpected serialization
	 *             error occurred
	 */
	String serializeToString() throws IOException {
		if (!isBuilt() && mUnbuiltInstances.size() == 0) {
			return "";
		} else if (!isBuilt()) {
			buildClassifier();
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);

		oos.writeObject(mClassifier);
		oos.flush();
		return Base64.encodeToString(os.toByteArray(), 0);
	}
}
