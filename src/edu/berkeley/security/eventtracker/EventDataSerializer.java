package edu.berkeley.security.eventtracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class EventDataSerializer extends Activity {

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finish();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#finish()
	 */
	@Override
	public void finish() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("mSerializedData", "This should be serialized data.");
		resultIntent.putExtra("maybeLaunchedExternally", true);
		setResult(RESULT_OK, resultIntent);
		super.finish();
	}
}
