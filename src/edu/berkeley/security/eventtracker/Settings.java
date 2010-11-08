package edu.berkeley.security.eventtracker;

import android.os.Bundle;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends EventActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.settings;
	}
}
