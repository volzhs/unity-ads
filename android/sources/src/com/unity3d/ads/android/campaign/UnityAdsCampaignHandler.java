package com.unity3d.ads.android.campaign;

import java.util.ArrayList;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsDownloadListener;

public class UnityAdsCampaignHandler implements IUnityAdsDownloadListener {
	private ArrayList<String> _downloadList = null;
	private UnityAdsCampaign _campaign = null;
	private IUnityAdsCampaignHandlerListener _handlerListener = null;

	public UnityAdsCampaignHandler (UnityAdsCampaign campaign) {
		_campaign = campaign;
	}
	
	public boolean hasDownloads () {
		return (_downloadList != null && _downloadList.size() > 0);
	}

	public UnityAdsCampaign getCampaign () {
		return _campaign;
	}

	public void setListener (IUnityAdsCampaignHandlerListener listener) {
		_handlerListener = listener;
	}

	@Override
	public void onFileDownloadCompleted (String downloadUrl) {
		if (finishDownload(downloadUrl)) {
			UnityAdsDeviceLog.debug("Reporting campaign download completion: " + _campaign.getCampaignId());
		}
	}

	@Override
	public void onFileDownloadCancelled (String downloadUrl) {	
		if (finishDownload(downloadUrl)) {
			UnityAdsDeviceLog.debug("Download cancelled: " + _campaign.getCampaignId());
		}
	}

	public void initCampaign(boolean firstInAdPlan) {
		checkFileAndDownloadIfNeeded(_campaign, firstInAdPlan);

		if (_handlerListener != null) {
			_handlerListener.onCampaignHandled(this);
		}
	}

	public void downloadCampaign() {
		if (!UnityAdsUtils.isFileInCache(_campaign.getVideoFilename()) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);

			addCampaignToDownloads();			
		}
		else if (!isCampaignFileOk(_campaign) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);

			UnityAdsUtils.removeFile(_campaign.getVideoFilename());
			UnityAdsDownloader.addListener(this);
			addCampaignToDownloads();
		}		
	}

	/* INTERNAL METHODS */

	private boolean finishDownload (String downloadUrl) {
		removeDownload(downloadUrl);
		
		if (_downloadList != null && _downloadList.size() == 0 && _handlerListener != null) {
			UnityAdsDownloader.removeListener(this);
			return true;
		}

		return false;
	}

	private void checkFileAndDownloadIfNeeded(UnityAdsCampaign campaign, boolean firstInAdPlan) {
		if((campaign.shouldCacheVideo() || (campaign.allowCacheVideo() && firstInAdPlan)) && !UnityAdsUtils.isFileInCache(campaign.getVideoFilename()) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);

			addCampaignToDownloads();			
		}
		else if (campaign.shouldCacheVideo() && !isCampaignFileOk(campaign) && UnityAdsUtils.canUseExternalStorage()) {
			UnityAdsDeviceLog.debug("The file was not okay, redownloading");
			UnityAdsUtils.removeFile(campaign.getVideoFilename());
			UnityAdsDownloader.addListener(this);
			addCampaignToDownloads();
		}		
	}

	private boolean isCampaignFileOk(UnityAdsCampaign campaign) {
		long localSize = UnityAdsUtils.getSizeForLocalFile(campaign.getVideoFilename());
		long expectedSize = campaign.getVideoFileExpectedSize();

		UnityAdsDeviceLog.debug("localSize=" + localSize + ", expectedSize=" + expectedSize);
		return localSize != -1 && (expectedSize == -1 || localSize > 0 && expectedSize > 0 && localSize == expectedSize);
	}

	private void addCampaignToDownloads () {
		if (_campaign == null) return;
		if (_downloadList == null) _downloadList = new ArrayList<>();

		_downloadList.add(_campaign.getVideoUrl());
		UnityAdsDownloader.addDownload(_campaign);
	}

	private void removeDownload (String downloadUrl) {
		if (_downloadList == null) return;

		int removeIndex = -1;

		for (int i = 0; i < _downloadList.size(); i++) {
			if (_downloadList.get(i).equals(downloadUrl)) {
				removeIndex = i;
				break;
			}
		}

		if (removeIndex > -1)
			_downloadList.remove(removeIndex);
	}
}
