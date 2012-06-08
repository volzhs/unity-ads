package com.unity3d.ads.android.cache;

import java.util.ArrayList;

import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignHandlerListener;

import android.util.Log;

public class UnityAdsCacheManager {
	private IUnityAdsDownloadListener _downloadListener = null;	
	private ArrayList<UnityAdsCampaign> _downloadingCampaigns = null;
	private ArrayList<UnityAdsCampaignHandler> _downloadingHandlers = null;
	
	public UnityAdsCacheManager () {
		UnityAdsUtils.createCacheDir();
		Log.d(UnityAdsProperties.LOG_NAME, "External storagedir: " + UnityAdsUtils.getCacheDirectory());
	}
	
	public ArrayList<UnityAdsCampaign> getDownloadingCampaigns () {
		return _downloadingCampaigns;
	}
	
	public void setDownloadListener (IUnityAdsDownloadListener listener) {
		_downloadListener = listener;
	}
	
	public boolean isDownloading () {
		return (_downloadingHandlers != null && _downloadingHandlers.size() > 0);
	}
	
	public void initCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		updateCache(activeList, pruneList);
	}
	
	public void updateCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		if (activeList != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Going through active campaigns");
			
			for (UnityAdsCampaign campaign : activeList) {
				UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign, activeList);
				
				if (campaignHandler.hasDownloads()) {
					campaignHandler.setListener(new IUnityAdsCampaignHandlerListener() {
						@Override
						public void onCampaignHandled(UnityAdsCampaignHandler campaignHandler) {
							removeFromDownloadingHandlers(campaignHandler);
							_downloadListener.onCampaignFilesDownloaded(campaignHandler);
							
							if (!isDownloading() && _downloadListener != null)
			        			_downloadListener.onAllDownloadsCompleted();
						}
					});
					
					// TODO: Could be in a better place?
					if (!isDownloading() && _downloadListener != null)
						_downloadListener.onDownloadsStarted();
					
					addToDownloadingHandlers(campaignHandler);
				}
				
				campaignHandler.handleCampaign();
			}
		}
		
		if (pruneList != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Pruning old campaigns");
			for (UnityAdsCampaign campaign : pruneList) {
				if (!UnityAdsUtils.isFileRequiredByCampaigns(campaign.getVideoUrl(), activeList)) {
					UnityAdsUtils.removeFile(campaign.getVideoUrl());
				}
			}
		}
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
