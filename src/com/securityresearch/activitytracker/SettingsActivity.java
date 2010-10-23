package com.securityresearch.activitytracker;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ViewStub v =(ViewStub) findViewById(R.id.content_view);
        v.setLayoutResource(R.layout.settings);
        v.inflate();
        v.
        
        findViewById(R.id.toolbar_right_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
		});
        
        findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(SettingsActivity.this, ActivityEdit.class);
				startActivity(settingsIntent);
			}
		});
        DateFormat dateFormat  = android.text.format.DateFormat.getDateFormat(this);
        ((TextView) findViewById(R.id.toolbar_date)).setText(dateFormat.format(new Date()));
	}

}
