package com.unity3d.ads.android;

public interface IUnityAdsListener {
	// Unity Ads view events
	void onHide ();
	void onShow ();

	// Unity Ads video events
	void onVideoStarted ();
	void onVideoCompleted (String rewardItemKey, boolean skipped);

	// Unity Ads campaign events
	void onFetchCompleted ();
	void onFetchFailed ();
}
