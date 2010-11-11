package edu.berkeley.security.eventtracker.eventdata;

public class GPSCoordinates {

	public GPSCoordinates(double latitude, double longitude) {
		this.mLongitude = longitude;
		this.mLatitude = latitude;
	}

	public double getLatitude() {
		return mLatitude;
	}

	public double getLongitude() {
		return mLongitude;
	}

	private double mLatitude;
	private double mLongitude;
}
