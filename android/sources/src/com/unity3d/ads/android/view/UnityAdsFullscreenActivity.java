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
	public void onStart() {
		UnityAdsDeviceLog.entered();
		super.onStart();
	}

	@Override
	public void onRestart() {
		UnityAdsDeviceLog.entered();
		super.onRestart();
	}

	@Override
	public void onResume() {
		UnityAdsDeviceLog.entered();   	
		super.onResume();
		UnityAds.changeActivity(this);
   		UnityAds.checkMainview();
	}

	@Override
	public void onPause() {
		UnityAdsDeviceLog.entered();
		super.onPause();
	}

	@Override
	public void onStop() {
		UnityAdsDeviceLog.entered();
		super.onStop();
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
