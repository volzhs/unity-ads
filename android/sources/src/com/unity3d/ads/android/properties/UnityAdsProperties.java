package com.unity3d.ads.android.properties;

import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
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
	public static Map<String, Object> UNITY_ADS_DEVELOPER_OPTIONS = null;
	public static int ALLOW_VIDEO_SKIP = 0;
	public static int ALLOW_BACK_BUTTON_SKIP = 0;
	
	public static String TEST_DATA = null;
	public static String TEST_URL = null;
	public static String TEST_JAVASCRIPT = null;
	public static Boolean RUN_WEBVIEW_TESTS = false;
	
	@SuppressWarnings("unused")
	private static Map<String, String> TEST_EXTRA_PARAMS = null; 

	public static final int MAX_NUMBER_OF_ANALYTICS_RETRIES = 5;
	public static final int MAX_BUFFERING_WAIT_SECONDS = 20;
	
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

			if (!UnityAdsDevice.getOdin1Id().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ODIN1ID_KEY, URLEncoder.encode(UnityAdsDevice.getOdin1Id(), "UTF-8"));
			
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
			UnityAdsUtils.Log("Problems creating campaigns query: " + e.getMessage() + e.getStackTrace().toString(), UnityAdsProperties.class);
		}
		
		if (TESTMODE_ENABLED) {
			queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TEST_KEY, "true");
		}
		else {
			if (UnityAdsProperties.CURRENT_ACTIVITY != null) {
				queryString = String.format("%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ENCRYPTED_KEY, UnityAdsUtils.isDebuggable(UnityAdsProperties.CURRENT_ACTIVITY) ? "false" : "true");
			}
		}
		
		_campaignQueryString = queryString;
	}
	
	public static JSONObject getDeveloperOptionsAsJson () {
		if (UNITY_ADS_DEVELOPER_OPTIONS != null) {
			JSONObject options = new JSONObject();
			
			boolean noOfferscreen = false;
			boolean openAnimated = false;
			boolean muteVideoSounds = false;
			
			try {
				if (UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY))
					noOfferscreen = (Boolean)UNITY_ADS_DEVELOPER_OPTIONS.get(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY);
				
				options.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, noOfferscreen);
				
				if (UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY))
					openAnimated = (Boolean)UNITY_ADS_DEVELOPER_OPTIONS.get(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY);
				
				options.put(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY, openAnimated);
				
				if (UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS))
					muteVideoSounds = (Boolean)UNITY_ADS_DEVELOPER_OPTIONS.get(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS);
				
				options.put(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS, muteVideoSounds);
				
				if (UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY))
					options.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY));
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not create JSON", UnityAdsProperties.class);
			}

			return options;
		}
		
		return null;
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
