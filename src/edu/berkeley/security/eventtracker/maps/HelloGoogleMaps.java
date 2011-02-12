package edu.berkeley.security.eventtracker.maps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
		
		
			EventEntry entry = (EventEntry) this.getIntent().getExtras().getSerializable("EventData");
			gpsList=new ArrayList<GPSCoordinates>();
			gpsList=entry.getGPSCoordinates();
			if(gpsList.size()==0){
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				  dialog.setMessage("No data available yet");
				  dialog.show();
			}
		
		geopointList=new ArrayList<GeoPoint>();
		int index=0;
		for(GPSCoordinates gps:gpsList){
			
			 GeoPoint point = new GeoPoint(
			            (int) (gps.getLatitude() * 1E6), 
			            (int) (gps.getLongitude() * 1E6));
			 geopointList.add(point);
			
			int icon;
			if(index==0)
				icon=R.drawable.green_flag1;
			else if(index==gpsList.size()-1)
				icon=R.drawable.red_flag;
			else
				icon=R.drawable.androidmarker;
			Drawable drawable=this.getResources().getDrawable(icon); 
			HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this); 
			OverlayItem overlayitem = new OverlayItem(point, entry.mName,new SimpleDateFormat().format(new Date(gps.getTime())));
			itemizedoverlay.addOverlay(overlayitem);
			mapOverlays.add(itemizedoverlay);
			index++;
		}
		
		
		
	    mapView.setMapBoundsToPois(geopointList, .10, .10);
	 }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
	    MapView mapView = (MapView) findViewById(R.id.mapview); 
	    mapView.setBuiltInZoomControls(true);
		return false;
	}
	
}