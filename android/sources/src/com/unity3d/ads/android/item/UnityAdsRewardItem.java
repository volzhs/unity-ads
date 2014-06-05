package com.unity3d.ads.android.item;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsRewardItem {
	private String _key = null;
	private String _name = null;
	private String _pictureURL = null;
	private JSONObject _rewardItemJSON = null;
	
	private String[] _requiredKeys = new String[] {
			UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY,
			UnityAdsConstants.UNITY_ADS_REWARD_NAME_KEY,
			UnityAdsConstants.UNITY_ADS_REWARD_PICTURE_KEY};
	
	public UnityAdsRewardItem (JSONObject fromJSON) {
		_rewardItemJSON = fromJSON;
		parseValues();
	}
	
	public String getKey () {
		return _key;
	}
	
	public String getName () {
		return _name;
	}
	
	public String getPictureUrl () {
		return _pictureURL;
	}
	
	public boolean hasValidData () {
		return checkDataIntegrity();
	}
	
	public void clearData () {
		_key = null;
		_name = null;
		_pictureURL = null;
		_rewardItemJSON = null;
		_requiredKeys = null;
	}
	
	public Map<String, String> getDetails () {
		Map<String, String> returnMap = new HashMap<String, String>();
		returnMap.put(UnityAds.UNITY_ADS_REWARDITEM_NAME_KEY, getName());
		returnMap.put(UnityAds.UNITY_ADS_REWARDITEM_PICTURE_KEY, getPictureUrl());
		return returnMap;
	}
	
	/* INTERNAL METHODS */
	
	private void parseValues () {
		try {
			_key = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY);
			_name = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_NAME_KEY);
			_pictureURL = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_PICTURE_KEY);
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Problem parsing campaign values");
		}
	}
	
	private boolean checkDataIntegrity () {
		if (_rewardItemJSON != null) {
			for (String key : _requiredKeys) {
				if (!_rewardItemJSON.has(key)) {
					return false;
				}
			}
			
			return true;
		}
		return false;
	}
}
