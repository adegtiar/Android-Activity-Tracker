package com.securityresearch.activitytracker;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

public class ActivityEdit extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ViewStub v =(ViewStub) findViewById(R.id.content_view);
        v.setLayoutResource(R.layout.activity_edit);
        v.inflate();
        
        findViewById(R.id.toolbar_right_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(ActivityEdit.this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
		});
        
        findViewById(R.id.toolbar_left_option).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(ActivityEdit.this, ActivityList.class);
				startActivity(settingsIntent);
			}
		});
        DateFormat dateFormat  = android.text.format.DateFormat.getDateFormat(this);
        ((TextView) findViewById(R.id.toolbar_date)).setText(dateFormat.format(new Date()));
    }
}