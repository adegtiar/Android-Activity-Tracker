package edu.berkeley.security.eventtracker.eventdata;

public class GPSCoordinates {
	
	public GPSCoordinates(int latitude, int longitude){
		this.mLongitude=longitude;
		this.mLatitude=latitude;
	}

	public int getLatitude(){
		return mLatitude;
	}
	public int getLongitude(){
		return mLongitude;
	}
	
	private int mLatitude;
	private int mLongitude;
}
