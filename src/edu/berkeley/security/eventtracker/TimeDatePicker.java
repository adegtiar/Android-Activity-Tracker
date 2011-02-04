package edu.berkeley.security.eventtracker;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class TimeDatePicker extends Activity {
	private TimePicker time;
	private DatePicker date;
	private Calendar cal;

	@Override
	protected void onResume() {
		super.onResume();
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timedatepicker);
		cal = Calendar.getInstance();

		Long oldTime = this.getIntent().getLongExtra("Time", 0);
		if (oldTime != Long.MAX_VALUE) {

			cal.setTimeInMillis(oldTime);
		}

		time = ((TimePicker) findViewById(R.id.timePicker));
		time.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		time.setCurrentMinute(cal.get(Calendar.MINUTE));

		date = ((DatePicker) findViewById(R.id.datePicker));

		date.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal
				.get(Calendar.DAY_OF_MONTH));

		findViewById(R.id.setTimeDate).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {

						int hour = time.getCurrentHour();
						int minute = time.getCurrentMinute();

						int day = date.getDayOfMonth();
						int year = date.getYear();
						int month = date.getMonth();

						cal.set(year, month, day, hour, minute);
						Intent mIntent = new Intent();
						Bundle bundle = new Bundle();
						bundle.putLong("Time", cal.getTimeInMillis());
						mIntent.putExtras(bundle);
						setResult(RESULT_OK, mIntent);
						finish();

					}
				});

	}
}