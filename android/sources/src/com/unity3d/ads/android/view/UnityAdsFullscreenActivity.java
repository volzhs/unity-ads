package com.unity3d.ads.android.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;

public class UnityAdsFullscreenActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
		UnityAdsDeviceLog.entered();   	
        super.onCreate(savedInstanceState);
   		UnityAds.changeActivity(this);
    }
    
    @Override
    public void onResume () {
		UnityAdsDeviceLog.entered();   	
    	super.onResume();
   		UnityAds.changeActivity(this);
   		UnityAds.checkMainview();
    }
    
    @Override
	protected void onDestroy() {
		UnityAdsDeviceLog.entered();   	
    	super.onDestroy();
    	UnityAds.handleFullscreenDestroy();
	}
    
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	return false;
    }
}
