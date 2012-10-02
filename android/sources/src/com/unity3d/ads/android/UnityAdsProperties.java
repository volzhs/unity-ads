package com.unity3d.ads.android;

import android.app.Activity;

public class UnityAdsProperties {	
	public static final String LOG_NAME = "UnityAds";
	public static final String CACHE_DIR_NAME = "UnityAdsVideoCache";
	public static final String CACHE_MANIFEST_FILENAME = "manifest.json";
	public static final String PENDING_REQUESTS_FILENAME = "pendingrequests.dat";
	//public static final String WEBDATA_URL = "http://ads-dev.local/manifest.json";	
	public static final String WEBDATA_URL = "https://impact.applifier.com/mobile/campaigns";
	
	public static Activity CURRENT_ACTIVITY = null;
	public static String UNITY_ADS_APP_ID = "";
}
