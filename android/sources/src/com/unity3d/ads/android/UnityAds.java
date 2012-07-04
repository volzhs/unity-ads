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
import com.unity3d.ads.android.view.IUnityAdsVideoPlayerListener;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class UnityAds implements IUnityAdsCacheListener, IUnityAdsWebDataListener, IUnityAdsWebViewListener, IUnityAdsVideoPlayerListener {
	
	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManifest cachemanifest = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private boolean _initialized = false;
	private boolean _showingAds = false;
	private boolean _adsReadySent = false;
	
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
		UnityAdsProperties.CURRENT_ACTIVITY = activity;
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
		if (activity == null) return;
		
		UnityAdsProperties.CURRENT_ACTIVITY = activity;
		
		if (_vp != null)
			_vp.setActivity(UnityAdsProperties.CURRENT_ACTIVITY);
	}
	
	public boolean show () {
		selectCampaign();
		
		if (!_showingAds && _selectedCampaign != null && _webView != null && _webView.isWebAppLoaded()) {
			UnityAdsProperties.CURRENT_ACTIVITY.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
			focusToView(_webView);
			_webView.setView("videoStart");
			_webView.setSelectedCampaign(_selectedCampaign);
			_showingAds = true;	
			
			if (_adsListener != null)
				_adsListener.onShow();
			
			return _showingAds;
		}
		
		return false;
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
		
		if (canShowAds())
			sendAdsReadyEvent();
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
	public void onWebAppLoaded () {
		ArrayList<UnityAdsCampaign> campaignList = solveCurrentCampaigns();
		
		if (campaignList != null)
			_webView.setAvailableCampaigns(UnityAdsUtils.createJsonFromCampaigns(campaignList).toString());
		
		if (canShowAds())
			sendAdsReadyEvent();
	}
	
	@Override
	public void onCloseButtonClicked (View view) {
		closeView(view, true);
	}
	
	@Override
	public void onBackButtonClicked (View view) {
		closeView(view, true);
	}
	
	@Override
	public void onPlayVideoClicked () {
		closeView(_webView, false);
		UnityAdsProperties.CURRENT_ACTIVITY.addContentView(_vp, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_vp);
		
		if (_selectedCampaign != null) {
			String playUrl = UnityAdsUtils.getCacheDirectory() + "/" + _selectedCampaign.getVideoFilename();
			if (!UnityAdsUtils.isFileInCache(_selectedCampaign.getVideoFilename()))
				playUrl = _selectedCampaign.getVideoStreamUrl(); 

			_vp.playVideo(playUrl);
		}			
		else
			Log.d(UnityAdsProperties.LOG_NAME, "Campaign is null");
					
		if (_videoListener != null)
			_videoListener.onVideoStarted();
	}
		
	@Override
	public void onVideoCompletedClicked () {
		closeView(_webView, true);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {				
		if (_videoListener != null)
			_videoListener.onVideoCompleted();
		
		_selectedCampaign.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
		cachemanifest.writeCurrentCacheManifest();
		webdata.sendCampaignViewed(_selectedCampaign);
		_vp.setKeepScreenOn(false);
		closeView(_vp, false);
		
		_webView.setView("videoCompleted");
		UnityAdsProperties.CURRENT_ACTIVITY.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_webView);
		_selectedCampaign = null;
	}
	
	/* PRIVATE METHODS */
	
	private void initCache () {
		if (_initialized) {
			Log.d(UnityAdsProperties.LOG_NAME, "Init cache");
			// Campaigns that were received in the videoPlan
			ArrayList<UnityAdsCampaign> videoPlanCampaigns = solveCurrentCampaigns();
			// Campaigns that were in cache but were not returned in the videoPlan (old or not current)
			ArrayList<UnityAdsCampaign> pruneList = solvePruneList();
				
			// If current videoPlan is null (nothing in the cache either), just forget going any further.
			if (videoPlanCampaigns == null || videoPlanCampaigns.size() == 0) return;
			
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(videoPlanCampaigns, pruneList);			
			setupViews();		
		}
	}
	
	private ArrayList<UnityAdsCampaign> solveCurrentCampaigns () {
		ArrayList<UnityAdsCampaign> campaigns = webdata.getVideoPlanCampaigns();
		if (campaigns == null)
			campaigns = cachemanifest.getCachedCampaigns();
		
		return campaigns;
	}
	
	private ArrayList<UnityAdsCampaign> solvePruneList () {
		if (webdata.getVideoPlanCampaigns() == null) return null;		
		return UnityAdsUtils.substractFromCampaignList(cachemanifest.getCachedCampaigns(), webdata.getVideoPlanCampaigns());		
	}
	
	private boolean canShowAds () {
		return _webView != null && _webView.isWebAppLoaded() && cachemanifest.getViewableCachedCampaignAmount() > 0;
	}
	
	private void sendAdsReadyEvent () {
		if (!_adsReadySent && _campaignListener != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Unity Ads ready!");
			_adsReadySent = true;
			_campaignListener.onFetchCompleted();
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

	private void closeView (View view, boolean freeView) {
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
	
	private void focusToView (View view) {
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
	}
	
	private void setupViews () {
		_webView = new UnityAdsWebView(UnityAdsProperties.CURRENT_ACTIVITY, this);	
		_vp = new UnityAdsVideoPlayView(UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext(), this, UnityAdsProperties.CURRENT_ACTIVITY);	
	}
}
