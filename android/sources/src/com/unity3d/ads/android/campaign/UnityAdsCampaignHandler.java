package com.unity3d.ads.android.campaign;

import java.util.ArrayList;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsDownloadListener;

public class UnityAdsCampaignHandler implements IUnityAdsDownloadListener {
	
	private ArrayList<String> _downloadList = null;
	private UnityAdsCampaign _campaign = null;
	private IUnityAdsCampaignHandlerListener _handlerListener = null;
	//private boolean _cancelledDownloads = false;
	
	
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
		if (finishDownload(downloadUrl))
			UnityAdsUtils.Log("Reporting campaign download completion: " + _campaign.getCampaignId(), this);
		
	}
	
	@Override
	public void onFileDownloadCancelled (String downloadUrl) {	
		if (finishDownload(downloadUrl)) {
			UnityAdsUtils.Log("Download cancelled: " + _campaign.getCampaignId(), this);
			//_cancelledDownloads = true;
		}
	}
	
	public void initCampaign () {
		// Check video
		checkFileAndDownloadIfNeeded(_campaign.getVideoUrl());
		
		if (_handlerListener != null) {
			_handlerListener.onCampaignHandled(this);
		}
		
		/*
		if (!hasDownloads() && _handlerListener != null && !_cancelledDownloads) {
			_handlerListener.onCampaignHandled(this);
		}
		*/
	}
	
	
	/* INTERNAL METHODS */
	
	private boolean finishDownload (String downloadUrl) {
		removeDownload(downloadUrl);
		
		if (_downloadList != null && _downloadList.size() == 0 && _handlerListener != null) {
			UnityAdsDownloader.removeListener(this);
			//_handlerListener.onCampaignHandled(this);
			return true;
		}
		
		return false;
	}
	
	private void checkFileAndDownloadIfNeeded (String fileUrl) {
		if (_campaign.shouldCacheVideo() && !UnityAdsUtils.isFileInCache(_campaign.getVideoFilename()) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);
			
			addCampaignToDownloads();
		}
		else if (!isFileOk(fileUrl) && _campaign.shouldCacheVideo() && UnityAdsUtils.canUseExternalStorage()) {
			UnityAdsUtils.removeFile(fileUrl);
			UnityAdsDownloader.addListener(this);
			addCampaignToDownloads();
		}		
	}
	
	private boolean isFileOk (String fileUrl) {
		// TODO: Implement isFileOk
		return true;
	}
	
	private void addCampaignToDownloads () {
		if (_campaign == null) return;
		if (_downloadList == null) _downloadList = new ArrayList<String>();
		
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
