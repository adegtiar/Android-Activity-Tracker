package edu.berkeley.security.eventtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends EventActivity {

	public static final String PREFERENCE_FILENAME = "SettingPrefs";
	public static final String GPSTime = "GPSTime";
	public static final String Sensitivity = "Sensitivity";
	public static final String isGPSEnabled = "isGPSEnabled";
	public static final String areNotificationsEnabled = "notificationsEnabled";

	private CheckBox GPSEnabled;
	private NumberPicker GPSSensitivity;
	private NumberPicker GPSUpdateTime;

	private CheckBox notificationsEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GPSUpdateTime = (NumberPicker) findViewById(R.id.Picker1);
		GPSSensitivity = (NumberPicker) findViewById(R.id.Picker2);
		GPSEnabled = (CheckBox) findViewById(R.id.plain_cb);

		notificationsEnabled = (CheckBox) findViewById(R.id.notifications_cb);

		focusOnNothing();
		initializeButtons();

		GPSEnabled.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				CheckBox gps = (CheckBox) view;
				if (gps.isChecked())
					serviceIntent.putExtra("gps", true);
				else{
					serviceIntent.putExtra("gps", false);
				}
				if( mEventManager.isTracking())
					startService(serviceIntent);
				
				 String zipFileName = "C:/myFolder/myZip.zip";
				 String directoryToExtractTo = "C:/myFolder/unzipped/";
				 unzipMyZip(zipFileName, directoryToExtractTo);

				GPSSensitivity.setEnabled(gps.isChecked());
				GPSUpdateTime.setEnabled(gps.isChecked());

			}
		});

		notificationsEnabled
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						Settings.this.updatePreferences();
						Settings.this.updateTrackingStatus();
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
		prefEditor.putBoolean(areNotificationsEnabled, notificationsEnabled
				.isChecked());
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
	
	public static boolean areNotificationsEnabled() {
		return settings.getBoolean(areNotificationsEnabled, true);
	}

}
