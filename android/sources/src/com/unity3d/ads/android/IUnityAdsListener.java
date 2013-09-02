package com.unity3d.ads.android;

public interface IUnityAdsListener {
	// Unity Ads view events
	public void onHide ();
	public void onShow ();
	
	// Unity Ads video events
	public void onVideoStarted ();
	public void onVideoCompleted (String rewardItemKey, boolean skipped);
	
	// Unity Ads campaign events
	public void onFetchCompleted ();
	public void onFetchFailed ();
}
