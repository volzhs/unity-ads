package com.unity3d.ads.android.campaign;

import org.json.JSONObject;

import android.util.Log;

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
	
	/* INTERNAL METHODS */
	
	private void parseValues () {
		try {
			_key = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY);
			_name = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_NAME_KEY);
			_pictureURL = _rewardItemJSON.getString(UnityAdsConstants.UNITY_ADS_REWARD_PICTURE_KEY);
		}
		catch (Exception e) {
			Log.d(UnityAdsConstants.LOG_NAME, "Problem parsing campaign values");
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
