
package com.mycompany.test;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;

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

public class UnityAdsTestStartActivity extends Activity implements IUnityAdsListener {
	private UnityAds ai = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((ImageView)findViewById(R.id.playbtn)).setAlpha(80);
		Log.d(UnityAdsConstants.LOG_NAME, "Init Unity Ads");
		UnityAds.setDebugMode(true);
		UnityAds.setTestMode(true);
		
		ai = new UnityAds(this, "16", this);
    }
    
    @Override
    public void onResume () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onResume()");
    	super.onResume();
		UnityAds.instance.changeActivity(this);
		UnityAds.instance.setListener(this);
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
		    	ai.stopAll();
		    	System.runFinalizersOnExit(true);		
				finish();
		    	Log.d(UnityAdsConstants.LOG_NAME, "Quitting");

		    	break;
		}
		
		return true;
	}
	
    @Override
	protected void onDestroy() {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onDestroy()");
    	super.onDestroy();		
	}
	
    @Override
	public void onHide () {
    	
    }
    
    @Override
	public void onShow () {
    	
    }
	
	// Unity Ads video events
    @Override
	public void onVideoStarted () {
    	
    }
    
    @Override
	public void onVideoCompleted (String rewardItemKey) {
    }
	
	// Unity Ads campaign events
    @Override
	public void onFetchCompleted () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onFetchCompleted()");
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
    
    @Override
    public void onFetchFailed () {
    }
}