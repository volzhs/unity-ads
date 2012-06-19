package com.mycompany.test;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignListener;

import com.mycompany.test.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class UnityAdsTestStartActivity extends Activity implements IUnityAdsCampaignListener {
	private UnityAds ai = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((ImageView)findViewById(R.id.playbtn)).setAlpha(80);
		Log.d(UnityAdsProperties.LOG_NAME, "Init Unity Ads");
		ai = new UnityAds(this, "892347239");
		ai.setCampaignListener(this);
		ai.init();
    }
    
    @Override
    public void onResume () {
    	super.onResume();
		UnityAds.instance.changeActivity(this);
    }
    
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.kill:
				finish();
				break;
		}
		
		return true;
	}
	
    @Override
	protected void onDestroy() {
    	ai.stopAll();
    	System.runFinalizersOnExit(true);		
    	super.onDestroy();		
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
    @Override
	public void onFetchCompleted () {
    	((ImageView)findViewById(R.id.playbtn)).setAlpha(255);
    	((ImageView)findViewById(R.id.playbtn)).setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent newIntent = new Intent(getBaseContext(), UnityAdsGameActivity.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(newIntent);
			}
		});  
	}
}