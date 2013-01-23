package com.unity3d.ads.android.view;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.properties.UnityAdsConstants;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class UnityAdsFullscreenActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsFullscreenActivity->onCreate()");
        super.onCreate(savedInstanceState);
		UnityAds.instance.changeActivity(this);
    }
    
    @Override
    public void onResume () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsFullscreenActivity->onResume()");
    	super.onResume();
    }
    
    @Override
	protected void onDestroy() {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsFullscreenActivity->onDestroy()");
    	super.onDestroy();		
	}
}
