package com.unity3d.ads.android.properties;

import java.net.URLEncoder;

import com.unity3d.ads.android.data.UnityAdsDevice;

import android.app.Activity;
import android.util.Log;

public class UnityAdsProperties {
	public static String CAMPAIGN_DATA_URL = "http://192.168.1.152:3500/mobile/campaigns";
	public static String WEBVIEW_BASE_URL = null;
	public static String ANALYTICS_BASE_URL = null;
	public static String UNITY_ADS_BASE_URL = null;
	public static String CAMPAIGN_QUERY_STRING = null;
	public static String UNITY_ADS_GAME_ID = null;
	public static String UNITY_ADS_GAMER_ID = null;
	public static Boolean TESTMODE_ENABLED = false;
	public static Activity CURRENT_ACTIVITY = null;
	public static final int MAX_NUMBER_OF_ANALYTICS_RETRIES = 5;
	
	private static String _campaignQueryString = null; 
	
	private static void createCampaignQueryString () {
		String queryString = "?";
		
		TESTMODE_ENABLED = true;
		
		//Mandatory params
		try {
			queryString = String.format("%s%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICEID_KEY, URLEncoder.encode(UnityAdsDevice.getDeviceId(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_PLATFORM_KEY, "android");
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_GAMEID_KEY, URLEncoder.encode(UnityAdsProperties.UNITY_ADS_GAME_ID, "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_OPENUDID_KEY, URLEncoder.encode(UnityAdsDevice.getOpenUdid(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_MACADDRESS_KEY, URLEncoder.encode(UnityAdsDevice.getMacAddress(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SDKVERSION_KEY, URLEncoder.encode(UnityAdsConstants.UNITY_ADS_VERSION, "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SOFTWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getSoftwareVersion(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_HARDWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getHardwareVersion(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICETYPE_KEY, URLEncoder.encode(UnityAdsDevice.getDeviceType(), "UTF-8"));
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_CONNECTIONTYPE_KEY, URLEncoder.encode(UnityAdsDevice.getConnectionType(), "UTF-8"));
		}
		catch (Exception e) {
			Log.d(UnityAdsConstants.LOG_NAME, "Problems creating campaigns query");
		}
		
		if (TESTMODE_ENABLED) {
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TEST_KEY, "true");
		}
		
		_campaignQueryString = queryString;
	}
	
	public static String getCampaignQueryUrl () {
		if (_campaignQueryString == null) {
			createCampaignQueryString();
		}
		
		return String.format("%s%s", UnityAdsProperties.CAMPAIGN_DATA_URL, _campaignQueryString);
	}
}
