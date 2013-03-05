package com.unity3d.ads.android.properties;

import java.net.URLEncoder;
import java.util.Map;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.data.UnityAdsDevice;

import android.app.Activity;

public class UnityAdsProperties {
	public static String CAMPAIGN_DATA_URL = "https://impact.applifier.com/mobile/campaigns";
	public static String WEBVIEW_BASE_URL = null;
	public static String ANALYTICS_BASE_URL = null;
	public static String UNITY_ADS_BASE_URL = null;
	public static String CAMPAIGN_QUERY_STRING = null;
	public static String UNITY_ADS_GAME_ID = null;
	public static String UNITY_ADS_GAMER_ID = null;
	public static String GAMER_SID = null;
	public static Boolean TESTMODE_ENABLED = false;
	public static Activity BASE_ACTIVITY = null;
	public static Activity CURRENT_ACTIVITY = null;
	public static UnityAdsCampaign SELECTED_CAMPAIGN = null;
	public static Boolean UNITY_ADS_DEBUG_MODE = false;
	
	public static String TEST_DATA = null;
	public static String TEST_URL = null;
	public static String TEST_JAVASCRIPT = null;
	public static Boolean RUN_WEBVIEW_TESTS = false;
	private static Map<String, String> TEST_EXTRA_PARAMS = null; 

	public static final int MAX_NUMBER_OF_ANALYTICS_RETRIES = 5;
	
	private static String _campaignQueryString = null; 
	
	private static void createCampaignQueryString () {
		String queryString = "?";
		
		//Mandatory params
		try {
			queryString = String.format("%s%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICEID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(), "UTF-8"));
			
			if (!UnityAdsDevice.getAndroidId().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ANDROIDID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(), "UTF-8"));
			
			if (!UnityAdsDevice.getTelephonyId().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TELEPHONYID_KEY, URLEncoder.encode(UnityAdsDevice.getTelephonyId(), "UTF-8"));
			
			if (!UnityAdsDevice.getAndroidSerial().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SERIALID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidSerial(), "UTF-8"));

			if (!UnityAdsDevice.getOpenUdid().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_OPENUDID_KEY, URLEncoder.encode(UnityAdsDevice.getOpenUdid(), "UTF-8"));
			
			if (!UnityAdsDevice.getMacAddress().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_MACADDRESS_KEY, URLEncoder.encode(UnityAdsDevice.getMacAddress(), "UTF-8"));

			
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_PLATFORM_KEY, "android");
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_GAMEID_KEY, URLEncoder.encode(UnityAdsProperties.UNITY_ADS_GAME_ID, "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SDKVERSION_KEY, URLEncoder.encode(UnityAdsConstants.UNITY_ADS_VERSION, "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SOFTWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getSoftwareVersion(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_HARDWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getHardwareVersion(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICETYPE_KEY, UnityAdsDevice.getDeviceType());
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_CONNECTIONTYPE_KEY, URLEncoder.encode(UnityAdsDevice.getConnectionType(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENSIZE_KEY, UnityAdsDevice.getScreenSize());
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENDENSITY_KEY, UnityAdsDevice.getScreenDensity());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Problems creating campaigns query", UnityAdsProperties.class);
		}
		
		if (TESTMODE_ENABLED) {
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TEST_KEY, "true");
		}
		
		_campaignQueryString = queryString;
		
		/*
		PackageManager manager = UnityAdsProperties.CURRENT_ACTIVITY.getPackageManager();
		FeatureInfo[] features = manager.getSystemAvailableFeatures();
		for (FeatureInfo feature : features) {
			if (feature.name != null)
				UnityAdsUtils.Log("Feature:" + feature.name, UnityAdsProperties.class);
			else
				UnityAdsUtils.Log("Feature: OpenGLES " + feature.getGlEsVersion(), UnityAdsProperties.class);
		}
		*/
	}
	
	public static String getCampaignQueryUrl () {
		if (_campaignQueryString == null) {
			createCampaignQueryString();
		}
		
		String url = CAMPAIGN_DATA_URL;
		
		if (UnityAdsUtils.isDebuggable(BASE_ACTIVITY) && TEST_URL != null)
			url = TEST_URL;
			
		return String.format("%s%s", url, _campaignQueryString);
	}
	
	public static void setExtraParams (Map<String, String> params) {
		if (params.containsKey("testData")) {
			TEST_DATA = params.get("testData");
		}
		
		if (params.containsKey("testUrl")) {
			TEST_URL = params.get("testUrl");
		}
		
		if (params.containsKey("testJavaScript")) {
			TEST_JAVASCRIPT = params.get("testJavaScript");
		}
	}
}
