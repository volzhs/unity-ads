package com.unity3d.ads.android;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

import com.unity3d.ads.android.cache.UnityAdsCache;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.data.UnityAdsAdvertisingId;
import com.unity3d.ads.android.data.UnityAdsDevice;
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
import com.unity3d.ads.android.zone.UnityAdsZoneManager;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAds implements IUnityAdsWebDataListener {

	// Reward item HashMap keys
	public static final String UNITY_ADS_REWARDITEM_PICTURE_KEY = "picture";
	public static final String UNITY_ADS_REWARDITEM_NAME_KEY = "name";

	// Unity Ads developer options keys
	public static final String UNITY_ADS_OPTION_NOOFFERSCREEN_KEY = "noOfferScreen";
	public static final String UNITY_ADS_OPTION_OPENANIMATED_KEY = "openAnimated";
	public static final String UNITY_ADS_OPTION_GAMERSID_KEY = "sid";
	public static final String UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS = "muteVideoSounds";
	public static final String UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION = "useDeviceOrientationForVideo";

	// Temporary data
	private static boolean _initialized = false;

	// Listeners
	private static IUnityAdsListener _adsListener = null;

	private static UnityAds _instance = null;

	private UnityAds() {
	}

	/* PUBLIC STATIC METHODS */

	public static boolean isSupported() {
		return Build.VERSION.SDK_INT >= 9;
	}

	@SuppressWarnings("SameParameterValue")
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

	@SuppressWarnings("SameReturnValue")
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
		UnityAdsProperties.CURRENT_ACTIVITY = new WeakReference<>(activity);
		if (!(activity instanceof UnityAdsFullscreenActivity)) {
			UnityAdsProperties.BASE_ACTIVITY = new WeakReference<>(activity);
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

	@SuppressWarnings("unused")
	public static String getZone() {
		UnityAdsZoneManager zoneManager = UnityAdsWebData.getZoneManager();

		if(zoneManager != null) {
			UnityAdsZone currentZone = zoneManager.getCurrentZone();

			if(currentZone != null) {
				return currentZone.getZoneId();
			}
		}

		return null;
	}

	public static boolean show (Map<String, Object> options) {
		if (canShow()) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();

			if (currentZone != null) {
				UnityAdsCache.stopAllDownloads();

				currentZone.mergeOptions(options);

				if (currentZone.noOfferScreen()) {
					ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();

					if (viewableCampaigns.size() > 0) {
						UnityAdsProperties.SELECTED_CAMPAIGN = viewableCampaigns.get(0);
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

	@SuppressWarnings("unused")
	public static boolean show() {
		return show(null);
	}

	private static void startFullscreenActivity () {
		Intent newIntent = new Intent(UnityAdsProperties.getCurrentActivity(), UnityAdsFullscreenActivity.class);
		int flags = Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK;

		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.openAnimated()) {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK;
		}

		newIntent.addFlags(flags);
		Activity baseActivity = UnityAdsProperties.getBaseActivity();

		try {
			if (baseActivity != null) baseActivity.startActivity(newIntent);
		} catch (ActivityNotFoundException e) {
			UnityAdsDeviceLog.error("Could not find UnityAdsFullScreenActivity (failed Android manifest merging?): " + e.getMessage());
		} catch (Exception e) {
			UnityAdsDeviceLog.error("Weird error: " + e.getMessage());
		}
	}

	/**
	 * Returns if ads can be shown or not.
	 * @deprecated use {@link #canShow()} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public static boolean canShowAds() {
		return canShow();
	}

	private static boolean isShowingAds() {
		return UnityAdsProperties.isShowingAds();
	}

	public static boolean canShow() {
		if(!UnityAdsProperties.isAdsReadySent()) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.WEBAPP_NOT_INITIALIZED);
			return false;
		}

		if(isShowingAds()) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.SHOWING_ADS);
			return false;
		}

		if(!UnityAdsDevice.isActiveNetworkConnected()) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.NO_INTERNET);
			return false;
		}

		if(UnityAdsWebData.initInProgress()) return false;

		ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();

		if(viewableCampaigns == null) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.NO_ADS);
			return false;
		}

		if(viewableCampaigns.size() == 0) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.ZERO_ADS);
			return false;
		}

		// TODO: if for some reason the streaming video won't be allowed and the campaign is not cached by then, UnityAds will stay like that and is unable to view any ads.
		UnityAdsCampaign nextCampaign = viewableCampaigns.get(0);
		if(!nextCampaign.allowStreamingVideo() && !UnityAdsCache.isCampaignCached(nextCampaign)) {
			UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.VIDEO_NOT_CACHED);
			return false;
		}

		UnityAdsDeviceLog.logShowStatus(UnityAdsDeviceLog.UnityAdsShowMsg.READY);
		return true;
	}

	/* PUBLIC MULTIPLE REWARD ITEM SUPPORT */

	/**
	 * Check if adplan has multiple reward items
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().allItems().count()}
	 */
	@Deprecated
	public static boolean hasMultipleRewardItems() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.itemCount() > 1;
		}
		return false;
	}

	/**
	 * Get a list of all the reward items
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().allItems()}
	 */
	@Deprecated
	public static ArrayList<String> getRewardItemKeys() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			ArrayList<UnityAdsRewardItem> rewardItems = itemManager.allItems();
			ArrayList<String> rewardItemKeys = new ArrayList<>();
			for (UnityAdsRewardItem rewardItem : rewardItems) {
				rewardItemKeys.add(rewardItem.getKey());
			}

			return rewardItemKeys;
		}
		return null;
	}

	/**
	 * Get the default reward item key
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().getDefaultItem().getKey()}
	 */
	@Deprecated
	public static String getDefaultRewardItemKey() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.getDefaultItem().getKey();
		}
		return null;
	}

	/**
	 * Get the current reward item key
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().getCurrentItem().getKey()}
	 */
	@Deprecated
	public static String getCurrentRewardItemKey() {
		UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (zone != null && zone.isIncentivized()) {
			UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
			return itemManager.getCurrentItem().getKey();
		}
		return null;
	}

	/**
	 * Set the current reward item key
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().setCurrentItem()}
	 */
	@Deprecated
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

	/**
	 * Sets the default reward item as current reward item
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().setCurrentItem()} with {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().getDefaultItem()}
	 */
	@Deprecated
	public static void setDefaultRewardItemAsRewardItem() {
		if (canShow()) {
			UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (zone != null && zone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone) zone).itemManager();
				itemManager.setCurrentItem(itemManager.getDefaultItem().getKey());
			}
		}
	}

	/**
	 * Get details for the specified reward item
	 * @deprecated use {@link com.unity3d.ads.android.webapp.UnityAdsWebData#getZoneManager().getCurrentZone().itemManager().getItem()}
	 */
	@Deprecated
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

		if (!dataFetchFailed && !sdkIsCurrent && UnityAdsProperties.getCurrentActivity() != null && UnityAdsUtils.isDebuggable()) {
			final AlertDialog alertDialog = new AlertDialog.Builder(UnityAdsProperties.getCurrentActivity()).create();
			alertDialog.setTitle("Unity Ads");
			alertDialog.setMessage("You are not running the latest version of Unity Ads android. Please update your version (this dialog won't appear in release builds).");
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					alertDialog.dismiss();
				}
			});

			alertDialog.show();
		}

		setup();
	}

	@Override
	public void onWebDataFailed() {
		if (getListener() != null && !UnityAdsProperties.UNITY_ADS_READY_SENT) {
			getListener().onFetchFailed();
			UnityAdsProperties.UNITY_ADS_READY_SENT = true;
		}
	}

	public static void init (final Activity activity, String gameId, IUnityAdsListener listener) {
		if (_instance != null || _initialized) return;

		if (gameId == null || gameId.length() == 0) {
			UnityAdsDeviceLog.error("Unity Ads init failed: gameId is empty");
			return;
		} else {
			try {
				int gameIdInteger = Integer.parseInt(gameId);
				if (gameIdInteger <= 0) {
					UnityAdsDeviceLog.error("Unity Ads init failed: gameId is invalid");
					return;
				}
			} catch (NumberFormatException e) {
				UnityAdsDeviceLog.error("Unity Ads init failed: gameId does not parse as an integer");
				return;
			}
		}

		if (UnityAdsProperties.UNITY_VERSION != null && UnityAdsProperties.UNITY_VERSION.length() > 0) {
			UnityAdsDeviceLog.info("Initializing Unity Ads version " + UnityAdsConstants.UNITY_ADS_VERSION + " (Unity + " + UnityAdsProperties.UNITY_VERSION + ") with gameId " + gameId);
		} else {
			UnityAdsDeviceLog.info("Initializing Unity Ads version " + UnityAdsConstants.UNITY_ADS_VERSION + " with gameId " + gameId);
		}

		int videoLayoutId = activity.getResources().getIdentifier("unityads_view_video_play", "layout", activity.getPackageName());
		if(videoLayoutId == 0) {
			UnityAdsDeviceLog.error("Unity Ads layout resources not found, check that you have properly merged Unity Ads resource files in your project");
			return;
		} else {
			UnityAdsDeviceLog.debug("Unity Ads layout resources ok");
		}

		try {
			Class<?> unityAdsWebBridge = Class.forName("com.unity3d.ads.android.webapp.UnityAdsWebBridge");
			Method handleWebEvent = unityAdsWebBridge.getMethod("handleWebEvent", String.class, String.class);

			// JavascriptInterface annotation was added in API level 17
			if(Build.VERSION.SDK_INT >= 17) {
				Annotation[] annotations = handleWebEvent.getAnnotations();

				boolean annotationMissing = true;
				if (annotations != null) {
					for (Annotation a : annotations) {
						Class<?> annotationClass = a.annotationType();
						if (annotationClass != null && annotationClass.isAnnotation() && annotationClass.getName().equals("android.webkit.JavascriptInterface")) {
							annotationMissing = false;
							break;
						}
					}
				}

				if (annotationMissing) {
					UnityAdsDeviceLog.error("UnityAds ProGuard check fail: com.unity3d.ads.android.webapp.handleWebEvent lacks android.webkit.JavascriptInterface annotation");
					return;
				}
			}

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

		if (_instance == null) {
			_instance = new UnityAds();
		}

		setListener(listener);

		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
		UnityAdsProperties.BASE_ACTIVITY = new WeakReference<>(activity);
		UnityAdsProperties.APPLICATION_CONTEXT = activity.getApplicationContext();
		UnityAdsProperties.CURRENT_ACTIVITY = new WeakReference<>(activity);

		UnityAdsDeviceLog.debug("Is debuggable=" + UnityAdsUtils.isDebuggable());

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
			UnityAdsCache.initialize(UnityAdsWebData.getVideoPlanCampaigns());
		}
	}
}
