package edu.berkeley.security.eventtracker;

import edu.berkeley.security.eventtracker.webserver.EventDataServer;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerActivity extends EventActivity {
	public static final String PREFERENCE_FILENAME = "ServerPrefs";
	public static final String isServerRunning = "isServerRunning";
	private Button serverButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		serverButton = (Button) findViewById(R.id.serverButton);

		serverButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				boolean newServerStatus = !isServerRunning();
				updateServerStatus(newServerStatus);
				updateGUIStatus();

				// what if not able to connect to internet??
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateGUIStatus();
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.server;
	}

	/**
	 * Returns whether or not the web server is currently running
	 */
	public static boolean isServerRunning() {
		return serverSettings.getBoolean(isServerRunning, false);

	}

	/**
	 * Called whenever a change to the status of the web server has been made by
	 * clicking on the serverButton. Updates preferences and starts/stops the
	 * service that begins the web server.
	 * 
	 * @param serverRunning
	 *            whether or not the server is currently running.
	 */
	private void updateServerStatus(boolean serverRunning) {
		SharedPreferences.Editor prefEditor = serverSettings.edit();
		prefEditor.putBoolean(isServerRunning, serverRunning);
		prefEditor.commit();
		if (serverRunning) {
			startService(serverServiceIntent);
		} else {
			stopService(serverServiceIntent);
		}
	}

	/**
	 * @return true if the phone has either wifi or 3G access.
	 */
	private boolean canStartServer() {

		final ConnectivityManager connMgr = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		final android.net.NetworkInfo wifi =

		connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		final android.net.NetworkInfo mobile = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		return wifi.isAvailable() || mobile.isAvailable();
	}

	/**
	 * Refreshes the view with the current status (affects enabled/disable
	 * server button, server active icon, and the server address text).
	 */
	private void updateGUIStatus() {
		boolean isServerRunning = isServerRunning();

		// enable or disable the server button based on internet connectivity
		serverButton.setText(isServerRunning ? R.string.stopEventServer
				: R.string.startEventServer);
		serverButton.setEnabled(canStartServer());

		// set the network icon to dim or light up
		((ImageView) findViewById(R.id.toolbar_right_option))
				.setImageResource(isServerRunning ? R.drawable.server_on_64
						: R.drawable.server_off_64);

		// set the address text
		String serverText;
		if (isServerRunning) {
			serverText = String.format("%s\n\nhttp://%s:%d",
					getString(R.string.webserverAddressIntro),
					getInetAddress(), EventDataServer.PORT);
		} else {
			serverText = "";
		}
		((TextView) findViewById(R.id.serverText)).setText(serverText);
	}

	/**
	 * Uses the WifiManager to retrieve a String representation of the device's
	 * IP address.
	 * 
	 * @return the String IPv4 address of the device.
	 */
	private String getInetAddress() {
		WifiManager wifi_manager = (WifiManager) this
				.getSystemService(Context.WIFI_SERVICE);
		int wifi_address = wifi_manager.getConnectionInfo().getIpAddress();
		return String.format("%d.%d.%d.%d", (wifi_address & 0xff),
				(wifi_address >> 8 & 0xff), (wifi_address >> 16 & 0xff),
				(wifi_address >> 24 & 0xff));
	}
}
