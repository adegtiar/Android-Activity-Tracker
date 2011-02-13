package edu.berkeley.security.eventtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.security.eventtracker.webserver.EventDataServer;

public class ServerActivity extends EventActivity {
	public static final String PREFERENCE_FILENAME = "ServerPrefs";
	public static final String isServerRunning = "isServerRunning";
	public static final String ipAddress = "ipAddress";
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

		// set the web server address text
		((TextView) findViewById(R.id.remoteServerIntro))
				.setVisibility(isServerRunning ? View.VISIBLE : View.INVISIBLE);
		((TextView) findViewById(R.id.remoteServerText))
				.setVisibility(isServerRunning ? View.VISIBLE : View.INVISIBLE);

		// set the device ip address text (used for development)
		((TextView) findViewById(R.id.localAddressIntro))
				.setVisibility(isServerRunning ? View.VISIBLE : View.INVISIBLE);
		String localServerText;
		if (isServerRunning) {
			localServerText = String.format("http://%s:%d", serverSettings
					.getString(ipAddress, "could not find ip address"),
					EventDataServer.PORT);
		} else {
			localServerText = "";
		}
		((TextView) findViewById(R.id.localAddressText))
				.setText(localServerText);
	}

	public static void updateIpAdress(String newIpAddress) {
		SharedPreferences.Editor prefEditor = serverSettings.edit();
		prefEditor.putString(ipAddress, newIpAddress);
		prefEditor.commit();

	}

	@Override
	protected void startTrackingActivity() {
		super.startTrackingActivity();
		overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (velocityX > 0) {// going to right screen
			startTrackingActivity();
			return true;
		}
		return false;
	}
}
