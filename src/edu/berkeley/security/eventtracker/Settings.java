package edu.berkeley.security.eventtracker;

import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends PreferenceActivity {

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
	public static final String Registered = "registered";
	private static final String isSychronizationEnabled = "enableSychronization";
	private static final int DIALOG_TEXT_ENTRY = 0;

	private static CheckBoxPreference sychronizeDataEnabled;
	private static CheckBoxPreference gpsEnabled;
	private static CheckBoxPreference notificationsEnabled;

	//don't access these directly
	private static boolean gpsPreference;
	private static boolean notificationPreferences;
	private static boolean webPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			sychronizeDataEnabled = (CheckBoxPreference) findPreference("webPref");
			gpsEnabled = (CheckBoxPreference) findPreference("gpsPref");
			notificationsEnabled = (CheckBoxPreference) findPreference("notificationsPref");
			getPrefs();
			if (getDeviceUUID().length() == 0) {
				setDeviceUUID();
			}
			
			
			  gpsEnabled.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				  
                  public boolean onPreferenceClick(Preference preference) {
                  	
                  	
					if(gpsEnabled.isChecked())
						EventActivity.gpsServiceIntent.putExtra("gps", true);
					else {
						EventActivity.gpsServiceIntent.putExtra("gps", false);
					}
					if (EventActivity.mEventManager.isTracking())
						startService(EventActivity.gpsServiceIntent);

					
                    return true;
                  }

          });
				notificationsEnabled.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
	                  	//TODO fix this
						SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
						 boolean test=prefs.getBoolean("notificationsPref", false);
						 updatePreferences();
						return true;
					}
				});	
				
				sychronizeDataEnabled.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {

						if (!isPasswordSet() && sychronizeDataEnabled.isChecked()) {
							sychronizeDataEnabled.setChecked(false);
						
							showDialog(DIALOG_TEXT_ENTRY);
						}
						return true;

					}
				});		
	
	}

	private void getPrefs() {
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		gpsPreference = prefs.getBoolean("gpsPref", false);
		notificationPreferences = prefs.getBoolean("notificationsPref", false);
		webPreferences = prefs.getBoolean("webPref", false);

	}

	private void updatePreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putBoolean(isGPSEnabled, prefs.getBoolean("gpsPref", false));
		prefEditor.putBoolean(areNotificationsEnabled, prefs.getBoolean("notificationsPref", false));
		prefEditor.putBoolean(isSychronizationEnabled, prefs.getBoolean("webPref", false));
		prefEditor.commit();
	}

	protected static void updatePasswordSettings() {
		if (isPasswordSet()) {
			sychronizeDataEnabled.setChecked(true);

		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		updatePreferences();
		boolean isPass=isPasswordSet();
		if (isPasswordSet() && isSychronizationEnabled()) {
			if (!registeredAlready()) {
				// attempt to register with the server
				Networking.sendToServer(ServerRequest.REGISTER, null, this);

			}
		}
		if(!areNotificationsEnabled()){
			EventActivity.disableTrackingNotification(this);
		}
		if(areNotificationsEnabled() && EventActivity.mEventManager.isTracking()){
			EventActivity.enableTrackingNotification(this, EventActivity.mEventManager.getCurrentEvent());
		}
	}

	protected static boolean isGPSEnabled() {
		return EventActivity.settings.getBoolean(isGPSEnabled, false);
	}

	protected static boolean areNotificationsEnabled() {
		return EventActivity.settings
				.getBoolean(areNotificationsEnabled, false);
	}

	protected static boolean isPasswordSet() {
		return EventActivity.settings.getBoolean(isPasswordSet, false);
	}

	public static boolean isSychronizationEnabled() {
		return EventActivity.settings
				.getBoolean(isSychronizationEnabled, false);
	}

	public static String getPollTime() {
		return EventActivity.settings.getString(POLL_TIME, null);
	}

	protected static void setPassword(String passwd) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		// String test = Encryption.base64(Encryption.hash(passwd));
		// prefEditor.putString(password,
		// Encryption.base64(Encryption.hash(passwd)));
		prefEditor.putString(password, passwd);
		prefEditor.putBoolean(isPasswordSet, true);
		prefEditor.commit();

	}

	protected static void setPhoneNumber(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String telephoneNumber = telephonyManager.getLine1Number();
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putString(PhoneNumber, telephoneNumber);
		prefEditor.commit();
	}

	protected void setDeviceUUID() {
		UUID uuid = UUID.randomUUID();
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putString(UUIDOfDevice, uuid.toString());
		prefEditor.commit();
	}

	public static void setPollTime(String pollTime) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putString(POLL_TIME, pollTime);
		prefEditor.commit();
	}

	public static void confirmRegistrationWithWebServer() {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putBoolean(Registered, true);
		prefEditor.commit();

	}

	public static String getPassword() {
		return EventActivity.settings.getString(password, "");
	}

	public static String getDeviceUUID() {
		return EventActivity.settings.getString(UUIDOfDevice, "");
	}

	public static String getPhoneNumber() {
		return EventActivity.settings.getString(PhoneNumber, null);
	}

	public static boolean registeredAlready() {
		return EventActivity.settings.getBoolean(Registered, false);
	}
	/*
	 * Dialog box for password entry
	 */
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {

		case DIALOG_TEXT_ENTRY:
			// This example shows how to add a custom layout to an AlertDialog
			LayoutInflater te_factory = LayoutInflater.from(this);
			final View textEntryView = te_factory.inflate(
					R.layout.alert_dialog_text_entry, null);

			return new AlertDialog.Builder(this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.alert_dialog_text_entry)
					.setView(textEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User entered a password and clicked OK */
									EditText passwdEditText = (EditText) textEntryView
											.findViewById(R.id.password_edit);
									String password = passwdEditText.getText()
											.toString();
									Settings.setPassword(password);
									
									Settings.updatePasswordSettings();
									

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		default:
			return super.onCreateDialog(id, bundle);
		}
	}

}
