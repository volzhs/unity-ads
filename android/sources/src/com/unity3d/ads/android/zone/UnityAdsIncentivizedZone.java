package com.unity3d.ads.android.zone;

import org.json.JSONException;
import org.json.JSONObject;

import com.unity3d.ads.android.item.UnityAdsRewardItem;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsIncentivizedZone extends UnityAdsZone {

	private UnityAdsRewardItemManager _rewardItems = null;
	
	public UnityAdsIncentivizedZone(JSONObject zoneObject) throws JSONException {
		super(zoneObject);
		UnityAdsRewardItem defaultItem = new UnityAdsRewardItem(zoneObject.getJSONObject(UnityAdsConstants.UNITY_ADS_ZONE_DEFAULT_REWARD_ITEM_KEY));
		_rewardItems = new UnityAdsRewardItemManager(zoneObject.getJSONArray(UnityAdsConstants.UNITY_ADS_ZONE_REWARD_ITEMS_KEY), defaultItem.getKey());
	}
	
	@Override
	public boolean isIncentivized() {
		return true;
	}
	
	public UnityAdsRewardItemManager itemManager() {
		return _rewardItems;
	}

}
