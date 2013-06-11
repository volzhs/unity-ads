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
	
	public static String UNITY_ADS_MOPUB_MUTE_OPTION = "muteSounds";
	public static String UNITY_ADS_MOPUB_ORIENTATION_OPTION = "deviceOrientation";
	
	private boolean optionMute = false;
	private boolean optionDeviceOrientation = false;

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
		
		// First go through the local extras, then the server extras so we can easily
		// over-ride the settings on the server-side
		Map[] maps = new Map[] {localExtras, serverExtras};
		
		for(Map<String, String> opMap : maps) {
			for(String key : opMap.keySet()) {
				if(UNITY_ADS_MOPUB_MUTE_OPTION.equals(key)) {
					if("true".equals(opMap.get(UNITY_ADS_MOPUB_MUTE_OPTION))) {
						this.optionMute = true;
					}
				}
				if(UNITY_ADS_MOPUB_ORIENTATION_OPTION.equals(key)) {
					if("true".equals(opMap.get(UNITY_ADS_MOPUB_ORIENTATION_OPTION))) {
						this.optionDeviceOrientation = true;
					}
				}
			}
		}
		
		UnityAds.setDebugMode(true);
		this.unityAdsInstance = new UnityAds((Activity)context, gameId, this);
		
		Log.d("UnityAds", "initialized");
		
		
	}

	@Override
	protected void showInterstitial() {
		if(this.unityAdsInstance.canShowAds()) {
			
			HashMap<String, Object> options = new HashMap<String, Object>();
			
			// Always use no offer screen as MoPub is always non-incent
			options.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, true);
			
			// Configured options
			options.put(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION, this.optionDeviceOrientation);
			options.put(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS, this.optionMute);
			
			this.unityAdsInstance.show(options);
		}
	}

	@Override 
	protected void onInvalidate() {

	}

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
	public void onVideoCompleted(String rewardItemKey) {}

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
