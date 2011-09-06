package edu.berkeley.security.eventtracker.prediction;

public class PredictedPair {

	private String mName;
	private double mLikelihood;

	PredictedPair(String name, double likelihood) {
		mName = name;
		mLikelihood = likelihood;
	}

	public String getName() {
		return mName;
	}

	public double getLikelihood() {
		return mLikelihood;
	}

	@Override
	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass()))
			return false;
		PredictedPair otherPair = (PredictedPair) other;
		return mName.equals(otherPair.mName)
				&& mLikelihood == otherPair.mLikelihood;
	}

	@Override
	public String toString() {
		return "{" + getName() + ":" + " " + getLikelihood() + "}";
	}
}
