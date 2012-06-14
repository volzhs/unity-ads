package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;

public interface IUnityAdsCacheListener {
	public void onCampaignUpdateStarted ();
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler);
	public void onAllCampaignsReady ();
}
