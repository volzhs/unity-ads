package com.unity3d.ads.android.cache;

import java.io.File;
import java.util.ArrayList;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignHandlerListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;

import android.util.Log;

public class UnityAdsCacheManager implements IUnityAdsCampaignHandlerListener {
	
	private IUnityAdsCacheListener _downloadListener = null;	
	private ArrayList<UnityAdsCampaignHandler> _downloadingHandlers = null;
	private ArrayList<UnityAdsCampaignHandler> _handlers = null;	
	private int _amountPrepared = 0;
	private int _totalCampaigns = 0;
	
	
	public UnityAdsCacheManager () {
		UnityAdsUtils.createCacheDir();
		UnityAdsUtils.Log("External storagedir: " + UnityAdsUtils.getCacheDirectory(), this);
	}
	
	public void setDownloadListener (IUnityAdsCacheListener listener) {
		_downloadListener = listener;
	}
	
	public boolean hasDownloadingHandlers () {
		return (_downloadingHandlers != null && _downloadingHandlers.size() > 0);
	}
	
	public void initCache (ArrayList<UnityAdsCampaign> activeList) {
		updateCache(activeList);
	}
		
	public void updateCache (ArrayList<UnityAdsCampaign> activeList) {
		if (_downloadListener != null)
			_downloadListener.onCampaignUpdateStarted();
		
		_amountPrepared = 0;
		
		UnityAdsUtils.Log(activeList.toString(), this);
		
		// Check cache directory and delete all files that don't match the current files in campaigns
		if (UnityAdsUtils.getCacheDirectory() != null) {
			File dir = new File(UnityAdsUtils.getCacheDirectory());
			File[] fileList = dir.listFiles();
			
			if (fileList != null) {
				for (File currentFile : fileList) {
					UnityAdsUtils.Log("Checking file: " + currentFile.getName(), this);
					if (!currentFile.getName().equals(UnityAdsConstants.PENDING_REQUESTS_FILENAME) && 
						!currentFile.getName().equals(UnityAdsConstants.CACHE_MANIFEST_FILENAME) && 
						!UnityAdsUtils.isFileRequiredByCampaigns(currentFile.getName(), activeList)) {
						UnityAdsUtils.removeFile(currentFile.getName());
					}
				}
			}
		}

		// Active -list contains campaigns that came with the videoPlan
		if (activeList != null) {
			_totalCampaigns = activeList.size();
			UnityAdsUtils.Log("Updating cache: Going through active campaigns", this);			
			for (UnityAdsCampaign campaign : activeList) {
				UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign);
				addToUpdatingHandlers(campaignHandler);
				campaignHandler.setListener(this);
				campaignHandler.initCampaign();
				
				if (campaignHandler.hasDownloads()) {
					addToDownloadingHandlers(campaignHandler);
				}					
			}
		}
	}

	
	// EVENT METHDOS
	
	@Override
	public void onCampaignHandled(UnityAdsCampaignHandler campaignHandler) {
		_amountPrepared++;
		removeFromDownloadingHandlers(campaignHandler);
		removeFromUpdatingHandlers(campaignHandler);
		_downloadListener.onCampaignReady(campaignHandler);
		
		if (_amountPrepared == _totalCampaigns)
			_downloadListener.onAllCampaignsReady();
	}	
	
	
	// INTERNAL METHODS
	
	private void removeFromUpdatingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_handlers != null)
			_handlers.remove(campaignHandler);
	}
	
	private void addToUpdatingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_handlers == null)
			_handlers = new ArrayList<UnityAdsCampaignHandler>();
		
		_handlers.add(campaignHandler);
	}
	
	private void removeFromDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers != null)
			_downloadingHandlers.remove(campaignHandler);
	}
	
	private void addToDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers == null)
			_downloadingHandlers = new ArrayList<UnityAdsCampaignHandler>();
		
		_downloadingHandlers.add(campaignHandler);
	}
}
