package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewStub;

abstract public class EventActivity extends Activity{
	static final int trackingStringID = R.string.toolbarTracking;
	static final int notTrackingStringID = R.string.toolbarNotTracking;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        ViewStub v =(ViewStub) findViewById(R.id.content_view);
        v.setLayoutResource(getLayoutResource());
        v.inflate();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.event_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.settings_option:
	        startSettingsActivity();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * Launches the Settings activity.
	 */
	protected void startSettingsActivity() {
		Intent settingsIntent = new Intent(this, Settings.class);
		settingsIntent.putExtra(getString(R.string.isTracking), isTracking());
		startActivity(settingsIntent);
	}

	/**
	 * Launches the ListEvents activity.
	 */
	protected void startListEventsActivity() {
		Intent listIntent = new Intent(this, ListEvents.class);
		listIntent.putExtra(getString(R.string.isTracking), isTracking());
		startActivity(listIntent);
	}
	
	abstract protected boolean isTracking();
	
	abstract protected int getLayoutResource();
}
