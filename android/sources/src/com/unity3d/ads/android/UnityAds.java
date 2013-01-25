package com.unity3d.ads.android;

import org.json.JSONObject;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.video.UnityAdsVideoPlayView;
import com.unity3d.ads.android.video.IUnityAdsVideoListener;
import com.unity3d.ads.android.video.IUnityAdsVideoPlayerListener;
import com.unity3d.ads.android.view.UnityAdsMainView;
import com.unity3d.ads.android.view.IUnityAdsMainViewListener;
import com.unity3d.ads.android.view.IUnityAdsViewListener;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewAction;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewState;
import com.unity3d.ads.android.webapp.*;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class UnityAds implements IUnityAdsCacheListener, 
										IUnityAdsWebDataListener, 
										IUnityAdsWebBrigeListener,
										IUnityAdsMainViewListener {
	
	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private boolean _initialized = false;
	private boolean _showingAds = false;
	private boolean _adsReadySent = false;
	private boolean _webAppLoaded = false;
		
	// Main View
	private UnityAdsMainView _mainView = null;
	
	// Listeners
	private IUnityAdsListener _adsListener = null;
	private IUnityAdsCampaignListener _campaignListener = null;
	private IUnityAdsVideoListener _videoListener = null;
	
	
	public UnityAds (Activity activity, String gameId) {
		instance = this;
		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
		UnityAdsProperties.BASE_ACTIVITY = activity;
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
		webdata = new UnityAdsWebData();
		webdata.setWebDataListener(this);

		if (webdata.initCampaigns()) {
			_initialized = true;
		}
	}
		
	public void changeActivity (Activity activity) {
		if (activity == null) return;
		
		if (!activity.equals(UnityAdsProperties.CURRENT_ACTIVITY)) {
			UnityAdsProperties.CURRENT_ACTIVITY = activity;
			
			// Not the most pretty way to detect when the fullscreen activity is ready
			if (activity.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				open();
			}
		}
	}
	
	public boolean closeAds () {
		if (_showingAds) {
			close();
			return true;
		}
		
		return false;
	}
	
	public boolean show () {
		if (!_showingAds && canShowAds()) {
			Intent newIntent = new Intent(UnityAdsProperties.CURRENT_ACTIVITY, com.unity3d.ads.android.view.UnityAdsFullscreenActivity.class);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK);
			UnityAdsProperties.CURRENT_ACTIVITY.startActivity(newIntent);
			_showingAds = true;	
			return _showingAds;
		}

		return false;
	}
		
	public boolean hasCampaigns () {
		if (webdata != null && canShowAds()) {
			return webdata.getViewableVideoPlanCampaigns().size() > 0;
		}
		
		return false;
	}
	
	public void stopAll () {
		UnityAdsUtils.Log("stopAll()", this);
		UnityAdsDownloader.stopAllDownloads();
		webdata.stopAllRequests();
	}
	
	
	/* LISTENER METHODS */
	
	// IUnityAdsMainViewListener
	public void onMainViewAction (UnityAdsMainViewAction action) {
		switch (action) {
			case BackButtonPressed:
				close();
				break;
			case VideoStart:
				if (_videoListener != null)
					_videoListener.onVideoStarted();
				break;
			case VideoEnd:
				if (_videoListener != null)
					_videoListener.onVideoCompleted();
				break;
		}
	}
	
	
	// IUnityAdsCacheListener
	@Override
	public void onCampaignUpdateStarted () {	
		UnityAdsUtils.Log("Campaign updates started.", this);
	}
	
	@Override
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;
				
		UnityAdsUtils.Log("Got onCampaignReady: " + campaignHandler.getCampaign().toString(), this);
		
		if (canShowAds())
			sendAdsReadyEvent();
	}
	
	@Override
	public void onAllCampaignsReady () {
		UnityAdsUtils.Log("Listener got \"All campaigns ready.\"", this);
	}
	
	// IUnityAdsWebDataListener
	@Override
	public void onWebDataCompleted () {
		setup();
	}
	
	@Override
	public void onWebDataFailed () {
	}
	
	
	// IUnityAdsWebBrigeListener
	@Override
	public void onPlayVideo(JSONObject data) {
		if (data.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY)) {
			String campaignId = null;
			
			try {
				campaignId = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get campaignId", this);
			}
			
			if (campaignId != null) {
				UnityAdsProperties.SELECTED_CAMPAIGN = webdata.getCampaignById(campaignId);
				Boolean rewatch = false;
				
				try {
					rewatch = data.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_REWATCH_KEY);
				}
				catch (Exception e) {
				}
				
				if (UnityAdsProperties.SELECTED_CAMPAIGN != null && (rewatch || !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
					UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
					UnityAdsUtils.Log("Running threaded", this);
					UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(playVideoRunner);
				}
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
	}

	@Override
	public void onCloseAdsView(JSONObject data) {
		closeAds();
	}
	
	@Override
	public void onWebAppInitComplete (JSONObject data) {
		UnityAdsUtils.Log("WebApp init complete", this);
		_webAppLoaded = true;
		Boolean dataOk = true;
		
		if (canShowAds()) {
			JSONObject setViewData = new JSONObject();
			
			try {
				setViewData.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_INITCOMPLETE);
				setViewData.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
			}
			catch (Exception e) {
				dataOk = false;
			}
			
			if (dataOk) {
				_mainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, setViewData);
				sendAdsReadyEvent();			
			}
		}
	}
	

	/* PRIVATE METHODS */
	
	private void close () {
		UnityAdsCloseRunner closeRunner = new UnityAdsCloseRunner();
		UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(closeRunner);
	}
	
	private void open () {
		Boolean dataOk = true;			
		JSONObject data = new JSONObject();
		
		UnityAdsUtils.Log("dataOk: " + dataOk, this);
		
		try  {
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_OPEN);
			data.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
		}
		catch (Exception e) {
			dataOk = false;
		}

		if (dataOk) {
			_mainView.openAds(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, data);
		}
	}

	private void setup () {
		initCache();
		setupViews();
	}
	
	private void initCache () {
		if (_initialized) {
			UnityAdsUtils.Log("Init cache", this);
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(webdata.getVideoPlanCampaigns());				
		}
	}
	
	private boolean canShowAds () {
		return _mainView != null && _mainView.webview != null && _mainView.webview.isWebAppLoaded() && _webAppLoaded && webdata.getViewableVideoPlanCampaigns().size() > 0;
	}
	
	private void sendAdsReadyEvent () {
		if (!_adsReadySent && _campaignListener != null) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					UnityAdsUtils.Log("Unity Ads ready!", this);
					_adsReadySent = true;
					_campaignListener.onFetchCompleted();
				}
			});
		}
	}

	private void setupViews () {
		_mainView = new UnityAdsMainView(UnityAdsProperties.CURRENT_ACTIVITY, this);
	}

	
	/* INTERNAL CLASSES */

	// FIX: Could these 2 classes be moved to MainView
	
	private class UnityAdsCloseRunner implements Runnable {
		@Override
		public void run() {
			_showingAds = false;
			if (UnityAdsProperties.CURRENT_ACTIVITY.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				Boolean dataOk = true;			
				JSONObject data = new JSONObject();
				
				UnityAdsUtils.Log("dataOk: " + dataOk, this);
				
				try  {
					data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_CLOSE);
				}
				catch (Exception e) {
					dataOk = false;
				}

				if (dataOk) {
					_mainView.closeAds(data);
					UnityAdsProperties.CURRENT_ACTIVITY.finish();
				}
			}
		}
	}
	
	private class UnityAdsPlayVideoRunner implements Runnable {
		@Override
		public void run() {			
			if (UnityAdsProperties.SELECTED_CAMPAIGN != null) {
				String playUrl = UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename();
				if (!UnityAdsUtils.isFileInCache(UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename()))
					playUrl = UnityAdsProperties.SELECTED_CAMPAIGN.getVideoStreamUrl(); 

				_mainView.setViewState(UnityAdsMainViewState.VideoPlayer);
				_mainView.videoplayerview.playVideo(playUrl);
			}			
			else
				UnityAdsUtils.Log("Campaign is null", this);
		}		
	}
}
