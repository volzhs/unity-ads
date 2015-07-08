package com.unity3d.ads.android.cache;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;

public class UnityAdsCache {
	public static void initialize(ArrayList<UnityAdsCampaign> campaigns) {
		if(campaigns == null || campaigns.size() == 0) return;

		UnityAdsDeviceLog.debug("Unity Ads cache: initializing cache with " + campaigns.size() + " campaigns");

		stopAllDownloads();

		HashMap<String,String> downloadFiles = new HashMap<String,String>();
		HashMap<String,Long> allFiles = new HashMap<String,Long>();

		boolean first = true;
		for(UnityAdsCampaign campaign : campaigns) {
			if(campaign.shouldCacheVideo() || (campaign.allowCacheVideo() && first)) {
				String filename = campaign.getVideoFilename();

				if(!isFileCached(filename, campaign.getVideoFileExpectedSize())) {
					UnityAdsDeviceLog.debug("Unity Ads cache: queuing " + filename + " for download");
					downloadFiles.put(campaign.getVideoUrl(), filename);
				}
			}

			allFiles.put(campaign.getVideoFilename(), campaign.getVideoFileExpectedSize());

			first = false;
		}

		initializeCacheDirectory(allFiles);

		for(Map.Entry<String,String> entry : downloadFiles.entrySet()) {
			UnityAdsCacheThread.download(entry.getKey(), getFullFilename(entry.getValue()));
		}
	}

	public static void cacheCampaign(UnityAdsCampaign campaign) {
		String filename = campaign.getVideoFilename();
		long size = campaign.getVideoFileExpectedSize();

		// Check if video is already in cache
		if(isFileCached(filename, size)) return;

		String currentDownload = UnityAdsCacheThread.getCurrentDownload();

		// Check if video is currently downloaded
		if(currentDownload != null && currentDownload.equals(getFullFilename(filename))) return;

		UnityAdsCacheThread.download(campaign.getVideoUrl(), getFullFilename(filename));
	}

	public static boolean isCampaignCached(UnityAdsCampaign campaign) {
		String filename = campaign.getVideoFilename();
		long size = campaign.getVideoFileExpectedSize();

		return isFileCached(filename, size);
	}

	public static void stopAllDownloads() {
		UnityAdsCacheThread.stopAllDownloads();
	}

	private static void initializeCacheDirectory(HashMap<String,Long> files) {
		// TODO: Remove references to UnityAdsUtils
		UnityAdsUtils.chooseCacheDirectory(UnityAdsProperties.APPLICATION_CONTEXT);
		File cacheDir = UnityAdsUtils.createCacheDir();

		if(cacheDir == null || !cacheDir.isDirectory()) {
			UnityAdsDeviceLog.error("Unity Ads cache: Creating cache dir failed");
			return;
		}

		// Don't delete pending events
		files.put(UnityAdsConstants.PENDING_REQUESTS_FILENAME, Long.valueOf(-1));

		File[] fileList;

		if(cacheDir.getAbsolutePath().endsWith(UnityAdsConstants.CACHE_DIR_NAME)) {
			fileList = cacheDir.listFiles();
		} else {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					boolean filter = filename.startsWith(UnityAdsConstants.UNITY_ADS_LOCALFILE_PREFIX);
					UnityAdsDeviceLog.debug("Filtering result for file: " + filename + ", " + filter);
					return filter;
				}
			};

			fileList = cacheDir.listFiles(filter);
		}

		for(File cacheFile : fileList) {
			String name = cacheFile.getName();

			if(!files.containsKey(name)) {
				UnityAdsDeviceLog.debug("Unity Ads cache: " + name + " not found in ad plan, deleting from cache");
				cacheFile.delete();
			} else {
				long expectedSize = files.get(name);

				if(expectedSize != -1) {
					long size = cacheFile.length();

					if(size != expectedSize && expectedSize != -1) {
						UnityAdsDeviceLog.debug("Unity Ads cache: " + name + " file size mismatch, deleting from cache");
						cacheFile.delete();
					}
				}
			}
		}
	}

	public static String getCacheDirectory() {
		// TODO: Remove references to UnityAdsUtils
		return UnityAdsUtils.getCacheDirectory();
	}

	private static String getFullFilename(String filename) {
		return getCacheDirectory() + "/" + filename;
	}

	private static boolean isFileCached(String file, long size) {
		File cacheFile = new File(getCacheDirectory() + "/" + file);

		if(cacheFile.exists()) {
			if(size == -1 || cacheFile.length() == size) {
				return true;
			}
		}

		return false;
	}
}