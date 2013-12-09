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
	private UnityAds unityAdsInstance = null;
	private String gameId = null;
	private String zoneId = null;
	private Map<String, Object> options = null;

	@Override
	protected void loadInterstitial(Context context,
			CustomEventInterstitialListener customEventInterstitialListener,
			Map<String, Object> localExtras, Map<String, String> serverExtras) {
		Log.i("UnityAds", "Got loadInterstitial");
		this.listener = customEventInterstitialListener;
		
		// The gameId must be sent from the server-side
		if(serverExtras.get("gameId") == null || !(serverExtras.get("gameId") instanceof String)) {
			this.listener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			return;
		}
		
		this.gameId = serverExtras.get("gameId");
		this.zoneId = serverExtras.get("zoneId");
		
		this.options = new HashMap<String, Object>();
		this.options.putAll(localExtras);
		this.options.putAll(serverExtras);
		
		UnityAds.setDebugMode(true);
		this.unityAdsInstance = new UnityAds((Activity)context, gameId, this);
		
		Log.d("UnityAds", "initialized");	
	}

	@Override
	protected void showInterstitial() {
		if(this.unityAdsInstance.canShowAds()) {
			this.unityAdsInstance.setZone(this.zoneId);			
			this.unityAdsInstance.show(options);
		}
	}

	@Override 
	protected void onInvalidate() {}

	@Override
	public void onHide() {
		this.listener.onInterstitialDismissed();
	}

	@Override
	public void onShow() {
		this.listener.onInterstitialShown();
	}
	
	@Override
	public void onVideoStarted() {}
	@Override
	public void onVideoCompleted(String rewardItemKey, boolean skipped) {}

	@Override
	public void onFetchCompleted() {
		Log.d("UnityAds", "onFetchCompleted");
		this.listener.onInterstitialLoaded();
	}

	@Override
	public void onFetchFailed() {
		this.listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);	
	}
}