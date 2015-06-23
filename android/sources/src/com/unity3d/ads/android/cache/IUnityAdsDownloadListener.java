package com.unity3d.ads.android.cache;

public interface IUnityAdsDownloadListener {
	void onFileDownloadCompleted (String downloadUrl);
	void onFileDownloadCancelled (String downloadUrl);
}
