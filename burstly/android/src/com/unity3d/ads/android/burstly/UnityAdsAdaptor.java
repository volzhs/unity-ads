package com.unity3d.ads.android.burstly;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;
import com.burstly.lib.component.IBurstlyAdaptor;
import com.burstly.lib.component.IBurstlyAdaptorListener;

/**
 * Unity Ads adaptor for Burstly
 * 
 * @author tuomasrinta
 *
 */
public class UnityAdsAdaptor implements IBurstlyAdaptor, IUnityAdsListener {
	
	public static String FEATURE_PRECACHE = "precacheInterstitial";
	
	public final static String KEY_UNITY_ADS_GAME_ID ="unityads_game_id";
	public final static String KEY_TEST_MODE = "unityads_test_mode";
	public final static String KEY_CLIENT_TARGETING_PARAMS = "clientTargetingParams";
	
	private String gameId = null;
	
	private boolean campaignLoadingComplete = false;
	
	/**
	 * If we're using non-precache, the request to show ads gets fired
	 * before we know if we have campaigns or not, so we need to store
	 * the fact that we've been requested
	 */
	private boolean adShowRequested = false;
	
	/**
	 * Lifecycle status
	 */
	private boolean isLCRunning = true;

	/**
	 * The context
	 */
	private Context mContext = null;
	
	/**
	 * The UnityAds instance
	 */
	private UnityAds unityAds = null;
	
	/**
	 * Our AdaptorListener
	 */
	private IBurstlyAdaptorListener listener;
	
	/**
	 * Custom SID, if any
	 */
	private String customSid = null;

	/**
	 * Construct a new UnityAdsAdaptor
	 * @param ctx
	 */
	public UnityAdsAdaptor(Context ctx) {
		this.mContext = ctx;
	}

	@Override
	public void destroy() {
		// Tell Unity Ads to shut down
		if(this.unityAds != null) {
			this.unityAds.stopAll();
		}

	}

	@Override
	public BurstlyAdType getAdType() {
		return BurstlyAdType.INTERSTITIAL_AD_TYPE;
	}

	@Override
	public String getNetworkName() {
		return "applifierimpact";
	}
	
	private void notifyBurstlyOfAdLoading() {
		Log.d("burstly_unityads", "notifyBurstlyOfAdLoading: " + this.unityAds.canShowAds());
		if(this.unityAds.canShowAds()) {
			this.listener.didLoad(this.getNetworkName(), true);
			if(this.adShowRequested) {
				// We've been requested to show stuff
				this.showPrecachedInterstitialAd();
			}
		} else {
			this.listener.failedToLoad(this.getNetworkName(), true, "No ads available");
		}
	}
	

	@Override
	public void precacheInterstitialAd() {
		
		Log.d("burstly_unityads", "UnityAdsAdaptor.precacheInterstitialAd()");
		
		// Do nothing, as Unity Ads by default precaches interstitials
		if(this.campaignLoadingComplete) {
			this.notifyBurstlyOfAdLoading();
		}
		return;
	}


	/**
	 * Get the AdaptorListener that we notify of events
	 */
	@Override
	public void setAdaptorListener(IBurstlyAdaptorListener adaptorListener) {
		this.listener = adaptorListener;
	}

