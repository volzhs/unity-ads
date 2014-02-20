package com.unity3d.ads.android.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsUtils;

public class UnityAdsFullscreenActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onCreate()", this);
    	
    	if (UnityAds.instance != null)
    		UnityAds.instance.changeActivity(this);
    	else
        	UnityAdsUtils.Log("onCreate() Unity Ads instance is NULL!", this);
    }
    
    @Override
    public void onResume () {
    	super.onResume();
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onResume()", this);
    	
    	if (UnityAds.instance != null)
    		UnityAds.instance.changeActivity(this);
    	else
        	UnityAdsUtils.Log("onResume() Unity Ads instance is NULL!", this);
    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();		
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onDestroy()", this);
	}
    
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	return false;
    }
}
