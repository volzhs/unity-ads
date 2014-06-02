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
		
		if(UnityAds.instance == null) {
			new UnityAds((Activity)context, gameId, this);
		} else {
			UnityAds.instance.changeActivity((Activity)context);
			UnityAds.instance.setListener(this);
			listener.onInterstitialLoaded();
		}		
	}

	@Override
	protected void showInterstitial() {
		if(UnityAds.instance.canShow() && UnityAds.instance.canShowAds()) {
			UnityAds.instance.setZone(zoneId);			
			UnityAds.instance.show(options);
		} else {
			listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
		}
	}

	@Override 
	protected void onInvalidate() {
		Log.d("UnityAds", "onInvalidate");
	}

	@Override
	public void onHide() {
		Log.d("UnityAds", "onHide");
		listener.onInterstitialDismissed();
	}

	@Override
	public void onShow() {
		Log.d("UnityAds", "onShow");
		listener.onInterstitialShown();
	}
	
	@Override
	public void onVideoStarted() {
		Log.d("UnityAds", "onVideoStarted");
	}
	
	@Override
	public void onVideoCompleted(String rewardItemKey, boolean skipped) {
		Log.d("UnityAds", "onVideoCompleted - " + rewardItemKey + " - " + skipped);
	}

	@Override
	public void onFetchCompleted() {
		Log.d("UnityAds", "onFetchCompleted");
		listener.onInterstitialLoaded();
	}

	@Override
	public void onFetchFailed() {
		Log.d("UnityAds", "onFetchFailed");
		listener.onInterstitialFailed(MoPubErrorCode.NO_FILL);	
	}
}