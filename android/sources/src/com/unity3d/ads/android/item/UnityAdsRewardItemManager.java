package com.unity3d.ads.android.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.unity3d.ads.android.UnityAdsDeviceLog;

public class UnityAdsRewardItemManager {

	private Map<String, UnityAdsRewardItem> _rewardItems = null;
	private UnityAdsRewardItem _currentItem = null;
	private UnityAdsRewardItem _defaultItem = null;
	
	public UnityAdsRewardItemManager(JSONArray rewardItemArray, String defaultItem) {
		_rewardItems = new HashMap<String, UnityAdsRewardItem>();
		
		for(int i = 0; i < rewardItemArray.length(); ++i) {
			try {
				JSONObject rewardItemObject = rewardItemArray.getJSONObject(i);
				UnityAdsRewardItem rewardItem = new UnityAdsRewardItem(rewardItemObject);
				
				if(rewardItem.hasValidData()) {
					if(rewardItem.getKey().equals(defaultItem)) {
						_currentItem = rewardItem;
						_defaultItem = rewardItem;
					}
					
					_rewardItems.put(rewardItem.getKey(), rewardItem);
				}
			} catch(JSONException e) {
				UnityAdsDeviceLog.error("Failed to parse reward item");
			}
		}
	}
	
	public UnityAdsRewardItem getItem(String rewardItemKey) {
		if(_rewardItems.containsKey(rewardItemKey)) {
			return _rewardItems.get(rewardItemKey);
		}
		return null;
	}
	
	public UnityAdsRewardItem getCurrentItem() {
		return _currentItem;
	}
	
	public UnityAdsRewardItem getDefaultItem() {
		return _defaultItem;
	}
	
	public boolean setCurrentItem(String rewardItemKey) {
		if(_rewardItems.containsKey(rewardItemKey)) {
			_currentItem = _rewardItems.get(rewardItemKey);
			return true;
		}
		return false;
	}
	
	public ArrayList<UnityAdsRewardItem> allItems() {
		ArrayList<UnityAdsRewardItem> itemArray = new ArrayList<UnityAdsRewardItem>();
		for(UnityAdsRewardItem rewardItem : _rewardItems.values()) {
			itemArray.add(rewardItem);
		}
		return itemArray;
	}
	
	public int itemCount() {
		return _rewardItems.size();
	}
	
}
