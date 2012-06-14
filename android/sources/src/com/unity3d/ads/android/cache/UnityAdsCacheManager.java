package com.unity3d.ads.android.cache;

import java.util.ArrayList;

import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignHandlerListener;

import android.util.Log;

public class UnityAdsCacheManager implements IUnityAdsCampaignHandlerListener {
	private IUnityAdsCacheListener _downloadListener = null;	
	private ArrayList<UnityAdsCampaign> _downloadingCampaigns = null;
	private ArrayList<UnityAdsCampaignHandler> _downloadingHandlers = null;
	
	public UnityAdsCacheManager () {
		UnityAdsUtils.createCacheDir();
		Log.d(UnityAdsProperties.LOG_NAME, "External storagedir: " + UnityAdsUtils.getCacheDirectory());
	}
	
	public ArrayList<UnityAdsCampaign> getDownloadingCampaigns () {
		return _downloadingCampaigns;
	}
	
	public void setDownloadListener (IUnityAdsCacheListener listener) {
		_downloadListener = listener;
	}
	
	public boolean hasDownloadingHandlers () {
		return (_downloadingHandlers != null && _downloadingHandlers.size() > 0);
	}
	
	public void initCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		updateCache(activeList, pruneList);
	}
		
	public void updateCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		if (_downloadListener != null)
			_downloadListener.onCampaignUpdateStarted();
		
		// Active -list contains campaigns that came with the videoPlan
		if (activeList != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Going through active campaigns");			
			for (UnityAdsCampaign campaign : activeList) {
				UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign, activeList);
				
				if (campaignHandler.hasDownloads()) {
					campaignHandler.setListener(this);
					addToDownloadingHandlers(campaignHandler);
				}
			}
		}
		
		// Prune -list contains campaigns that were still in cache but not in the received videoPlan.
		// There for they will not be put into cache. Check that the existing videos for those
		// campaigns are not needed by current active ones and remove them if needed.
		if (pruneList != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Pruning old campaigns");
			for (UnityAdsCampaign campaign : pruneList) {
				if (!UnityAdsUtils.isFileRequiredByCampaigns(campaign.getVideoUrl(), activeList)) {
					UnityAdsUtils.removeFile(campaign.getVideoUrl());
				}
			}
		}
		
		if (!hasDownloadingHandlers() && _downloadListener != null)
			_downloadListener.onAllCampaignsReady();
	}

	
	// EVENT METHDOS
	
	@Override
	public void onCampaignHandled(UnityAdsCampaignHandler campaignHandler) {
		removeFromDownloadingHandlers(campaignHandler);
		_downloadListener.onCampaignReady(campaignHandler);
		
		if (!hasDownloadingHandlers() && _downloadListener != null)
			_downloadListener.onAllCampaignsReady();
	}	
	
	
	// INTERNAL METHODS
	
	private void removeFromDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers != null)
			_downloadingHandlers.remove(campaignHandler);
		
		if (_downloadingCampaigns != null)
			_downloadingCampaigns.remove(campaignHandler.getCampaign());
	}
	
	private void addToDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers == null)
			_downloadingHandlers = new ArrayList<UnityAdsCampaignHandler>();
		
		_downloadingHandlers.add(campaignHandler);
		
		if (_downloadingCampaigns == null)
			_downloadingCampaigns = new ArrayList<UnityAdsCampaign>();
		
		_downloadingCampaigns.add(campaignHandler.getCampaign());
	}
}
