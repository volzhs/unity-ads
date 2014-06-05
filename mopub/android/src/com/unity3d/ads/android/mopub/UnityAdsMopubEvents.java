package com.unity3d.ads.android.mopub;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

public class UnityAdsMopubEvents extends CustomEventInterstitial implements IUnityAdsListener {
	
	private CustomEventInterstitialListener listener = null;
	private String gameId = null;
	private String zoneId = null;
	private Map<String, Object> options = null;

	@Override
	protected void loadInterstitial(Context context,
			CustomEventInterstitialListener customEventInterstitialListener,
			Map<String, Object> localExtras, Map<String, String> serverExtras) {
		listener = customEventInterstitialListener;	
		
		if(serverExtras.get("gameId") == null || !(serverExtras.get("gameId") instanceof String)) {
			listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			return;
		}
		
		gameId = serverExtras.get("gameId");
		zoneId = serverExtras.get("zoneId");
		
		options = new HashMap<String, Object>();
		options.putAll(localExtras);
		options.putAll(serverExtras);
		
		UnityAds.init((Activity)context, gameId, this);
		UnityAds.changeActivity((Activity)context);
		UnityAds.setListener(this);
		
		if (UnityAds.canShowAds()) {
			listener.onInterstitialLoaded();
		}
	}

	@Override
	protected void showInterstitial() {
		if(UnityAds.canShow() && UnityAds.canShowAds()) {
			UnityAds.setZone(zoneId);			
			UnityAds.show(options);
		} else {
			listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
		}
	}

	@Override 
	protected void onInvalidate() {
		UnityAdsDeviceLog.entered();
	}

	@Override
	public void onHide() {
		UnityAdsDeviceLog.entered();
		listener.onInterstitialDismissed();
	}

	@Override
	public void onShow() {
		UnityAdsDeviceLog.entered();
		listener.onInterstitialShown();
	}
	
	@Override
	public void onVideoStarted() {
		UnityAdsDeviceLog.entered();
	}
	
	@Override
	public void onVideoCompleted(String rewardItemKey, boolean skipped) {
		UnityAdsDeviceLog.debug(rewardItemKey + ", " + skipped);
	}

	@Override
	public void onFetchCompleted() {
		UnityAdsDeviceLog.entered();
		listener.onInterstitialLoaded();
	}

	@Override
	public void onFetchFailed() {
		UnityAdsDeviceLog.entered();
		listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);	
	}
}