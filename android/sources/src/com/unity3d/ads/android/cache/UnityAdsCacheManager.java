package com.unity3d.ads.android.cache;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.Activity;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignHandlerListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsCacheManager implements IUnityAdsCampaignHandlerListener {

	private IUnityAdsCacheListener _downloadListener = null;	
	private ArrayList<UnityAdsCampaignHandler> _downloadingHandlers = null;
	private ArrayList<UnityAdsCampaignHandler> _handlers = null;	
	private int _amountPrepared = 0;
	private int _totalCampaigns = 0;

	public UnityAdsCacheManager(Activity activity) {
		UnityAdsUtils.chooseCacheDirectory(activity);
		UnityAdsDeviceLog.debug("Unity Ads cache directory: " + UnityAdsUtils.getCacheDirectory());

		if (UnityAdsUtils.canUseExternalStorage()) {
			UnityAdsDeviceLog.debug("Cache directory created with result: " + UnityAdsUtils.createCacheDir());
		}
		else {
			UnityAdsDeviceLog.info("Could not create cache, no external memory present");
		}
	}

	public void setDownloadListener (IUnityAdsCacheListener listener) {
		_downloadListener = listener;
	}

	public void updateCache (ArrayList<UnityAdsCampaign> activeList) {
		if (_downloadListener != null)
			_downloadListener.onCampaignUpdateStarted();
		
		_amountPrepared = 0;
		
		if (activeList != null)
			UnityAdsDeviceLog.debug(activeList.toString());
		
		// Check cache directory and delete all files that don't match the current files in campaigns
		if (UnityAdsUtils.getCacheDirectory() != null) {
			File dir = new File(UnityAdsUtils.getCacheDirectory());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					boolean filter = filename.startsWith(UnityAdsConstants.UNITY_ADS_LOCALFILE_PREFIX);
					UnityAdsDeviceLog.debug("Filtering result for file: " + filename + ", " + filter);
					return filter;
				}
			};

			File[] fileList = dir.listFiles(filter);

			if (fileList != null) {
				for (File currentFile : fileList) {
					UnityAdsDeviceLog.debug("Checking file: " + currentFile.getName());
					if (!currentFile.getName().equals(UnityAdsConstants.PENDING_REQUESTS_FILENAME) && 
						!currentFile.getName().equals(UnityAdsConstants.CACHE_MANIFEST_FILENAME) && 
						!UnityAdsUtils.isFileRequiredByCampaigns(currentFile.getName(), activeList)) {
						if (UnityAdsUtils.removeFile(currentFile.getName()))
							UnityAdsDeviceLog.debug("Removed file: " + currentFile.getName());
						else
							UnityAdsDeviceLog.debug("Should have removed file: " + currentFile.getName() + " but couldn't");
					}
				}
			}
		}

		// Active -list contains campaigns that came with the videoPlan
		if (activeList != null) {
			_totalCampaigns = activeList.size();
			boolean firstInAdPlan = true;

			UnityAdsDeviceLog.debug("Updating cache: Going through active campaigns: " + _totalCampaigns);			
			for (UnityAdsCampaign campaign : activeList) {
				UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign);
				addToUpdatingHandlers(campaignHandler);
				campaignHandler.setListener(this);
				campaignHandler.initCampaign(firstInAdPlan);
				firstInAdPlan = false;

				if (campaignHandler.hasDownloads()) {
					addToDownloadingHandlers(campaignHandler);
				}					
			}
		}
	}

	public boolean isCampaignCached(UnityAdsCampaign campaign, boolean requireCompleteVideo) {
		if(UnityAdsUtils.isFileInCache(campaign.getVideoFilename())) {
			if(!requireCompleteVideo) {
				return true;
			}

			long localSize = UnityAdsUtils.getSizeForLocalFile(campaign.getVideoFilename());
			long expectedSize = campaign.getVideoFileExpectedSize();

			if(localSize > 0 && expectedSize > 0 && localSize == expectedSize) {
				return true;
			}
		}

		return false;
	}

	public void cacheNextVideo(UnityAdsCampaign campaign) {
		UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign);
		campaignHandler.downloadCampaign();
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
			_handlers = new ArrayList<>();

		_handlers.add(campaignHandler);
	}

	private void removeFromDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers != null)
			_downloadingHandlers.remove(campaignHandler);
	}

	private void addToDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers == null)
			_downloadingHandlers = new ArrayList<>();

		_downloadingHandlers.add(campaignHandler);
	}
}