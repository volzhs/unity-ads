package com.unity3d.ads.android;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsCacheManifest;
import com.unity3d.ads.android.cache.UnityAdsWebData;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
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
	
	// Temporary data
	private UnityAdsWebData _webdata = null;
	private ArrayList<JSONObject> _CurrentAd = null;
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
		_initialized = true;
	}
		
	public void changeActivity (Activity activity) {
		_currentActivity = activity;
	}
	
	public boolean show () {
		_CurrentAd = new ArrayList<JSONObject>();
		ArrayList<String> _cachedCampaigns = cachemanifest.getCachedCampaignIds();
		
		for (String id : _cachedCampaigns) {
			_CurrentAd.add(cachemanifest.getCampaign(id));
			
			if (_CurrentAd.size() > 2)
				break;
		}
		
		/*
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
		}*/
				
		Log.d(UnityAdsProperties.LOG_NAME, _CurrentAd.toString());
		
		_currentActivity.addContentView(_vs, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		focusToView(_vs);
		
		if (_adsListener != null)
			_adsListener.onShow();
		
		return true;
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
		if (_webdata != null && cachemanifest != null) {
			if (_webdata.getCampaignAmount() + cachemanifest.getCampaignAmount() > 2)
				return true;
		}
		
		return false;
	}
	
	
	/* PRIVATE METHODS */
	
	private ArrayList<UnityAdsCampaign> createCampaignsFromJson (JSONObject json) {
		if (json != null && json.has("va")) {
			ArrayList<UnityAdsCampaign> campaignData = new ArrayList<UnityAdsCampaign>();
			JSONArray va = null;
			JSONObject currentCampaign = null;
			
			try {
				va = json.getJSONArray("va");
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON");
			}
			
			for (int i = 0; i < va.length(); i++) {
				try {
					currentCampaign = va.getJSONObject(i);
					campaignData.add(new UnityAdsCampaign(currentCampaign));
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON");
				}				
			}
			
			return campaignData;
		}
		
		return null;
	}
	
	private ArrayList<UnityAdsCampaign> mergeCampaignLists (ArrayList<UnityAdsCampaign> list1, ArrayList<UnityAdsCampaign> list2) {
		ArrayList<UnityAdsCampaign> mergedData = new ArrayList<UnityAdsCampaign>();
		
		if (list1 == null || list1.size() == 0) return list2;
		if (list2 == null || list2.size() == 0) return list1;
		
		if (list1 != null && list2 != null) {
			mergedData.addAll(list1);
			for (UnityAdsCampaign list1Campaign : list1) {
				UnityAdsCampaign inputCampaign = null;
				boolean match = false;
				for (UnityAdsCampaign list2Campaign : list2) {
					inputCampaign = list2Campaign;
					if (list1Campaign.getCampaignId().equals(list2Campaign.getCampaignId())) {
						match = true;
						break;
					}
				}
				
				if (!match)
					mergedData.add(inputCampaign);
			}
			
			return mergedData;
		}
		
		return null;
	}
	
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
