package com.unity3d.ads.android.properties;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.data.UnityAdsDevice;
import com.unity3d.ads.android.view.UnityAdsFullscreenActivity;

public class UnityAdsProperties {
	public static String CAMPAIGN_DATA_URL = "https://impact.applifier.com/mobile/campaigns";
	public static String WEBVIEW_BASE_URL = null;
	public static String ANALYTICS_BASE_URL = null;
	public static String UNITY_ADS_BASE_URL = null;
	public static String UNITY_ADS_GAME_ID = null;
	public static String UNITY_ADS_GAMER_ID = null;
	public static String APPFILTER_LIST = null;
	public static String INSTALLED_APPS_URL = null;
	public static Boolean TESTMODE_ENABLED = false;
	public static WeakReference<Activity> BASE_ACTIVITY = null;
	public static Context APPLICATION_CONTEXT = null;
	public static WeakReference<Activity> CURRENT_ACTIVITY = null;
	public static UnityAdsCampaign SELECTED_CAMPAIGN = null;
	public static Boolean SELECTED_CAMPAIGN_CACHED = false;
	public static int CAMPAIGN_REFRESH_VIEWS_COUNT = 0;
	public static int CAMPAIGN_REFRESH_VIEWS_MAX = 0;
	public static int CAMPAIGN_REFRESH_SECONDS = 0;
	public static long CACHING_SPEED = 0;
	public static String UNITY_VERSION = null;

	// Cannot be final since in test cases values are put into these properties
	@SuppressWarnings("CanBeFinal")
	public static String TEST_DATA = null;
	@SuppressWarnings("CanBeFinal")
	public static String TEST_JAVASCRIPT = null;

	public static Boolean RUN_WEBVIEW_TESTS = false;
	public static Boolean UNITY_DEVELOPER_INTERNAL_TEST = false;

	public static String TEST_DEVELOPER_ID = null;
	public static String TEST_OPTIONS_ID = null;

	@SuppressWarnings("unused")
	private static Map<String, String> TEST_EXTRA_PARAMS = null; 

	public static final int MAX_NUMBER_OF_ANALYTICS_RETRIES = 5;
	public static final int MAX_BUFFERING_WAIT_SECONDS = 20;

	public static Boolean UNITY_ADS_READY_SENT = false;

	private static Boolean SEND_INTERNAL_DETAILS = false;
	private static String _campaignQueryString = null;

