package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

abstract public class EventActivity extends Activity{
	
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
}
