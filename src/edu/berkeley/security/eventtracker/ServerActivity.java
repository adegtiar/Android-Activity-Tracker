package edu.berkeley.security.eventtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

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
				serverButton
						.setText(newServerStatus ? R.string.stopEventServer
								: R.string.startEventServer);
				((ImageView) findViewById(R.id.toolbar_right_option))
						.setImageResource(newServerStatus ? R.drawable.server_on_64
								: R.drawable.server_off_64);
				// what if not able to connect to internet??
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateButtonStatus();
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
	 * service that begins the web server
	 * 
	 * @param serverRunning
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
	 * Returns true if the phone has either wifi or 3G access
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

	private void updateButtonStatus() {
		boolean isServerRunning = isServerRunning();
		serverButton.setText(isServerRunning ? R.string.stopEventServer
				: R.string.startEventServer);
		serverButton.setEnabled(canStartServer());
	}

}