	/**
	 * Show an already cached (loaded) interstitial
	 */
	@Override
	public void showPrecachedInterstitialAd() {
		
		// If pause() or stop() have been called, do not show ads
		if(!this.isLCRunning) {
			return;
		}
		
		if(this.unityAds.canShow() && this.unityAds.canShowAds()) {
			if(this.customSid != null) {
				HashMap<String, Object> props = new HashMap<String, Object>();
				props.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, this.customSid);
				props.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, false);
				this.unityAds.show(props);
				
				
				
			} else {
				this.unityAds.show();
			}
		}
	}

	/**
	 * Initialize the adaptor
	 */
	@Override
	public void startTransaction(Map<String, ?> unityAdsParams)
			throws IllegalArgumentException {
		
		this.isLCRunning = true;
		
		for(Object k : unityAdsParams.keySet()) {
			Log.d("burstly_unityads", "startTransaction: " + k.toString() + " -> " + unityAdsParams.get(k));
		}
		
		this.gameId = (String)unityAdsParams.get(UnityAdsAdaptor.KEY_UNITY_ADS_GAME_ID);
		
		Log.d("burstly_unityads", "UnityAdsAdaptor.startTransaction(" + this.gameId + ")"); 
		
		if(gameId == null) {
			throw new IllegalArgumentException("Server must return unityads_game_id");
		}
		
	    boolean testModeEnabled = "true".equals(unityAdsParams.get(UnityAdsAdaptor.KEY_TEST_MODE));
	    UnityAds.setDebugMode(testModeEnabled); 
	    UnityAds.setTestMode(testModeEnabled);
		
		this.unityAds = new UnityAds((Activity)this.mContext, this.gameId, this);
		this.unityAds.setListener(this);
		
		// See if we have a custom user ID
		if(unityAdsParams.get(UnityAdsAdaptor.KEY_CLIENT_TARGETING_PARAMS) != null) {
			Map targetingParams = (Map)unityAdsParams.get(UnityAdsAdaptor.KEY_CLIENT_TARGETING_PARAMS);
			if(targetingParams.get("sid") != null) {
				this.customSid = targetingParams.get("sid").toString(); 
			}
		}
		
		Log.d("burstly_unityads", "Unity Ads initialized");
	}
	 

	@Override
	public View getNewAd() {

		// This gets called pretty fast, so we must delay
		// unless loading is complete
		if(this.campaignLoadingComplete) {
			this.showPrecachedInterstitialAd();
		} else {
			this.adShowRequested = true;
		}
		
		
		return null;
	}
	
	@Override
	public void pause() {
		this.isLCRunning = false;
	}
	
	@Override
	public void resume() {
		this.isLCRunning = true;
	}
	
	@Override
	public void stop() {
		this.isLCRunning = false;
	}

	@Override
	public boolean supports(String feature) {
		
		Log.d("burstly_unityads", "UnityAdsAdaptor.supports(" + feature + ")");

		if(FEATURE_PRECACHE.equals(feature)) {
			return true;
		}
		
		return false;
	}
	

	/* Lifecycle methods, not used */
	@Override
	public void endTransaction(TransactionCode tx) {}
	@Override
	public void endViewSession() {}
	@Override
	public void startViewSession() {}


	/*========================
	 * Unity Ads listener methods
	 */

	
	@Override
	public void onHide() {
		this.listener.dismissedFullscreen(
				new IBurstlyAdaptorListener.FullscreenInfo(this.getNetworkName(), true));
	} 

	@Override
	public void onShow() {
		this.listener.shownFullscreen(
				new IBurstlyAdaptorListener.FullscreenInfo(this.getNetworkName(), true));
		
	}

	@Override
	public void onFetchCompleted() {
		this.campaignLoadingComplete = true;
		Log.d("burstly_unityads","UnityAdsAdaptor.onFetchCompleted");
		this.notifyBurstlyOfAdLoading();
	}

	@Override
	public void onFetchFailed() {
		this.campaignLoadingComplete = true;
		Log.d("burstly_unityads","UnityAdsAdaptor.onFetchFailed");		
		this.notifyBurstlyOfAdLoading();
	}	

	/*
	 * No Burstly equivelants for these
	 */
	@Override
	public void onVideoStarted() {}

	@Override
	public void onVideoCompleted(String key, boolean skipped) {
		// TODO Auto-generated method stub
		
	}


	/*=====================================
	 * METHODS NOT USED AS NO BANNERS USED
	 */

	@Override
	public View precacheAd() {
		// No banners here
		return null;
	}

	
}