	private static void createCampaignQueryString () {
		String queryString = "?";

		//Mandatory params
		try {
			queryString = String.format(Locale.US, "%s%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_PLATFORM_KEY, "android");

			String advertisingId = UnityAdsDevice.getAdvertisingTrackingId();
			if(advertisingId != null) {
				queryString = String.format(Locale.US, "%s&%s=%d", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TRACKINGENABLED_KEY, UnityAdsDevice.isLimitAdTrackingEnabled() ? 0 : 1);

				String advertisingIdMd5 = UnityAdsUtils.Md5(advertisingId).toLowerCase(Locale.US);
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ADVERTISINGTRACKINGID_KEY, URLEncoder.encode(advertisingIdMd5, "UTF-8"));
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_RAWADVERTISINGTRACKINGID_KEY, URLEncoder.encode(advertisingId, "UTF-8"));
			}
			else {
				if (!UnityAdsDevice.getAndroidId(false).equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN)) {
					queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ANDROIDID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(true), "UTF-8"));
					queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_RAWANDROIDID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(false), "UTF-8"));
				}
			}

			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_GAMEID_KEY, URLEncoder.encode(UnityAdsProperties.UNITY_ADS_GAME_ID, "UTF-8"));
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SDKVERSION_KEY, URLEncoder.encode(UnityAdsConstants.UNITY_ADS_VERSION, "UTF-8"));
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SOFTWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getSoftwareVersion(), "UTF-8"));
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_HARDWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getHardwareVersion(), "UTF-8"));
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICETYPE_KEY, UnityAdsDevice.getDeviceType());
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_CONNECTIONTYPE_KEY, URLEncoder.encode(UnityAdsDevice.getConnectionType(), "UTF-8"));

			if(UNITY_VERSION != null && UNITY_VERSION.length() > 0) {
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_UNITYVERSION_KEY, URLEncoder.encode(UNITY_VERSION, "UTF-8"));
			}

			if(!UnityAdsDevice.isUsingWifi()) {
				queryString = String.format(Locale.US, "%s&%s=%d", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ANDROIDNETWORKTYPE_KEY, UnityAdsDevice.getNetworkType());
			}

			if(CACHING_SPEED > 0) {
				queryString = String.format(Locale.US, "%s&%s=%d", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_CACHINGSPEED_KEY, CACHING_SPEED);
			}

			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENSIZE_KEY, UnityAdsDevice.getScreenSize());
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENDENSITY_KEY, UnityAdsDevice.getScreenDensity());

			if(APPFILTER_LIST != null) {
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_APPFILTER_KEY, APPFILTER_LIST);
				APPFILTER_LIST = null;
			}
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Problems creating campaigns query: " + e.getMessage());
		}

		if (TESTMODE_ENABLED) {
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TEST_KEY, "true");

			if (TEST_OPTIONS_ID != null && TEST_OPTIONS_ID.length() > 0) {
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, "optionsId", TEST_OPTIONS_ID);
			}

			if (TEST_DEVELOPER_ID != null && TEST_DEVELOPER_ID.length() > 0) {
				queryString = String.format(Locale.US, "%s&%s=%s", queryString, "developerId", TEST_DEVELOPER_ID);
			}
		}
		else {
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ENCRYPTED_KEY, UnityAdsUtils.isDebuggable() ? "false" : "true");
		}

		if(SEND_INTERNAL_DETAILS) {
			queryString = String.format(Locale.US, "%s&%s=%s", queryString, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SENDINTERNALDETAILS_KEY, "true");
			SEND_INTERNAL_DETAILS = false;
		}

		_campaignQueryString = queryString;
	}

	public static String getCampaignQueryUrl () {
		createCampaignQueryString();
		String url = CAMPAIGN_DATA_URL;

		return String.format(Locale.US, "%s%s", url, _campaignQueryString);
	}

	public static String getCampaignQueryArguments() {
		if(_campaignQueryString != null && _campaignQueryString.length() > 2) {
			return _campaignQueryString.substring(1);
		}

		return "";
	}

	public static Activity getBaseActivity() {
		if (BASE_ACTIVITY != null &&
			BASE_ACTIVITY.get() != null &&
			!BASE_ACTIVITY.get().isFinishing() &&
			!isActivityDestroyed(BASE_ACTIVITY.get())) {
			return BASE_ACTIVITY.get();
		}
		return null;
	}

	public static Activity getCurrentActivity() {
		if (CURRENT_ACTIVITY != null &&
			CURRENT_ACTIVITY.get() != null &&
			!CURRENT_ACTIVITY.get().isFinishing() &&
			!isActivityDestroyed(CURRENT_ACTIVITY.get())) {
			return CURRENT_ACTIVITY.get();
		} else {
			return getBaseActivity();
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean isActivityDestroyed(Activity activity) {
		boolean isDestroyed = false;
		Method isDestroyedMethod = null;

		try {
			isDestroyedMethod = Activity.class.getMethod("isDestroyed");
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Couldn't get isDestroyed -method");
		}

		if (isDestroyedMethod != null) {
			if (activity != null) {
				try {
					isDestroyed = (Boolean) isDestroyedMethod.invoke(activity);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Error running isDestroyed -method");
				}
			}
		}

		return isDestroyed;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isAdsReadySent () {
		return UNITY_ADS_READY_SENT;
	}

	@SuppressWarnings("SameParameterValue")
	public static void setAdsReadySent (boolean sent) {
		UNITY_ADS_READY_SENT = sent;
	}

	public static boolean isShowingAds() {
		return getCurrentActivity() instanceof UnityAdsFullscreenActivity;
	}
}