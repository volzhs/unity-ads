package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;

public interface IUnityAdsDownloadListener {
	public void onDownloadsStarted ();
	public void onCampaignFilesDownloaded (UnityAdsCampaignHandler campaignHandler);
	public void onAllDownloadsCompleted ();
}
