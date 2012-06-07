package com.unity3d.ads.android;

import java.util.ArrayList;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsCacheManifest;
import com.unity3d.ads.android.cache.UnityAdsWebData;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.video.IUnityAdsVideoListener;
import com.unity3d.ads.android.view.UnityAdsVideoCompletedView;
import com.unity3d.ads.android.view.UnityAdsVideoPlayView;
import com.unity3d.ads.android.view.UnityAdsVideoSelectView;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class UnityAds {
	
	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManifest cachemanifest = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private Activity _currentActivity = null;
	
	// Views
	private UnityAdsVideoSelectView _vs = null;
	private UnityAdsVideoPlayView _vp = null;
	private UnityAdsVideoCompletedView _vc = null;
	
	// Listeners
	private IUnityAdsListener _adsListener = null;
	private IUnityAdsCacheListener _cacheListener = null;
	private IUnityAdsVideoListener _videoListener = null;
	
	private boolean _initialized = false;
	
	public UnityAds (Activity activity, String gameId) {
		instance = this;
		UnityAdsProperties.UNITY_ADS_APP_ID = gameId;
		UnityAdsProperties.ROOT_ACTIVITY = activity;
		_currentActivity = activity;
	}
		
	public void setListener (IUnityAdsListener listener) {
		_adsListener = listener;
	}
	
	public void setCacheListener (IUnityAdsCacheListener listener) {
		_cacheListener = listener;
	}
	
	public void setVideoListener (IUnityAdsVideoListener listener) {
		_videoListener = listener;
	}
	
	public void init () {
		if (_initialized) return; 
		
		cachemanager = new UnityAdsCacheManager();
		cachemanifest = new UnityAdsCacheManifest();
		webdata = new UnityAdsWebData();
		
		if (webdata.initVideoPlan(cachemanifest.getCachedCampaignIds())) {
			ArrayList<UnityAdsCampaign> cachedCampaigns = cachemanifest.getCachedCampaigns();
			ArrayList<UnityAdsCampaign> videoPlanCampaigns = webdata.getVideoPlanCampaigns();
			ArrayList<UnityAdsCampaign> pruneList = UnityAdsUtils.createPruneList(cachedCampaigns, videoPlanCampaigns);
			
			if (cachedCampaigns != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Cached campaigns: " + cachedCampaigns.toString());
			
			if (videoPlanCampaigns != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Campaigns in videoPlan: " + videoPlanCampaigns.toString());
			
			if (pruneList != null)
				Log.d(UnityAdsProperties.LOG_NAME, "Campaigns to prune: " + pruneList.toString());
			
			cachemanager.updateCache(videoPlanCampaigns, pruneList);
			cachemanifest.setCachedCampaigns(webdata.getVideoPlanCampaigns());
		}
		
		
		/*
		cachemanager = new UnityAdsCacheManager();
		cachemanager.setCacheListener(new IUnityAdsCacheListener() {			
			@Override
			public void onCachedCampaignsAvailable() {
				if (_cacheListener != null)
					_cacheListener.onCachedCampaignsAvailable();
			}
		});
		cachemanifest = new UnityAdsCacheManifest(cachemanager.getCacheDir());
		_webdata = new UnityAdsWebData();
		
		if (_webdata.initVideoPlan(cachemanifest.getCacheManifest())) {
			cachemanager.initCache(cachemanifest.getCacheManifest(), _webdata.getVideoPlan());
		}
		
		ArrayList<UnityAdsCampaign> mergedCampaigns = mergeCampaignLists(createCampaignsFromJson(_webdata.getVideoPlan()), createCampaignsFromJson(cachemanifest.getCacheManifest()));
		
		if (mergedCampaigns != null)
			Log.d(UnityAdsProperties.LOG_NAME, mergedCampaigns.toString());
		else
			Log.d(UnityAdsProperties.LOG_NAME, "Jenkem");
		
		setupViews();
		*/
		
		_initialized = true;
	}
		
	public void changeActivity (Activity activity) {
		_currentActivity = activity;
	}
	
	public boolean show () {
		/*
		_CurrentAd = new ArrayList<JSONObject>();
		ArrayList<String> _cachedCampaigns = cachemanifest.getCachedCampaignIds();
		
		for (String id : _cachedCampaigns) {
			_CurrentAd.add(cachemanifest.getCampaign(id));
			
			if (_CurrentAd.size() > 2)
				break;
		}
		
		
		if (_CurrentAd.size() < 3) {
			int left = 3 - _CurrentAd.size();
			JSONObject plan = _webdata.getVideoPlan();
			JSONArray va = null;
			
			try {
				va = plan.getJSONArray("va");
			}
			catch (Exception e) {
				return false;
			}
			
			for (int i = 0; i < left; i++) {
				try {
					_CurrentAd.add(va.getJSONObject(i));
				}
				catch (Exception e) {
					return false;
				}
			}
		}
				
		Log.d(UnityAdsProperties.LOG_NAME, _CurrentAd.toString());
		
		_currentActivity.addContentView(_vs, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_vs);
		
		if (_adsListener != null)
			_adsListener.onShow();
		*/
		return false;
	}
	
	public void closeAdsView (View view, boolean reportClosed) {
		view.setFocusable(false);
		view.setFocusableInTouchMode(false);
		
		ViewGroup vg = (ViewGroup)view.getParent();
		if (vg != null)
			vg.removeView(view);
		
		if (_adsListener != null && reportClosed)
			_adsListener.onHide();
	}
	
	public boolean hasCampaigns () {
		if (webdata != null && cachemanifest != null) {
			if (webdata.getCampaignAmount() + cachemanifest.getCachedCampaignAmount() > 2)
				return true;
		}
		
		return false;
	}
	
	
	/* PRIVATE METHODS */
	

	private void focusToView (View view) {
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
	}
	
	private void setupViews () {
		_vc = new UnityAdsVideoCompletedView(_currentActivity.getBaseContext());
		_vc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeAdsView(_vc, true);
			}
		});
		
		_vs = new UnityAdsVideoSelectView(_currentActivity.getBaseContext());		
		_vs.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeAdsView(_vs, false);
				_currentActivity.addContentView(_vp, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
				focusToView(_vp);
				_vp.playVideo();
				
				if (_videoListener != null)
					_videoListener.onVideoStarted();
			}
		});
		
		_vp = new UnityAdsVideoPlayView(_currentActivity.getBaseContext(), new MediaPlayer.OnCompletionListener() {			
			@Override
			public void onCompletion(MediaPlayer mp) {				
				if (_videoListener != null)
					_videoListener.onVideoCompleted();
				
				closeAdsView(_vp, false);
				_currentActivity.addContentView(_vc, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
				focusToView(_vc);
			}
		});
		
		_vp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});		
	}
}
