package com.securityresearch.activitytracker;

import java.text.DateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ActivityList extends ListActivity {
	
//	private NotesDbAdapter mDbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        findViewById(R.id.toolbar_right_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(ActivityList.this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
		});
        
        findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(ActivityList.this, ActivityEdit.class);
				startActivity(settingsIntent);
			}
		});
        DateFormat dateFormat  = android.text.format.DateFormat.getDateFormat(this);
        ((TextView) findViewById(R.id.toolbar_date)).setText(dateFormat.format(new Date()));
	}

}
