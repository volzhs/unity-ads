package com.unity3d.ads.android;

import java.util.ArrayList;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsCacheManifest;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.UnityAdsWebData;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.cache.IUnityAdsWebDataListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignListener;
import com.unity3d.ads.android.video.IUnityAdsVideoListener;
import com.unity3d.ads.android.view.UnityAdsWebView;
import com.unity3d.ads.android.view.UnityAdsVideoPlayView;
import com.unity3d.ads.android.view.IUnityAdsWebViewListener;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class UnityAds implements IUnityAdsCacheListener, IUnityAdsWebDataListener, IUnityAdsWebViewListener {
	
	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManifest cachemanifest = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private Activity _currentActivity = null;
	private boolean _initialized = false;
	private boolean _showingAds = false;
	
	// Views
	private UnityAdsVideoPlayView _vp = null;
	private UnityAdsWebView _webView = null;
	
	// Listeners
	private IUnityAdsListener _adsListener = null;
	private IUnityAdsCampaignListener _campaignListener = null;
	private IUnityAdsVideoListener _videoListener = null;
	
	// Currently Selected Campaign (for viewing)
	private UnityAdsCampaign _selectedCampaign = null;	
	
	
	public UnityAds (Activity activity, String gameId) {
		instance = this;
		UnityAdsProperties.UNITY_ADS_APP_ID = gameId;
		UnityAdsProperties.ROOT_ACTIVITY = activity;
		_currentActivity = activity;
	}
		
	public void setListener (IUnityAdsListener listener) {
		_adsListener = listener;
	}
	
	public void setCampaignListener (IUnityAdsCampaignListener listener) {
		_campaignListener = listener;
	}
	
	public void setVideoListener (IUnityAdsVideoListener listener) {
		_videoListener = listener;
	}
	
	public void init () {
		if (_initialized) return; 
		
		cachemanager = new UnityAdsCacheManager();
		cachemanager.setDownloadListener(this);
		cachemanifest = new UnityAdsCacheManifest();
		webdata = new UnityAdsWebData();
		webdata.setWebDataListener(this);
		
		if (webdata.initVideoPlan(cachemanifest.getCachedCampaignIds())) {
			_initialized = true;
		}
	}
		
	public void changeActivity (Activity activity) {
		_currentActivity = activity;
		
		if (_vp != null)
			_vp.setActivity(_currentActivity);
	}
	
	public boolean show () {
		selectCampaign();
		
		if (!_showingAds && _selectedCampaign != null) {
			_currentActivity.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
			focusToView(_webView);
			_webView.loadUrl("javascript:setView('videoStart')");
			_showingAds = true;	
			
			if (_adsListener != null)
				_adsListener.onShow();
			
			return _showingAds;
		}
		
		return false;
	}
	
	// TODO: Make this a private method (requires creating a listener)
	public void closeAdsView (View view, boolean freeView) {
		view.setFocusable(false);
		view.setFocusableInTouchMode(false);
		
		ViewGroup vg = (ViewGroup)view.getParent();
		if (vg != null)
			vg.removeView(view);
		
		if (_adsListener != null && freeView) {
			_showingAds = false;
			_adsListener.onHide();
		}
	}
	
	public boolean hasCampaigns () {
		if (cachemanifest != null) {
			return cachemanifest.getCachedCampaignAmount() > 0;
		}
		
		return false;
	}
	
	public void stopAll () {
		Log.d(UnityAdsProperties.LOG_NAME, "UnityAds->stopAll()");
		UnityAdsDownloader.stopAllDownloads();
		webdata.stopAllRequests();
	}
	
	
	/* LISTENER METHODS */
	
	@Override
	public void onCampaignUpdateStarted () {	
		Log.d(UnityAdsProperties.LOG_NAME, "Campaign updates started.");
	}
	
	@Override
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;
				
		Log.d(UnityAdsProperties.LOG_NAME, "Got onCampaignReady: " + campaignHandler.getCampaign().toString());
		if (!cachemanifest.addCampaignToManifest(campaignHandler.getCampaign()))
			cachemanifest.updateCampaignInManifest(campaignHandler.getCampaign());
		
		if (_campaignListener != null && cachemanifest.getViewableCachedCampaignAmount() > 0) {
			Log.d(UnityAdsProperties.LOG_NAME, "Reporting cached campaigns available");
			_campaignListener.onFetchCompleted();
		}
	}
	
	@Override
	public void onAllCampaignsReady () {
		Log.d(UnityAdsProperties.LOG_NAME, "Listener got \"All campaigns ready.\"");
	}
	
	@Override
	public void onWebDataCompleted () {
		initCache();
	}
	
	@Override
	public void onWebDataFailed () {
		initCache();
	}
	
	@Override
	public void onCloseButtonClicked () {
		closeAdsView(_webView, true);
	}
	
	@Override
	public void onPlayVideoClicked () {
		closeAdsView(_webView, false);
		_currentActivity.addContentView(_vp, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_vp);
		
		if (_selectedCampaign != null) {
			_vp.playVideo(_selectedCampaign.getVideoFilename());
		}
		else
			Log.d(UnityAdsProperties.LOG_NAME, "Campaign is null");
			
		
		if (_videoListener != null)
			_videoListener.onVideoStarted();
	}
	
	@Override
	public void onBackButtonClicked () {
		closeAdsView(_webView, true);
	}
	
	@Override
	public void onVideoCompletedClicked () {
		closeAdsView(_webView, true);
	}
	
	
	/* PRIVATE METHODS */
	
	private void initCache () {
		if (_initialized) {
			Log.d(UnityAdsProperties.LOG_NAME, "Init cache");
			// Campaigns that are currently cached
			ArrayList<UnityAdsCampaign> cachedCampaigns = cachemanifest.getCachedCampaigns();
			// Campaigns that were received in the videoPlan
			ArrayList<UnityAdsCampaign> videoPlanCampaigns = webdata.getVideoPlanCampaigns();
			// Campaigns that were in cache but were not returned in the videoPlan (old or not current)
			ArrayList<UnityAdsCampaign> pruneList = UnityAdsUtils.substractFromCampaignList(cachedCampaigns, videoPlanCampaigns);
			
			// If campaigns from web-data is null something has probably gone wrong, try to maintain something viewable by setting
			// the videoPlan campaigns from current cache and running them through.
			if (videoPlanCampaigns == null) {
				videoPlanCampaigns = cachemanifest.getCachedCampaigns();
				pruneList = null;
			}
				
			// If current videoPlan is still null (nothing in the cache either), just forget going any further.
			if (videoPlanCampaigns == null || videoPlanCampaigns.size() == 0) return;
			
			if (cachedCampaigns != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Cached campaigns: " + cachedCampaigns.toString());
			
			if (videoPlanCampaigns != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Campaigns in videoPlan: " + videoPlanCampaigns.toString());
			
			if (pruneList != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Campaigns to prune: " + pruneList.toString());
			
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(videoPlanCampaigns, pruneList);			
			setupViews();		
		}
	}
	
	private void selectCampaign () {
		ArrayList<UnityAdsCampaign> viewableCampaigns = cachemanifest.getViewableCachedCampaigns();
		
		if (viewableCampaigns != null && viewableCampaigns.size() > 0) {
			int campaignIndex = (int)Math.round(Math.random() * (viewableCampaigns.size() - 1));
			Log.d(UnityAdsProperties.LOG_NAME, "Selected campaign " + (campaignIndex + 1) + ", out of " + viewableCampaigns.size());
			_selectedCampaign = viewableCampaigns.get(campaignIndex);		
		}
	}

	private void focusToView (View view) {
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
	}
	
	private void setupViews () {
		_webView = new UnityAdsWebView(_currentActivity, null, this);
	
		_vp = new UnityAdsVideoPlayView(_currentActivity.getBaseContext(), new MediaPlayer.OnCompletionListener() {			
			@Override
			public void onCompletion(MediaPlayer mp) {				
				if (_videoListener != null)
					_videoListener.onVideoCompleted();
				
				_selectedCampaign.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
				cachemanifest.writeCurrentCacheManifest();
				webdata.sendCampaignViewed(_selectedCampaign);
				_vp.setKeepScreenOn(false);
				closeAdsView(_vp, false);
				
				_webView.loadUrl("javascript:setView('videoCompleted')");
				_currentActivity.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
				focusToView(_webView);
			}
		}, _currentActivity);	
	}
}
