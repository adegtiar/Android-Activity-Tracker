package edu.berkeley.security.eventtracker;

import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

/**
 * Manages the settings/miscellaneous parts of the Event Tracker.
 * This class is responsible for both managing the settings activity of the app
 * as well as various pieces of information such as the password and UUIDOfDevice.
 */
public class Settings extends PreferenceActivity {

	public static final String PREFERENCE_FILENAME = "SettingPrefs";
	public static final String isGPSEnabled = "isGPSEnabled";
	public static final String areNotificationsEnabled = "notificationsEnabled";
	public static final String password = "password";
	public static final String isPasswordSet = "isPasswordEntered";
	public static final String PhoneNumber = "phoneNumber";
	public static final String UUIDOfDevice = "deviceUUID";
	public static final String POLL_TIME = "pollTime";
	public static final String Registered = "registered";
	public static final String WekaModel = "wekaModel";
	private static final String isSychronizationEnabled = "enableSychronization";
	private static final String RegisteredServerOnly = "RegisteredServerOnly";
	private static final int DIALOG_ENTER_PASSWORD = 0;
	private static final int DIALOG_ACCOUNT_FOUND = 1;
	private static final int DIALOG_DELETE_DATA = 2;
	private static final int DIALOG_SUCCESS_NOW_SYNCING = 3;
	private static final int DIALOG_SHOW_CREDENTIALS = 4;
	private static final int DIALOG_ENTER_CORRECT_PASSWORD = 5;
	private static final String LAST_POLL_TIME = "lastPollTime";

	private static CheckBoxPreference sychronizeDataEnabled;
	private static CheckBoxPreference gpsEnabled;
	private static CheckBoxPreference notificationsEnabled;

	// Accessed by the Synchronizer service
	public static ProgressDialog creatingAcctDialog;
	public static ProgressDialog verifyingPwdDialog;
	
	public static String possiblePassword;
	
	// This enum is used in order to communicate between the Synchronizer and this activity
	//   when the user has clicked "Enable Web view" and the device believes it is not registered
	// NOT_REGISTERED means that this device is not registered with the server
	// REGISTERED means the device is registered with the server, but the device itself
	//   does not know it
	// COULD_NOT_CONTACT means that the server was not reached.
	public enum ServerAccoutStatus { NOT_REGISTERED, REGISTERED, COULD_NOT_CONTACT}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		sychronizeDataEnabled = (CheckBoxPreference) findPreference("webPref");
		gpsEnabled = (CheckBoxPreference) findPreference("gpsPref");
		notificationsEnabled = (CheckBoxPreference) findPreference("notificationsPref");
		creatingAcctDialog = new ProgressDialog(Settings.this);
		verifyingPwdDialog = new ProgressDialog(Settings.this);
		// Sets the device uuid in case it hasn't been set before.
		// This device uuid is used by the web server in order to identify users.
		if (getDeviceUUID().length() == 0) {
			setDeviceUUID();
		}

