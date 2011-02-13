package edu.berkeley.security.eventtracker.eventdata;

public class GPSCoordinates {

	public GPSCoordinates(double latitude, double longitude, long time) {
		this.mLongitude = longitude;
		this.mLatitude = latitude;
		this.mTime = time;
	}

	public double getLatitude() {
		return mLatitude;
	}

	public double getLongitude() {
		return mLongitude;
	}

	public long getTime() {
		return mTime;
	}

	private long mTime;
	private double mLatitude;
	private double mLongitude;

	@Override
	public String toString() {
		return "(Latitude: " + Double.toString(mLatitude) + "Longitude: "
				+ Double.toString(mLongitude) + ")";
	}
}
