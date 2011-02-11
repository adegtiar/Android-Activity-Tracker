package edu.berkeley.security.eventtracker.maps;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;

import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.R;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.GPSCoordinates;

public class HelloGoogleMaps extends MapActivity {
	 MyMapView mapView;
	 private MapController mc;
	 List<GPSCoordinates> gpsList;
	 List< GeoPoint> geopointList;
	 List<Overlay> mapOverlays;
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	   
			super.onCreate(savedInstanceState);
			setContentView(R.layout.maps);
			
			 mapView = (MyMapView) findViewById(R.id.mapview);
			 mapOverlays = mapView.getOverlays();
	   
	}
	
	 @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		generateGPSDATA();
		 mapView.setMapBoundsToPois(geopointList, .10, .10);
//	    String coordinates[] = {"40.747778", "-73.985556"};
//       double lat = Double.parseDouble(coordinates[0]);
//       double lng = Double.parseDouble(coordinates[1]);
//
//       GeoPoint point = new GeoPoint(
//           (int) (lat * 1E6), 
//           (int) (lng * 1E6));
		 

//		Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
//		HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this);
////		GeoPoint point = new GeoPoint(19240000,-99120000);
//		OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!", "I'm in Mexico City!");
//		itemizedoverlay.addOverlay(overlayitem);
//		mapOverlays.add(itemizedoverlay);
//       mc.animateTo(point);
//       mc.setZoom(17); 
//       mapView.invalidate(); 
	}


	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
	    MapView mapView = (MapView) findViewById(R.id.mapview); 
	    mapView.setBuiltInZoomControls(true);
		return false;
	}
	private void generateGPSDATA(){
		EventEntry entry=EventActivity.mEventManager.getCurrentEvent();
		gpsList=new ArrayList<GPSCoordinates>();
		gpsList=entry.getGPSCoordinates();
		if(gpsList.size()==0){
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			  dialog.setMessage("No data available yet");
			  dialog.show();
		}
		geopointList=new ArrayList<GeoPoint>();
		for(GPSCoordinates gps:gpsList){
			 GeoPoint point = new GeoPoint(
			            (int) (gps.getLatitude() * 1E6), 
			            (int) (gps.getLongitude() * 1E6));
			 geopointList.add(point);
			Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker); 
			HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this); 
			OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!", "I'm in Mexico City!");
			itemizedoverlay.addOverlay(overlayitem);
			mapOverlays.add(itemizedoverlay);
		}
		
	}
}