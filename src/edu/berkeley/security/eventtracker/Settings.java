package edu.berkeley.security.eventtracker;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends EventActivity {

	public static final String PREFERENCE_FILENAME = "SettingPrefs";
	public static final String GPSTime = "GPSTime";
	public static final String Sensitivity = "Sensitivity";
	public static final String isGPSEnabled = "isGPSEnabled";
	public static final String areNotificationsEnabled = "notificationsEnabled";
	public static final String password = "password";
	public static final String isPasswordSet = "isPasswordEntered";
	public static final String PhoneNumber = "phoneNumber";
	public static final String UUIDOfDevice = "deviceUUID";
	public static final String POLL_TIME = "pollTime";
	private static final String isSychronizationEnabled = "enableSychronization";
	public static final String Registered = "registered";
	private static NumberPicker GPSSensitivity;
	private static NumberPicker GPSUpdateTime;
	private static CheckBox GPSEnabled;
	private static CheckBox notificationsEnabled;
	private static CheckBox sychronizeDataEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GPSUpdateTime = (NumberPicker) findViewById(R.id.Picker1);
		GPSSensitivity = (NumberPicker) findViewById(R.id.Picker2);
		GPSEnabled = (CheckBox) findViewById(R.id.plain_cb);
		notificationsEnabled = (CheckBox) findViewById(R.id.notifications_cb);
		sychronizeDataEnabled = (CheckBox) findViewById(R.id.synchronizeData_cb);
		focusOnNothing();
		if (getDeviceUUID().length() == 0) {
			setDeviceUUID();
		}

		initializeButtons();

		GPSEnabled.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				CheckBox gps = (CheckBox) view;
				if (gps.isChecked())
					gpsServiceIntent.putExtra("gps", true);
				else {
					gpsServiceIntent.putExtra("gps", false);
				}
				if (mEventManager.isTracking())
					startService(gpsServiceIntent);

				GPSSensitivity.setEnabled(gps.isChecked());
				GPSUpdateTime.setEnabled(gps.isChecked());

			}
		});

		notificationsEnabled
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						Settings.updatePreferences();
						Settings.this.updateTrackingStatus();
					}
				});
		sychronizeDataEnabled
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {

						if (!isPasswordSet() && isChecked) {
							sychronizeDataEnabled.setChecked(false);
							// TODO make user set a password
							Bundle bundle = new Bundle();
							bundle.putBoolean("Settings", true);
							showDialog(DIALOG_TEXT_ENTRY, bundle);
						}

					}
				});
	}

	protected static void updatePasswordSettings() {
		if (isPasswordSet()) {
			sychronizeDataEnabled.setChecked(true);
			Settings.updatePreferences();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Settings.updatePreferences();
		if (isPasswordSet() && isSychronizationEnabled()) {
			if (!registeredAlready()) {
				// attempt to register with the server
				Networking.sendToServer(ServerRequest.REGISTER, null, this);

			}
		}
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.settings;
	}

	private void initializeButtons() {
		boolean enableGPS = isGPSEnabled();
		boolean enableNotifications = areNotificationsEnabled();
		boolean enableDataSychronization = isSychronizationEnabled();
		notificationsEnabled.setChecked(enableNotifications);
		GPSEnabled.setChecked(enableGPS);
		GPSSensitivity.setValue(settings.getInt(Sensitivity, 20));
		GPSUpdateTime.setValue(settings.getInt(GPSTime, 1));
		GPSSensitivity.setEnabled(enableGPS);
		GPSUpdateTime.setEnabled(enableGPS);
		sychronizeDataEnabled.setChecked(enableDataSychronization);
	}

	private static void updatePreferences() {

		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putBoolean(isGPSEnabled, GPSEnabled.isChecked());
		prefEditor.putInt(GPSTime, GPSUpdateTime.getValue());
		prefEditor.putInt(Sensitivity, GPSSensitivity.getValue());
		prefEditor.putBoolean(areNotificationsEnabled,
				notificationsEnabled.isChecked());
		prefEditor.putBoolean(isSychronizationEnabled,
				sychronizeDataEnabled.isChecked());
		prefEditor.commit();
	}

	private void focusOnNothing() {
		GPSSensitivity.valueText.clearFocus();
		GPSUpdateTime.valueText.clearFocus();
		LinearLayout dummyLayout = (LinearLayout) findViewById(R.id.dummyLayout2);
		dummyLayout.requestFocus();

	}

	protected static boolean isGPSEnabled() {
		return settings.getBoolean(isGPSEnabled, false);

	}

	protected static int getGPSSensitivity() {
		return settings.getInt(Sensitivity, 0);
	}

	protected static int getGPSUpdateTime() {
		return settings.getInt(GPSTime, 1);
	}

	protected static boolean areNotificationsEnabled() {
		return settings.getBoolean(areNotificationsEnabled, true);
	}

	protected static boolean isPasswordSet() {
		return settings.getBoolean(isPasswordSet, false);
	}

	public static boolean isSychronizationEnabled() {
		return settings.getBoolean(isSychronizationEnabled, false);
	}

	public static String getPollTime() {
		return settings.getString(POLL_TIME, null);
	}

	protected static void setPassword(String passwd) {
		SharedPreferences.Editor prefEditor = settings.edit();
//		String test = Encryption.base64(Encryption.hash(passwd));
//		prefEditor.putString(password,
//				Encryption.base64(Encryption.hash(passwd)));
		prefEditor.putString(password, passwd);
		prefEditor.putBoolean(isPasswordSet, true);
		prefEditor.commit();

	}

	protected static void setPhoneNumber(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String telephoneNumber = telephonyManager.getLine1Number();
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putString(PhoneNumber, telephoneNumber);
		prefEditor.commit();
	}

	protected void setDeviceUUID() {
		UUID uuid = UUID.randomUUID();
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putString(UUIDOfDevice, uuid.toString());
		prefEditor.commit();
	}

	public static void setPollTime(String pollTime) {
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putString(POLL_TIME, pollTime);
		prefEditor.commit();
	}

	public static void confirmRegistrationWithWebServer() {
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putBoolean(Registered, true);
		prefEditor.commit();

	}

	public static String getPassword() {
		return settings.getString(password, "");
	}

	public static String getDeviceUUID() {
		return settings.getString(UUIDOfDevice, "");
	}

	public static String getPhoneNumber() {
		return settings.getString(PhoneNumber, null);
	}

	public static boolean registeredAlready() {
		return settings.getBoolean(Registered, false);
	}

}
