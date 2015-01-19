package com.unity3d.ads.android.campaign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsDownloadListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.webapp.UnityAdsInstrumentation;

public class UnityAdsCampaignHandler implements IUnityAdsDownloadListener {
	
	private ArrayList<String> _downloadList = null;
	private UnityAdsCampaign _campaign = null;
	private IUnityAdsCampaignHandlerListener _handlerListener = null;
	private long _cacheStartMillis = 0;
	private long _cacheSolvedMillis = 0;
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
	
	public long getCachingDurationInMillis () {
		if (_cacheStartMillis > 0 && _cacheSolvedMillis > 0) {
			return _cacheSolvedMillis - _cacheStartMillis;
		}
		
		return 0;
	}
	
	public void setListener (IUnityAdsCampaignHandlerListener listener) {
		_handlerListener = listener;
	}
	
	@Override
	public void onFileDownloadCompleted (String downloadUrl) {
		if (finishDownload(downloadUrl)) {
			UnityAdsDeviceLog.debug("Reporting campaign download completion: " + _campaign.getCampaignId());
			
			// Analytics / Instrumentation
			Map<String, Object> values = new HashMap<String, Object>();
			values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VALUE_KEY, UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOCACHING_COMPLETED);
			values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_CACHINGDURATION_KEY, getCachingDurationInMillis());
			UnityAdsInstrumentation.gaInstrumentationVideoCaching(_campaign, values);		
		}
	}
	
	@Override
	public void onFileDownloadCancelled (String downloadUrl) {	
		if (finishDownload(downloadUrl)) {
			UnityAdsDeviceLog.debug("Download cancelled: " + _campaign.getCampaignId());
			
			// Analytics / Instrumentation
			Map<String, Object> values = new HashMap<String, Object>();
			values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VALUE_KEY, UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOCACHING_FAILED);			
			UnityAdsInstrumentation.gaInstrumentationVideoCaching(_campaign, values);	
		}
	}
	
	public void initCampaign(boolean firstInAdPlan) {
		// Check video
		checkFileAndDownloadIfNeeded(_campaign.getVideoUrl(), firstInAdPlan);
		
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
		else if (!isFileOk(_campaign.getVideoFilename()) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);

			UnityAdsUtils.removeFile(_campaign.getVideoFilename());
			UnityAdsDownloader.addListener(this);
			addCampaignToDownloads();
		}		
	}

	public void clearData () {
		if (_handlerListener != null)
			_handlerListener = null;
		
		if (_downloadList != null) {
			_downloadList.clear();
		}
		
		if (_campaign != null) {
			_campaign.clearData();
			_campaign = null;
		}
	}
	
	
	/* INTERNAL METHODS */
	
	private boolean finishDownload (String downloadUrl) {
		_cacheSolvedMillis = System.currentTimeMillis();
		removeDownload(downloadUrl);
		
		if (_downloadList != null && _downloadList.size() == 0 && _handlerListener != null) {
			UnityAdsDownloader.removeListener(this);
			return true;
		}
		
		return false;
	}
	
	private void checkFileAndDownloadIfNeeded(String fileUrl, boolean firstInAdPlan) {
		if((_campaign.shouldCacheVideo() || (_campaign.allowCacheVideo() && firstInAdPlan)) && !UnityAdsUtils.isFileInCache(_campaign.getVideoFilename()) && UnityAdsUtils.canUseExternalStorage()) {
			if (!hasDownloads())
				UnityAdsDownloader.addListener(this);
			
			addCampaignToDownloads();			
		}
		else if (_campaign.shouldCacheVideo() && !isFileOk(fileUrl) && UnityAdsUtils.canUseExternalStorage()) {
			UnityAdsDeviceLog.debug("The file was not okay, redownloading");
			UnityAdsUtils.removeFile(_campaign.getVideoFilename());
			UnityAdsDownloader.addListener(this);
			addCampaignToDownloads();
		}		
	}
	
	private boolean isFileOk (String fileUrl) {
		long localSize = UnityAdsUtils.getSizeForLocalFile(_campaign.getVideoFilename());
		long expectedSize = _campaign.getVideoFileExpectedSize();
		
		UnityAdsDeviceLog.debug("localSize=" + localSize + ", expectedSize=" + expectedSize);
				
		if (localSize == -1)
			return false;
		
		if (expectedSize == -1)
			return true;
		
		if (localSize > 0 && expectedSize > 0 && localSize == expectedSize)
			return true;
			
		return false;
	}
	
	private void addCampaignToDownloads () {
		if (_campaign == null) return;
		if (_downloadList == null) _downloadList = new ArrayList<String>();
		
		_downloadList.add(_campaign.getVideoUrl());
		_cacheStartMillis = System.currentTimeMillis();
		
		// Analytics / Instrumentation
		Map<String, Object> values = new HashMap<String, Object>();
		values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VALUE_KEY, UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOCACHING_START);			
		UnityAdsInstrumentation.gaInstrumentationVideoCaching(_campaign, values);
		
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
