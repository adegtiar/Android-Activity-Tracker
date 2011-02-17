package edu.berkeley.security.eventtracker;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

public class MyOnItemSelectedListener implements OnItemSelectedListener {


	private boolean ignoreFirst;
	public MyOnItemSelectedListener() {
		ignoreFirst=true;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if(ignoreFirst){
			ignoreFirst=false;
			return;
		}
		String tagChosen=parent.getItemAtPosition(position).toString();
		EventActivity.mEventManager.getCurrentEvent().mTag=tagChosen;
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
	
}
