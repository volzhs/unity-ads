package com.unity3d.ads.android.cache;

public interface IUnityAdsDownloadListener {
	public void onFileDownloadCompleted (String downloadUrl);
	public void onFileDownloadCancelled (String downloadUrl);
}
