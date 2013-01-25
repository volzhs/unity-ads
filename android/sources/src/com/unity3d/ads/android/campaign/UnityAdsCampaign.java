package com.unity3d.ads.android.campaign;

import java.io.File;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsCampaign {
	
	public enum UnityAdsCampaignStatus { READY, VIEWED, PANIC;
		@Override
		public String toString () {
			String output = name().toString().toLowerCase();
			return output;
		}
		
		public static UnityAdsCampaignStatus getValueOf (String status) {
			if (UnityAdsCampaignStatus.READY.toString().equals(status.toLowerCase()))
				return UnityAdsCampaignStatus.READY;
			else if (UnityAdsCampaignStatus.VIEWED.toString().equals(status.toLowerCase()))
				return UnityAdsCampaignStatus.VIEWED;
			else
				return UnityAdsCampaignStatus.PANIC;
		}
	};
	
	private JSONObject _campaignJson = null;
	private String[] _requiredKeys = new String[] {
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_ENDSCREEN_KEY, 
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_CLICKURL_KEY, 
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_PICTURE_KEY, 
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_TRAILER_DOWNLOADABLE_KEY, 
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_TRAILER_STREAMING_KEY,
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_GAME_ID_KEY,
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_GAME_NAME_KEY,
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_ID_KEY,
			UnityAdsConstants.UNITY_ADS_CAMPAIGN_TAGLINE_KEY};
	private UnityAdsCampaignStatus _campaignStatus = UnityAdsCampaignStatus.READY;
	
	public UnityAdsCampaign () {		
	}
	
	public UnityAdsCampaign (JSONObject fromJSON) {
		_campaignJson = fromJSON;
	}
	
	@Override
	public String toString () {
		return "<ID: " + getCampaignId() + ", STATUS: " + getCampaignStatus().toString() + ", URL: " + getVideoUrl() + ">"; 
	}
	
	public JSONObject toJson () {
		JSONObject retObject = _campaignJson;
		
		try {
			retObject.put("status", getCampaignStatus().toString());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Error creating campaign JSON", this);
			return null;
		}
		
		return retObject;
	}
	
	public Boolean shouldCacheVideo () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getBoolean(UnityAdsConstants.UNITY_ADS_CAMPAIGN_CACHE_VIDEO_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("shouldCacheVideo: This should not happen!", this);
			}			
		}
		return false;
	}

	public String getEndScreenUrl () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_ENDSCREEN_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getEndScreenUrl: This should not happen!", this);
			}
		}
		
		return null;		
	}
	
	public String getPicture () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_PICTURE_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getPicture: This should not happen!", this);
			}
		}
		
		return null;		
	}
	
	public String getCampaignId () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_ID_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getCampaignId: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getGameId () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_GAME_ID_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getGameId: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getGameName () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_GAME_NAME_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getGameName: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getVideoUrl () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_TRAILER_DOWNLOADABLE_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getVideoUrl: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getVideoStreamUrl () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_TRAILER_STREAMING_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getVideoStreamUrl: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getClickUrl () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_CLICKURL_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getClickUrl: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getVideoFilename () {
		if (checkDataIntegrity()) {
			try {
				File videoFile = new File(_campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_TRAILER_DOWNLOADABLE_KEY));
				return getCampaignId() + "-" + videoFile.getName();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getVideoFilename: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public String getTagLine () {
		if (checkDataIntegrity()) {
			try {
				return _campaignJson.getString(UnityAdsConstants.UNITY_ADS_CAMPAIGN_TAGLINE_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("getTagLine: This should not happen!", this);
			}
		}
		
		return null;
	}
	
	public UnityAdsCampaignStatus getCampaignStatus () {
		return _campaignStatus;
	}
	
	public void setCampaignStatus (UnityAdsCampaignStatus status) {
		_campaignStatus = status;
	}
	
	public Boolean isViewed () {
		if (_campaignStatus == UnityAdsCampaignStatus.VIEWED)
			return true;
		
		return false;
	}
	
	public boolean hasValidData () {
		return checkDataIntegrity();
	}
	
	/* INTERNAL METHODS */
	
	private boolean checkDataIntegrity () {
		if (_campaignJson != null) {
			for (String key : _requiredKeys) {
				if (!_campaignJson.has(key)) {
					return false;
				}
			}
			
			return true;
		}
		return false;
	}
}
