package com.applifier.impact.android;

import java.util.ArrayList;
import java.util.Map;

import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;

import android.app.Activity;
import android.os.Build;

public class ApplifierImpact implements IUnityAdsListener {
	
	// Reward item HashMap keys
	public static final String APPLIFIER_IMPACT_REWARDITEM_PICTURE_KEY = "picture";
	public static final String APPLIFIER_IMPACT_REWARDITEM_NAME_KEY = "name";
	
	// Impact developer options keys
	public static final String APPLIFIER_IMPACT_OPTION_NOOFFERSCREEN_KEY = "noOfferScreen";
	public static final String APPLIFIER_IMPACT_OPTION_OPENANIMATED_KEY = "openAnimated";
	public static final String APPLIFIER_IMPACT_OPTION_GAMERSID_KEY = "sid";
	public static final String APPLIFIER_IMPACT_OPTION_MUTE_VIDEO_SOUNDS = "muteVideoSounds";
	public static final String APPLIFIER_IMPACT_OPTION_VIDEO_USES_DEVICE_ORIENTATION = "useDeviceOrientationForVideo";

	// Impact components
	public static ApplifierImpact instance = null;
	
	// Temporary data
	private boolean _instanceInitialized = false;
	
	// Listeners
	private IApplifierImpactListener _impactListener = null;
	
	
	public ApplifierImpact (Activity activity, String gameId) {
		init(activity, gameId, null);
	}
	
	public ApplifierImpact (Activity activity, String gameId, IApplifierImpactListener listener) {
		init(activity, gameId, listener);
	}

	private void init (final Activity activity, String gameId, IApplifierImpactListener listener) {
		instance = this;
		setImpactListener(listener);

		if (!_instanceInitialized && UnityAds.instance == null) {
			_instanceInitialized = true;
			new UnityAds(activity, gameId, this);
		}
	}

	
	/* PUBLIC STATIC METHODS */
	
	public static boolean isSupported () {
		if (Build.VERSION.SDK_INT < 9) {
			return false;
		}
		
		return true;
	}
	
	public static void setDebugMode (boolean debugModeEnabled) {
		UnityAds.setDebugMode(debugModeEnabled);
	}
	
	public static void setTestMode (boolean testModeEnabled) {
		UnityAds.setTestMode(testModeEnabled);
	}
	
	public static void setTestDeveloperId (String testDeveloperId) {
		UnityAds.setTestDeveloperId(testDeveloperId);
	}
	
	public static void setTestOptionsId (String testOptionsId) {
		UnityAds.setTestOptionsId(testOptionsId);
	}
	
	public static String getSDKVersion () {
		return UnityAds.getSDKVersion();
	}
	
	
	/* PUBLIC METHODS */
	
	public void setImpactListener (IApplifierImpactListener listener) {
		_impactListener = listener;
	}
	
	public void changeActivity (Activity activity) {
		UnityAds.instance.changeActivity(activity);
	}
	
	public boolean hideImpact () {
		return UnityAds.instance.hide();
	}
	
	public boolean setZone(String zoneId) {
		return UnityAds.instance.setZone(zoneId);
	}
	
	public boolean setZone(String zoneId, String rewardItemKey) {
		return UnityAds.instance.setZone(zoneId, rewardItemKey);
	}
	
	public boolean showImpact (Map<String, Object> options) {
		return UnityAds.instance.show(options);
	}
	
	public boolean showImpact () {
		return UnityAds.instance.show();
	}
	
	public boolean canShowCampaigns () {
		return UnityAds.instance.canShowAds();
	}
	
	public boolean canShowImpact () {
		return UnityAds.instance.canShow();
	}

	public void stopAll () {
	}
	
	
	/* PUBLIC MULTIPLE REWARD ITEM SUPPORT */
	
	public boolean hasMultipleRewardItems () {
		return UnityAds.instance.hasMultipleRewardItems();
	}
	
	public ArrayList<String> getRewardItemKeys () {
		return UnityAds.instance.getRewardItemKeys();
	}
	
	public String getDefaultRewardItemKey () {
		return UnityAds.instance.getDefaultRewardItemKey();
	}
	
	public String getCurrentRewardItemKey () {
		return UnityAds.instance.getCurrentRewardItemKey();
	}
	
	public boolean setRewardItemKey (String rewardItemKey) {
		return UnityAds.instance.setRewardItemKey(rewardItemKey);
	}
	
	public void setDefaultRewardItemAsRewardItem () {
		UnityAds.instance.setDefaultRewardItemAsRewardItem();
	}
	
	public Map<String, String> getRewardItemDetailsWithKey (String rewardItemKey) {
		return UnityAds.instance.getRewardItemDetailsWithKey(rewardItemKey);
	}
	
	@Override
	public void onHide() {
		if (_impactListener != null)
			_impactListener.onImpactClose();
	}

	@Override
	public void onShow() {
		if (_impactListener != null)
			_impactListener.onImpactOpen();
	}

	@Override
	public void onVideoStarted() {
		if (_impactListener != null) {
			_impactListener.onVideoStarted();
		}
	}

	@Override
	public void onVideoCompleted(String rewardItemKey, boolean skipped) {
		if (_impactListener != null) {
			_impactListener.onVideoCompleted(rewardItemKey, skipped);
		}
	}

	@Override
	public void onFetchCompleted() {
		if (_impactListener != null) {
			_impactListener.onCampaignsAvailable();
		}
	}

	@Override
	public void onFetchFailed() {
		if (_impactListener != null) {
			_impactListener.onCampaignsFetchFailed();
		}
	}
}