		gpsEnabled
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {

						if (gpsEnabled.isChecked())
							EventActivity.gpsServiceIntent
									.putExtra("gps", true);
						else {
							EventActivity.gpsServiceIntent.putExtra("gps",
									false);
						}
						if (EventActivity.mEventManager.isTracking())
							startService(EventActivity.gpsServiceIntent);

						return true;
					}

				});
		notificationsEnabled
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						// TODO fix this
						updatePreferences();
						return true;
					}
				});

		sychronizeDataEnabled
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {

						if (!isPasswordSet()
								&& sychronizeDataEnabled.isChecked()) {
							sychronizeDataEnabled.setChecked(false);

							if (Settings.getPhoneNumber() == null){
								CharSequence text = "Could not determine phone number\nA phone number is needed in order to " +
										            "access data online";
								int duration = Toast.LENGTH_LONG;
								Toast toast = Toast.makeText(getApplicationContext(), text, duration);
								toast.show();
							} else {
								showCreatingAcctDialog();
							}
							
						

						}
						return true;

					}
				});
		
		
		creatingAcctDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				// If no account already exists on the web server, prompt the
				// user to create a new one
				if (Settings.accountRegisteredOnlyServer() == ServerAccoutStatus.NOT_REGISTERED) {
					showDialog(DIALOG_ENTER_PASSWORD);
				} else if (Settings.accountRegisteredOnlyServer() == ServerAccoutStatus.REGISTERED){
				// An account already exists. Ask user if they wish to link to it.
				    showDialog(DIALOG_ACCOUNT_FOUND);	
				} else {
					CharSequence text = "Could not contact server\nTry again later";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(getApplicationContext(), text, duration);
					toast.show();
				}
			}

		});
		verifyingPwdDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				// Device is registered which means that the correct password was entered.
				if (Settings.registeredAlready()) {
					// success!
					Settings.setPassword(possiblePassword);
					Settings.updatePasswordSettings();
					updatePreferences();
					// Get the already existing data from the web server
					Networking.pollServerIfAllowed(Settings.this);
					// Send all phone only events to the server
					Networking.sendAllEvents(Settings.this);
					//Display success dialog
					showDialog(DIALOG_SUCCESS_NOW_SYNCING);
				} else {
					showDialog(DIALOG_ENTER_CORRECT_PASSWORD);
					CharSequence text = "Wrong Password. Try again";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(getApplicationContext(), text, duration);
					toast.show();
				}
			}

		});


		Preference showCredentials = (Preference) findPreference("webCredentials");
		showCredentials
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					// User clicks on the Show Credentials option in settings
					public boolean onPreferenceClick(Preference preference) {
						showDialog(DIALOG_SHOW_CREDENTIALS);
						return true;
					}

				});
	}

	/**
	 * First Step in registering an account only. This method contacts the web server
	 *  in order to check to see if an accout with the same phone number is also in use. 
	 */
	private void showCreatingAcctDialog() {
		creatingAcctDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		creatingAcctDialog.setMessage("Creating an account...");
		creatingAcctDialog.setCancelable(false);
		creatingAcctDialog.show();
		Networking.checkIfAlreadyRegistered(getApplicationContext());

	}
	/**
	 * In order to link an account with an already existing account,
	 * the user must verify their password. They must enter in the same 
	 * password used with the previous account. This method brings up the dialog
	 * box that does that.
	 */
	private void showVerifyingPwdDialog() {
		
		verifyingPwdDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		verifyingPwdDialog.setMessage("Verifying password...");
		verifyingPwdDialog.setCancelable(true);
		verifyingPwdDialog.show();
		Networking.verifyPassword(getApplicationContext());

	}
	/**
	 * Copies the settings preferences over so that the EventActivity/other activies can access them
	 */
	private void updatePreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putBoolean(isGPSEnabled, prefs.getBoolean("gpsPref", false));
		prefEditor.putBoolean(areNotificationsEnabled, prefs.getBoolean(
				"notificationsPref", false));
		prefEditor.putBoolean(isSychronizationEnabled, prefs.getBoolean(
				"webPref", false));
		prefEditor.commit();
	}

	/*
	 * Updates password and registers with the server
	 */
	protected static void updatePasswordSettings() {
		if (isPasswordSet()) {
			sychronizeDataEnabled.setChecked(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		updatePreferences();
		if (!areNotificationsEnabled()) {
			EventActivity.disableTrackingNotification(this);
		}
		if (areNotificationsEnabled()
				&& EventActivity.mEventManager.isTracking()) {
			EventActivity.enableTrackingNotification(this,
					EventActivity.mEventManager.getCurrentEvent());
		}
	}

	/**	
	 *  onCreateDialog is responsible for the logic involved with displaying dialog boxes.
	 *  These dialog boxes are used in order to register a user with the web server 
	 */
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		LayoutInflater te_factory;
		final View textEntryView;
		String username, password, url, message;
		switch (id) {
		case DIALOG_ENTER_PASSWORD:
			te_factory = LayoutInflater.from(this);
			textEntryView = te_factory.inflate(
					R.layout.alert_dialog_text_entry, null);

			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(
					R.string.alert_dialog_text_entry).setView(textEntryView)
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
									updatePreferences();
									// Register with server now
								    Networking.sendToServer(ServerRequest.REGISTER, null, Settings.this);
									showDialog(DIALOG_SUCCESS_NOW_SYNCING);
								}
							}).setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
			
		case DIALOG_ACCOUNT_FOUND:
			username = Settings.getPhoneNumber();
			message = "Account with phone number " + username + " found. Do you want to link?";
			
			return new AlertDialog.Builder(this).setMessage(message)
					.setPositiveButton(R.string.dialog_yes,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									showDialog(DIALOG_ENTER_CORRECT_PASSWORD);
								
								}
							}).setNegativeButton(R.string.dialog_no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									showDialog(DIALOG_DELETE_DATA);
								}
							}).create();
		case DIALOG_DELETE_DATA:
			
			message = "Are you sure? This will delete all data associated with this account.";
			
			return new AlertDialog.Builder(this).setMessage(message)
					.setPositiveButton(R.string.dialog_yes,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									showDialog(DIALOG_ENTER_PASSWORD);
								
								}
							}).setNegativeButton(R.string.dialog_no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									showDialog(DIALOG_ACCOUNT_FOUND);
								}
							}).create();
			
		case DIALOG_SHOW_CREDENTIALS:
			url = "Visit eventtracker.heroku.com with username ";
			username = Settings.getPhoneNumber();
			password = Settings.getPassword();
			message =  url + username + " and password " + password;
			
			return new AlertDialog.Builder(this).setTitle("View Your Data Online").setMessage(message)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
								
								}
							}).create();
		case DIALOG_SUCCESS_NOW_SYNCING:
			String success = "Success! Now syncing data in background.\n";
			url = "Visit eventtracker.heroku.com with username ";
			username = Settings.getPhoneNumber();
			password = Settings.getPassword();
			message = success + url + username + " and password " + password;

			return new AlertDialog.Builder(this).setTitle("View Your Data Online").setMessage(message)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
								
								}
							}).create();
		case DIALOG_ENTER_CORRECT_PASSWORD:
			te_factory = LayoutInflater.from(this);
			textEntryView = te_factory.inflate(
					R.layout.dialog_enter_correct_password, null);

			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(
					R.string.enter_same_password).setView(textEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									// User entered a password and clicked OK
									
									EditText passwdEditText = (EditText) textEntryView
											.findViewById(R.id.password_edit);
									String password = passwdEditText.getText()
											.toString();
									possiblePassword = password;
									showVerifyingPwdDialog();
							
								}
							}).setNegativeButton(R.string.alert_dialog_cancel,
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

	public static Long getLastPolledTime() {
		return EventActivity.settings.getLong(LAST_POLL_TIME, 0);
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
	/**
	 * Returns true if according to the device, the phone is unregistered. But 
	 *   according to the web server, it is registered. False otherwise
	 * @return 
	 */
	public static ServerAccoutStatus accountRegisteredOnlyServer() {
		int position = EventActivity.settings.getInt(RegisteredServerOnly, ServerAccoutStatus.NOT_REGISTERED.ordinal());
		return ServerAccoutStatus.values()[position];  
	}

	protected static void setPassword(String passwd) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
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
	public static void setDeviceUUID(String uuid) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putString(UUIDOfDevice, uuid);
		prefEditor.commit();
	}

	public static void setLastPolledTime(Long pollTime) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putLong(LAST_POLL_TIME, pollTime);
		prefEditor.commit();
	}

	public static void setPollTime(String pollTime) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putString(POLL_TIME, pollTime);
		prefEditor.commit();
	}

	/*
	 * This method is called by two ways: 1) A new user clicked enable web view
	 * and created a new account 2) User clicked enable web view, the device
	 * thought the user was unregistered, but the web server claims that an
	 * account already exists.
	 */
	public static void confirmRegistrationWithWebServer() {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putBoolean(Registered, true);
		prefEditor.commit();

	}
	public static void setAccountRegisteredOnlyServer(ServerAccoutStatus serverOnly) {
		SharedPreferences.Editor prefEditor = EventActivity.settings.edit();
		prefEditor.putInt(RegisteredServerOnly, serverOnly.ordinal());
		prefEditor.commit();
	}

}
