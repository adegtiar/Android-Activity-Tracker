package edu.berkeley.security.eventtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends EventActivity {

	public static final String PREFERENCE_FILENAME = "SettingPrefs";
	public static final String GPSTime = "GPSTime";
	public static final String Sensitivity = "Sensitivity";
	public static final String isGPSEnabled = "isGPSEnabled";

	private CheckBox GPSEnabled;
	private NumberPicker GPSSensitivity;
	private NumberPicker GPSUpdateTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GPSUpdateTime = (NumberPicker) findViewById(R.id.Picker1);
		GPSSensitivity = (NumberPicker) findViewById(R.id.Picker2);
		GPSEnabled = (CheckBox) findViewById(R.id.plain_cb);

		focusOnNothing();
		initializeButtons();

		GPSEnabled.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				CheckBox gps = (CheckBox) view;
				if (gps.isChecked() && mEventManager.isTracking()) {	
					serviceIntent.putExtra("gps", true);
				} else {
					serviceIntent.putExtra("gps", false);
				}
				startService(serviceIntent);
				GPSSensitivity.setEnabled(gps.isChecked());
				GPSUpdateTime.setEnabled(gps.isChecked());

			}
		});
	}

	@Override
	protected void onPause() {
		updatePreferences();
		super.onPause();
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.settings;
	}

	private void initializeButtons() {
		boolean enableGPS = isGPSEnabled();
		GPSEnabled.setChecked(enableGPS);
		GPSSensitivity.setValue(settings.getInt(Sensitivity, 0));
		GPSUpdateTime.setValue(settings.getInt(GPSTime, 1));
		GPSSensitivity.setEnabled(enableGPS);
		GPSUpdateTime.setEnabled(enableGPS);

	}

	private void updatePreferences() {

		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putBoolean(isGPSEnabled, GPSEnabled.isChecked());
		prefEditor.putInt(GPSTime, GPSUpdateTime.getValue());
		prefEditor.putInt(Sensitivity, GPSSensitivity.getValue());
		prefEditor.commit();
	}

	private void focusOnNothing() {
		GPSSensitivity.valueText.clearFocus();
		GPSUpdateTime.valueText.clearFocus();
		LinearLayout dummyLayout = (LinearLayout) findViewById(R.id.dummyLayout2);
		dummyLayout.requestFocus();

	}

	public static boolean isGPSEnabled() {
		return settings.getBoolean(isGPSEnabled, false);

	}

	public static int getGPSSensitivity() {
		return settings.getInt(Sensitivity, 0);
	}

	public static int getGPSUpdateTime() {
		return settings.getInt(GPSTime, 1);
	}

}
