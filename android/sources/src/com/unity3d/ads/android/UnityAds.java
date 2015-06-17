package com.unity3d.ads.android;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.data.UnityAdsAdvertisingId;
import com.unity3d.ads.android.item.UnityAdsRewardItem;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsFullscreenActivity;
import com.unity3d.ads.android.view.UnityAdsMainView;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.webapp.IUnityAdsWebDataListener;
import com.unity3d.ads.android.zone.UnityAdsIncentivizedZone;
import com.unity3d.ads.android.zone.UnityAdsZone;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAds implements IUnityAdsCacheListener, IUnityAdsWebDataListener {

	// Reward item HashMap keys
	public static final String UNITY_ADS_REWARDITEM_PICTURE_KEY = "picture";
	public static final String UNITY_ADS_REWARDITEM_NAME_KEY = "name";

	// Unity Ads developer options keys
	public static final String UNITY_ADS_OPTION_NOOFFERSCREEN_KEY = "noOfferScreen";
	public static final String UNITY_ADS_OPTION_OPENANIMATED_KEY = "openAnimated";
	public static final String UNITY_ADS_OPTION_GAMERSID_KEY = "sid";
	public static final String UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS = "muteVideoSounds";
	public static final String UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION = "useDeviceOrientationForVideo";

	// Unity Ads components
	public static UnityAdsCacheManager cachemanager = null;

	// Temporary data
	private static boolean _initialized = false;
	private static AlertDialog _alertDialog = null;

	// Listeners
	private static IUnityAdsListener _adsListener = null;

	private static UnityAds _instance = null;

	private UnityAds() {
	}

	/* PUBLIC STATIC METHODS */

	public static boolean isSupported() {
		if (Build.VERSION.SDK_INT < 9) {
			return false;
		}

		return true;
	}

	public static void setDebugMode(boolean debugModeEnabled) {
		if (debugModeEnabled) {
			UnityAdsDeviceLog.setLogLevel(UnityAdsDeviceLog.LOGLEVEL_DEBUG);
		} else {
			UnityAdsDeviceLog.setLogLevel(UnityAdsDeviceLog.LOGLEVEL_INFO);
		}
	}

	public static void setTestMode(boolean testModeEnabled) {
		UnityAdsProperties.TESTMODE_ENABLED = testModeEnabled;
	}

	public static void setTestDeveloperId(String testDeveloperId) {
		UnityAdsProperties.TEST_DEVELOPER_ID = testDeveloperId;
	}

	public static void setTestOptionsId(String testOptionsId) {
		UnityAdsProperties.TEST_OPTIONS_ID = testOptionsId;
	}

	public static String getSDKVersion() {
		return UnityAdsConstants.UNITY_ADS_VERSION;
	}

	public static void setCampaignDataURL(String campaignDataURL) {
		UnityAdsProperties.CAMPAIGN_DATA_URL = campaignDataURL;
	}

	public static void enableUnityDeveloperInternalTestMode() {
		UnityAdsProperties.CAMPAIGN_DATA_URL = "https://impact.staging.applifier.com/mobile/campaigns";
		UnityAdsProperties.UNITY_DEVELOPER_INTERNAL_TEST = true;
	}

	/* PUBLIC METHODS */

	public static void setListener(IUnityAdsListener listener) {
		_adsListener = listener;
	}

	public static IUnityAdsListener getListener() {
		return _adsListener;
	}

	public static void changeActivity(Activity activity) {
		if (activity == null) {
			UnityAdsDeviceLog.debug("changeActivity: null, ignoring");
			return;
		}

		UnityAdsDeviceLog.debug("changeActivity: " + activity.getClass().getName());

		if (activity != null && !activity.equals(UnityAdsProperties.getCurrentActivity())) {
			UnityAdsProperties.CURRENT_ACTIVITY = new WeakReference<Activity>(activity);
			if (!(activity instanceof UnityAdsFullscreenActivity)) {
				UnityAdsProperties.BASE_ACTIVITY = new WeakReference<Activity>(activity);
			}
		}
	}

	public static boolean hide() {
		if (UnityAdsProperties.CURRENT_ACTIVITY.get() instanceof UnityAdsFullscreenActivity) {
			UnityAdsProperties.CURRENT_ACTIVITY.get().finish();
			return true;
		}

		return false;
	}

	public static boolean setZone(String zoneId) {
		if (!isShowingAds()) {
			if (UnityAdsWebData.getZoneManager() == null) {
				throw new IllegalStateException("Unable to set zone before campaigns are available");
			}

			return UnityAdsWebData.getZoneManager().setCurrentZone(zoneId);
		}
		return false;
	}

	public static boolean setZone(String zoneId, String rewardItemKey) {
		if (!isShowingAds() && setZone(zoneId)) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (currentZone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) currentZone).itemManager();
				return itemManager.setCurrentItem(rewardItemKey);
			}
		}
		return false;
	}

	public static boolean show(Map<String, Object> options) {
		if (canShow()) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();

			if (currentZone != null) {
				UnityAdsDownloader.stopAllDownloads();

				currentZone.mergeOptions(options);

				if (currentZone.noOfferScreen()) {
					ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();

					if (viewableCampaigns.size() > 0) {
						UnityAdsCampaign selectedCampaign = viewableCampaigns.get(0);
						UnityAdsProperties.SELECTED_CAMPAIGN = selectedCampaign;
					}
				}

				UnityAdsDeviceLog.info("Launching ad from \"" + currentZone.getZoneName() + "\", options: " + currentZone.getZoneOptions().toString());
				UnityAdsProperties.SELECTED_CAMPAIGN_CACHED = false;
				startFullscreenActivity();
				return true;
			} else {
				UnityAdsDeviceLog.error("Unity Ads current zone is null");
			}
		} else {
			UnityAdsDeviceLog.error("Unity Ads not ready to show ads");
		}

		return false;
	}

	public static boolean show() {
		return show(null);
	}

	public static boolean canShowAds() {
		return canShow();
	}

	private static boolean isShowingAds() {
		return UnityAdsProperties.isShowingAds();
	}

	public static boolean canShow() {
		/*
		if(webdata == null) {
			logCanShow(1);
			return false;
		}*/


		if(!UnityAdsProperties.isAdsReadySent()) {
			logCanShow(2);
			return false;
		}

		if(isShowingAds()) {
			logCanShow(3);
			return false;
		}

		Activity currentActivity = UnityAdsProperties.getCurrentActivity();
		if (currentActivity != null) {
			ConnectivityManager cm = (ConnectivityManager) currentActivity.getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

			if (cm != null) {
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

				if(!isConnected) {
					logCanShow(4);
					return false;
				}
			}
		}

		if(UnityAdsWebData.initInProgress()) return false;

		ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();

		if(viewableCampaigns == null) {
			logCanShow(5);
			return false;
		}

		if(viewableCampaigns.size() == 0) {
			logCanShow(6);
			return false;
		}

		UnityAdsCampaign nextCampaign = viewableCampaigns.get(0);
		if(!nextCampaign.allowStreamingVideo().booleanValue()) {
			if(!cachemanager.isCampaignCached(nextCampaign, true)) {
				logCanShow(7);
				return false;
			}
		}

		logCanShow(0);
		return true;
	}

	private static int prevCanShowLogMsg = -1;
	private static final String[] canShowLogMsgs = {
		"Unity Ads is ready to show ads",
		"Unity Ads not ready to show ads: not initialized",
		"Unity Ads not ready to show ads: webapp not initialized",
		"Unity Ads not ready to show ads: already showing ads",
		"Unity Ads not ready to show ads: no internet connection available",
		"Unity Ads not ready to show ads: no ads are available",
		"Unity Ads not ready to show ads: zero ads available",
		"Unity Ads not ready to show ads: video not cached",
	};

	private static void logCanShow(int reason) {
		if(reason != prevCanShowLogMsg) {
			prevCanShowLogMsg = reason;
			UnityAdsDeviceLog.info(canShowLogMsgs[reason]);
		}
	}

	/* PUBLIC MULTIPLE REWARD ITEM SUPPORT */

	public static boolean hasMultipleRewardItems() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.itemCount() > 1;
		}
		return false;
	}

	public static ArrayList<String> getRewardItemKeys() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			ArrayList<UnityAdsRewardItem> rewardItems = itemManager.allItems();
			ArrayList<String> rewardItemKeys = new ArrayList<String>();
			for (UnityAdsRewardItem rewardItem : rewardItems) {
				rewardItemKeys.add(rewardItem.getKey());
			}

			return rewardItemKeys;
		}
		return null;
	}

	public static String getDefaultRewardItemKey() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.getDefaultItem().getKey();
		}
		return null;
	}

	public static String getCurrentRewardItemKey() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.getCurrentItem().getKey();
		}
		return null;
	}

	public static boolean setRewardItemKey(String rewardItemKey) {
		if (canShow()) {
			UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (zone != null && zone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
				return itemManager.setCurrentItem(rewardItemKey);
			}
		}
		return false;
	}

	public static void setDefaultRewardItemAsRewardItem() {
		if (canShow()) {
			UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (zone != null && zone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
				itemManager.setCurrentItem(itemManager.getDefaultItem().getKey());
			}
		}
	}

	public static Map<String, String> getRewardItemDetailsWithKey(String rewardItemKey) {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			UnityAdsRewardItem rewardItem = itemManager.getItem(rewardItemKey);
			if (rewardItem != null) {
				return rewardItem.getDetails();
			} else {
				UnityAdsDeviceLog.info("Could not fetch reward item: " + rewardItemKey);
			}
		}
		return null;
	}

	/* LISTENER METHODS */

	// IUnityAdsCacheListener
	@Override
	public void onCampaignUpdateStarted() {
		UnityAdsDeviceLog.debug("Campaign updates started.");
	}

	@Override
	public void onCampaignReady(UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;

		UnityAdsDeviceLog.debug(campaignHandler.getCampaign().toString());
	}

	@Override
	public void onAllCampaignsReady() {
		UnityAdsDeviceLog.entered();
	}

	// IUnityAdsWebDataListener
	@SuppressWarnings("deprecation")
	@Override
	public void onWebDataCompleted() {
		UnityAdsDeviceLog.entered();
		JSONObject jsonData = null;
		boolean dataFetchFailed = false;
		boolean sdkIsCurrent = true;

		if (UnityAdsWebData.getData() != null && UnityAdsWebData.getData().has(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY)) {
			try {
				jsonData = UnityAdsWebData.getData().getJSONObject(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY);
			} catch (Exception e) {
				dataFetchFailed = true;
			}

			if (!dataFetchFailed) {
				UnityAdsWebData.setupCampaignRefreshTimer();

				if (jsonData.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_DATAPARAM_SDK_IS_CURRENT_KEY)) {
					try {
						sdkIsCurrent = jsonData.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_DATAPARAM_SDK_IS_CURRENT_KEY);
					} catch (Exception e) {
						dataFetchFailed = true;
					}
				}
			}
		}

		if (!dataFetchFailed && !sdkIsCurrent && UnityAdsProperties.getCurrentActivity() != null && UnityAdsUtils.isDebuggable(UnityAdsProperties.getCurrentActivity())) {
			_alertDialog = new AlertDialog.Builder(UnityAdsProperties.getCurrentActivity()).create();
			_alertDialog.setTitle("Unity Ads");
			_alertDialog.setMessage("You are not running the latest version of Unity Ads android. Please update your version (this dialog won't appear in release builds).");
			_alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					_alertDialog.dismiss();
				}
			});

			_alertDialog.show();
		}

		setup();
	}

	@Override
	public void onWebDataFailed() {
		if (getListener() != null && !UnityAdsProperties.UNITY_ADS_READY_SENT)
			getListener().onFetchFailed();
	}

	public static void init (final Activity activity, String gameId, IUnityAdsListener listener) {
		if (_instance != null || _initialized) return;

		if (gameId == null || gameId.length() == 0) {
			throw new IllegalArgumentException("gameId is empty");
		} else {
			try {
				int gameIdInteger = Integer.parseInt(gameId);
				if (gameIdInteger <= 0) {
					throw new IllegalArgumentException("gameId is invalid");
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("gameId does not parse as an integer");
			}
		}

		if (UnityAdsProperties.UNITY_VERSION != null && UnityAdsProperties.UNITY_VERSION.length() > 0) {
			UnityAdsDeviceLog.info("Initializing Unity Ads version " + UnityAdsConstants.UNITY_ADS_VERSION + " (Unity + " + UnityAdsProperties.UNITY_VERSION + ") with gameId " + gameId);
		} else {
			UnityAdsDeviceLog.info("Initializing Unity Ads version " + UnityAdsConstants.UNITY_ADS_VERSION + " with gameId " + gameId);
		}

		try {
			Class<?> unityAdsWebBridge = Class.forName("com.unity3d.ads.android.webapp.UnityAdsWebBridge");
			@SuppressWarnings("unused")
			Method handleWebEvent = unityAdsWebBridge.getMethod("handleWebEvent", String.class, String.class);
			UnityAdsDeviceLog.debug("UnityAds ProGuard check OK");
		} catch (ClassNotFoundException e) {
			UnityAdsDeviceLog.error("UnityAds ProGuard check fail: com.unity3d.ads.android.webapp.UnityAdsWebBridge class not found, check ProGuard settings");
			return;
		} catch (NoSuchMethodException e) {
			UnityAdsDeviceLog.error("UnityAds ProGuard check fail: com.unity3d.ads.android.webapp.handleWebEvent method not found, check ProGuard settings");
			return;
		} catch (Exception e) {
			UnityAdsDeviceLog.debug("UnityAds ProGuard check: Unknown exception: " + e);
		}

		String pkgName = activity.getPackageName();
		PackageManager pm = activity.getPackageManager();

		if (pkgName != null && pm != null) {
			try {
				PackageInfo pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);

				for (int i = 0; i < pkgInfo.activities.length; i++) {
					if (pkgInfo.activities[i].launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
						// TODO: Do we still need this?
						//_singleTaskApplication = true;
						UnityAdsDeviceLog.debug("Running in singleTask application mode");
					}
				}
			} catch (Exception e) {
				UnityAdsDeviceLog.debug("Error while checking singleTask activities");
			}
		}

		if (_instance == null) {
			_instance = new UnityAds();
		}

		setListener(listener);

		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
		UnityAdsProperties.BASE_ACTIVITY = new WeakReference<Activity>(activity);
		UnityAdsProperties.APPLICATION_CONTEXT = new WeakReference<Context>(activity.getApplicationContext());
		UnityAdsProperties.CURRENT_ACTIVITY = new WeakReference<Activity>(activity);

		UnityAdsDeviceLog.debug("Is debuggable=" + UnityAdsUtils.isDebuggable(activity));

		cachemanager = new UnityAdsCacheManager(activity);
		cachemanager.setDownloadListener(_instance);
		//webdata = new UnityAdsWebData();
		UnityAdsWebData.setWebDataListener(_instance);

		new Thread(new Runnable() {
			public void run() {
				UnityAdsAdvertisingId.init(activity);
				if (UnityAdsWebData.initCampaigns()) {
					_initialized = true;
				}
			}
		}).start();
	}

	private static void setup() {
		initCache();

		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				UnityAdsMainView.initWebView();
			}
		});
	}

	private static void initCache() {
		UnityAdsDeviceLog.entered();
		if (_initialized) {
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(UnityAdsWebData.getVideoPlanCampaigns());
		}
	}

	private static void startFullscreenActivity () {
		Intent newIntent = new Intent(UnityAdsProperties.getCurrentActivity(), com.unity3d.ads.android.view.UnityAdsFullscreenActivity.class);
		int flags = Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK;

		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.openAnimated()) {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK;
		}

		newIntent.addFlags(flags);

		try {
			UnityAdsProperties.getBaseActivity().startActivity(newIntent);
		} catch (ActivityNotFoundException e) {
			UnityAdsDeviceLog.error("Could not find UnityAdsFullScreenActivity (failed Android manifest merging?): " + e.getStackTrace());
		} catch (Exception e) {
			UnityAdsDeviceLog.error("Weird error: " + e.getStackTrace());
		}
	}
}