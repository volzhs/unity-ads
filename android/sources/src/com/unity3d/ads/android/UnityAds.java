package com.unity3d.ads.android;

import org.json.JSONObject;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
//import com.unity3d.ads.android.cache.UnityAdsCacheManifest;
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
import com.unity3d.ads.android.view.IUnityAdsViewListener;
import com.unity3d.ads.android.webapp.*;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class UnityAds implements IUnityAdsCacheListener, 
										IUnityAdsWebDataListener, 
										IUnityAdsWebViewListener, 
										IUnityAdsVideoPlayerListener,
										IUnityAdsWebBrigeListener,
										IUnityAdsViewListener {
	
	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private boolean _initialized = false;
	private boolean _showingAds = false;
	private boolean _adsReadySent = false;
	private boolean _webAppLoaded = false;
	
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
		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
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

		if (webdata.initVideoPlan()) {
			_initialized = true;
		}
	}
		
	public void changeActivity (Activity activity) {
		if (activity == null) return;
		UnityAdsProperties.CURRENT_ACTIVITY = activity;
	}
	
	public boolean show () {
		if (!_showingAds && canShowAds()) {
			UnityAdsProperties.CURRENT_ACTIVITY.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
			focusToView(_webView);
			_webView.setView("start");
			_showingAds = true;	
			
			if (_adsListener != null)
				_adsListener.onShow();
			
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
		Log.d(UnityAdsConstants.LOG_NAME, "UnityAds->stopAll()");
		UnityAdsDownloader.stopAllDownloads();
		webdata.stopAllRequests();
	}
	
	
	/* LISTENER METHODS */
	
	// IUnityAdsCacheListener
	@Override
	public void onCampaignUpdateStarted () {	
		Log.d(UnityAdsConstants.LOG_NAME, "Campaign updates started.");
	}
	
	@Override
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;
				
		Log.d(UnityAdsConstants.LOG_NAME, "Got onCampaignReady: " + campaignHandler.getCampaign().toString());
		
		if (canShowAds())
			sendAdsReadyEvent();
	}
	
	@Override
	public void onAllCampaignsReady () {
		Log.d(UnityAdsConstants.LOG_NAME, "Listener got \"All campaigns ready.\"");
	}
	
	// IUnityAdsWebDataListener
	@Override
	public void onWebDataCompleted () {
		setup();
	}
	
	@Override
	public void onWebDataFailed () {
	}
	
	// IUnityAdsWebViewListener
	@Override
	public void onWebAppLoaded () {
		_webView.initWebApp(webdata.getData());
	}
	
	// IUnityAdsViewListener
	@Override
	public void onCloseButtonClicked (View view) {
		closeView(view, true);
	}
	
	@Override
	public void onBackButtonClicked (View view) {
		closeView(view, true);
	}
	
	// IUnityAdsWebBrigeListener
	@Override
	public void onPlayVideo(JSONObject data) {
		if (data.has("campaignId")) {
			String campaignId = null;
			
			try {
				campaignId = data.getString("campaignId");
			}
			catch (Exception e) {
				Log.d(UnityAdsConstants.LOG_NAME, "Could not get campaignId");
			}
			
			if (campaignId != null) {
				_selectedCampaign = webdata.getCampaignById(campaignId);
				
				if (_selectedCampaign != null)
					UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new UnityAdsPlayVideoRunner());
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
		if (_vp != null)
			_vp.pauseVideo();
	}

	@Override
	public void onCloseView(JSONObject data) {
		UnityAdsCloseViewRunner closeViewRunner = new UnityAdsCloseViewRunner(_webView, true);
		UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(closeViewRunner);
	}
	
	@Override
	public void onWebAppInitComplete (JSONObject data) {
		Log.d(UnityAdsConstants.LOG_NAME, "WebAppInitComplete");
		_webAppLoaded = true;

		if (canShowAds())
			sendAdsReadyEvent();			
	}
	
	// IUnityAdsVideoPlayerListener
	@Override
	public void onEventPositionReached (UnityAdsVideoPosition position) {
		if (_selectedCampaign != null && !_selectedCampaign.getCampaignStatus().equals(UnityAdsCampaignStatus.VIEWED))
			webdata.sendCampaignViewProgress(_selectedCampaign, position);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {				
		if (_videoListener != null)
			_videoListener.onVideoCompleted();
				
		_vp.setKeepScreenOn(false);
		closeView(_vp, false);
		JSONObject params = null;
		
		try {
			params = new JSONObject("{\"campaignId\":\"" + _selectedCampaign.getCampaignId() + "\"}");
		}
		catch (Exception e) {
			Log.d(UnityAdsConstants.LOG_NAME, "Could not create JSON");
		}
		
		_webView.setView("completed", params);
		UnityAdsProperties.CURRENT_ACTIVITY.addContentView(_webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_webView);
		onEventPositionReached(UnityAdsVideoPosition.End);
		_selectedCampaign.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
		_selectedCampaign = null;
	}
	
	
	/* PRIVATE METHODS */
	
	private void setup () {
		initCache();
		setupViews();
	}
	
	private void initCache () {
		if (_initialized) {
			Log.d(UnityAdsConstants.LOG_NAME, "Init cache");
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(webdata.getVideoPlanCampaigns());				
		}
	}
	
	private boolean canShowAds () {
		return _webView != null && _webView.isWebAppLoaded() && _webAppLoaded && webdata.getViewableVideoPlanCampaigns().size() > 0;
	}
	
	private void sendAdsReadyEvent () {
		if (!_adsReadySent && _campaignListener != null) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					Log.d(UnityAdsConstants.LOG_NAME, "Unity Ads ready!");
					_adsReadySent = true;
					_campaignListener.onFetchCompleted();
				}
			});
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
		_webView = new UnityAdsWebView(UnityAdsProperties.CURRENT_ACTIVITY, this, new UnityAdsWebBridge(this));	
		_vp = new UnityAdsVideoPlayView(UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext(), this);	
	}
	
	
	/* INTERNAL CLASSES */
	
	private class UnityAdsCloseViewRunner implements Runnable {
		private View _view = null;
		private boolean _freeView = false;
		
		public UnityAdsCloseViewRunner (View view, boolean freeView) {
			_view = view;
			_freeView = freeView;
		}
		
		@Override
		public void run() {
			closeView(_view, _freeView);
		}
	}
	
	private class UnityAdsPlayVideoRunner implements Runnable {
		@Override
		public void run() {
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
				Log.d(UnityAdsConstants.LOG_NAME, "Campaign is null");
						
			if (_videoListener != null)
				_videoListener.onVideoStarted();
		}		
	}
}
