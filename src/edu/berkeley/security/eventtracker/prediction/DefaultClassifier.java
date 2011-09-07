package edu.berkeley.security.eventtracker.prediction;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

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
	DefaultClassifier(Classifier classifier) {
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

	public void buildClassifier() throws Exception {
		if (isBuilt()) {
			throw new IllegalStateException("Classifier is already built.");
		}
		buildClassifier(mUnbuiltInstances);
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
}
