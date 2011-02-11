package edu.berkeley.security.eventtracker.maps;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.maps.MapView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MyMapView extends MapView{


	public MyMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public void setMapBoundsToPois(List<GeoPoint> items, double hpadding, double vpadding) {

	    MapController mapController = this.getController();
	    // If there is only on one result
	    // directly animate to that location

	    if (items.size() == 1) { // animate to the location
	        mapController.animateTo(items.get(0));
	    } else {
	        // find the lat, lon span
	        int minLatitude = Integer.MAX_VALUE;
	        int maxLatitude = Integer.MIN_VALUE;
	        int minLongitude = Integer.MAX_VALUE;
	        int maxLongitude = Integer.MIN_VALUE;

	        // Find the boundaries of the item set
	        for (GeoPoint item : items) {
	            int lat = item.getLatitudeE6(); int lon = item.getLongitudeE6();

	            maxLatitude = Math.max(lat, maxLatitude);
	            minLatitude = Math.min(lat, minLatitude);
	            maxLongitude = Math.max(lon, maxLongitude);
	            minLongitude = Math.min(lon, minLongitude);
	        }

	        // leave some padding from corners
	        // such as 0.1 for hpadding and 0.2 for vpadding
	        maxLatitude = maxLatitude + (int)((maxLatitude-minLatitude)*hpadding);
	        minLatitude = minLatitude - (int)((maxLatitude-minLatitude)*hpadding);

	        maxLongitude = maxLongitude + (int)((maxLongitude-minLongitude)*vpadding);
	        minLongitude = minLongitude - (int)((maxLongitude-minLongitude)*vpadding);

	        // Calculate the lat, lon spans from the given pois and zoom
	        mapController.zoomToSpan(Math.abs(maxLatitude - minLatitude), Math
	.abs(maxLongitude - minLongitude));

	        // Animate to the center of the cluster of points
	        mapController.animateTo(new GeoPoint(
	              (maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2));
	    }
	} // end of the method

}
