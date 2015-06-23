package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;

public interface IUnityAdsCacheListener {
	void onCampaignUpdateStarted ();
	void onCampaignReady (UnityAdsCampaignHandler campaignHandler);
	void onAllCampaignsReady ();
}
