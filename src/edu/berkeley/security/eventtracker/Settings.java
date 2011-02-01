package edu.berkeley.security.eventtracker;

import java.util.UUID;

import android.app.SearchManager.OnDismissListener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.berkeley.security.eventtracker.eventdata.EventEntry.ColumnType;
import edu.berkeley.security.eventtracker.network.Encryption;
import edu.berkeley.security.eventtracker.network.Networking;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 */
public class Settings extends EventActivity  {

	public static final String PREFERENCE_FILENAME = "SettingPrefs";
	public static final String GPSTime = "GPSTime";
	public static final String Sensitivity = "Sensitivity";
	public static final String isGPSEnabled = "isGPSEnabled";
	public static final String areNotificationsEnabled = "notificationsEnabled";
	public static final String password = "password";
	public static final String isPasswordSet = "isPasswordEntered";
	public static final String PhoneNumber = "phoneNumber";
	public static final String UUIDOfDevice = "deviceUUID";
	private static final String UUIDMostSigBits = "UUIDMostSigBits";
	private static final String UUIDLeastSigBits = "UUIDLeastSigBits";
	private static final String isSychronizationEnabled="enableSychronization";

	
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
		setPhoneNumber();
		setDeviceUUID();
		initializeButtons();

		
		GPSEnabled.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				CheckBox gps = (CheckBox) view;
				if (gps.isChecked())
					gpsServiceIntent.putExtra("gps", true);
				else{
					gpsServiceIntent.putExtra("gps", false);
				}
				if( mEventManager.isTracking())
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
						Settings.this.updatePreferences();
						Settings.this.updateTrackingStatus();
					}
				});
		sychronizeDataEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
		
				
				
				if(!isPasswordSet()){
					sychronizeDataEnabled.setChecked(false);
					//TODO make user set a password
					Bundle bundle = new Bundle();
					bundle.putBoolean("Settings", true);
					showDialog(DIALOG_TEXT_ENTRY, bundle);

				}
				
				
			}
		});
	}


	public static void updatePasswordSettings(){
    	if(isPasswordSet()){
			sychronizeDataEnabled.setChecked(true);
			Settings.updatePreferences();
			Networking.sendRegistration();
			
		}	
	}


	@Override
	protected void onPause() {
		Settings.updatePreferences();
		super.onPause();
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.settings;
	}

	private void initializeButtons() {
		boolean enableGPS = isGPSEnabled();
		boolean enableNotifications=areNotificationsEnabled();
		boolean enableDataSychronization=isSychronizationEnabled();
		notificationsEnabled.setChecked(enableNotifications);
		GPSEnabled.setChecked(enableGPS);
		GPSSensitivity.setValue(settings.getInt(Sensitivity, 0));
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
		prefEditor.putBoolean(areNotificationsEnabled, notificationsEnabled
				.isChecked());
		prefEditor.putBoolean(isSychronizationEnabled, sychronizeDataEnabled.isChecked());
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
	
	public static boolean isPasswordSet(){
		return settings.getBoolean(isPasswordSet, false);
		
	}
	public static boolean isSychronizationEnabled(){
		return settings.getBoolean(isSychronizationEnabled, false);
		
	}
	
	public static void setPassword(String passwd){
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putString(password, Encryption.base64(Encryption.hash(passwd)));
		prefEditor.putBoolean(isPasswordSet, true);
		prefEditor.commit();
		
	}
	
	public void setPhoneNumber(){
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String telephoneNumber=telephonyManager.getLine1Number();
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putString(PhoneNumber, telephoneNumber);
		prefEditor.commit();
	}
	public void setDeviceUUID(){
		UUID uuid=UUID.randomUUID();
		SharedPreferences.Editor prefEditor = settings.edit();
		prefEditor.putLong(UUIDMostSigBits, uuid.getMostSignificantBits());
		prefEditor.putLong(UUIDLeastSigBits, uuid.getLeastSignificantBits());
		prefEditor.commit();
	}
	public static String getDeviceUUID(){
		
		long mostSigBits=settings.getLong(UUIDMostSigBits, 0);
		long leastSigBits=settings.getLong(UUIDLeastSigBits, 0);
		return new UUID(mostSigBits, leastSigBits).toString();
	}
	public static String getPhoneNumber(){
		return settings.getString(PhoneNumber, null);
	}
	


}
