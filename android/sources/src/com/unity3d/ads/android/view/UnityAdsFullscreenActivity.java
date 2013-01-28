package com.unity3d.ads.android.view;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsUtils;

import android.app.Activity;
import android.os.Bundle;

public class UnityAdsFullscreenActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onCreate()", this);
        super.onCreate(savedInstanceState);
		UnityAds.instance.changeActivity(this);
    }
    
    @Override
    public void onResume () {
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onResume()", this);
    	super.onResume();
    }
    
    @Override
	protected void onDestroy() {
    	UnityAdsUtils.Log("UnityAdsFullscreenActivity->onDestroy()", this);
    	super.onDestroy();		
	}
}
