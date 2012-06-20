package com.unity3d.ads.android.cache;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONObject;

import android.util.Log;

import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;

public class UnityAdsCacheManifest {
	
	private JSONObject _manifestJson = null;
	private String _manifestContent = "";
	private ArrayList<UnityAdsCampaign> _cachedCampaigns = null;
	
	
	public UnityAdsCacheManifest () {
		readCacheManifest();
		createCampaignsFromManifest();
	}
	
	public int getCachedCampaignAmount () {
		if (_cachedCampaigns == null) 
			return 0;
		else
			return _cachedCampaigns.size();
	}
	
	public ArrayList<String> getCachedCampaignIds () {
		if (_cachedCampaigns == null) 
			return null;
		else {
			ArrayList<String> retList = new ArrayList<String>();
			
			for (UnityAdsCampaign campaign : _cachedCampaigns) {
				if (campaign != null)
					retList.add(campaign.getCampaignId());
			}
			
			return retList;
		}
	}
	
	public void setCachedCampaigns (ArrayList<UnityAdsCampaign> campaigns) {
		_cachedCampaigns = campaigns;
		writeCurrentCacheManifest();
	}
	
	public ArrayList<UnityAdsCampaign> getCachedCampaigns () {
		return _cachedCampaigns;
	}
	
	public ArrayList<UnityAdsCampaign> getViewableCachedCampaigns () {
		ArrayList<UnityAdsCampaign> retList = new ArrayList<UnityAdsCampaign>();
		
		if (_cachedCampaigns != null) {
			for (UnityAdsCampaign campaign : _cachedCampaigns) {
				if (!campaign.getCampaignStatus().equals("viewed"))
					retList.add(campaign);
			}
			
			return retList;
		}
		
		return null;
	}
	
	public UnityAdsCampaign getCachedCampaignById (String id) {
		if (id == null || _cachedCampaigns == null) 
			return null;
		else {
			for (UnityAdsCampaign campaign : _cachedCampaigns) {
				if (campaign.getCampaignId().equals(id))
					return campaign;
			}
		}
		
		return null;
	}
	
	public boolean removeCampaignFromManifest (String campaignId) {		
		if (campaignId == null || _cachedCampaigns == null) return false;
		
		UnityAdsCampaign currentCampaign = null;
		int indexOfCampaignToRemove = -1;
		
		for (int i = 0; i < _cachedCampaigns.size(); i++) {
			currentCampaign = _cachedCampaigns.get(i);
			
			if (currentCampaign.getCampaignId().equals(campaignId)) {
				indexOfCampaignToRemove = i;
				break;
			}
		}
		
		if (indexOfCampaignToRemove > -1) {
			_cachedCampaigns.remove(indexOfCampaignToRemove);
			writeCurrentCacheManifest();
			return true;
		}
		
		return false;
	}
	
	public boolean addCampaignToManifest (UnityAdsCampaign campaign) {
		if (campaign == null) return false;
		if (_cachedCampaigns == null)
			_cachedCampaigns = new ArrayList<UnityAdsCampaign>();
		
		if (getCachedCampaignById(campaign.getCampaignId()) == null) {
			_cachedCampaigns.add(campaign);
			writeCurrentCacheManifest();
			return true;
		}
		
		return false;
	}
	
	public boolean updateCampaignInManifest (UnityAdsCampaign campaign) {
		if (campaign == null || _cachedCampaigns == null) return false;

		int updateIndex = -1;
		UnityAdsCampaign cacheCampaign = getCachedCampaignById(campaign.getCampaignId());
		if (cacheCampaign != null)
			updateIndex = _cachedCampaigns.indexOf(cacheCampaign);
		
		if (updateIndex > -1) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating campaign: " + campaign.getCampaignId());
			_cachedCampaigns.set(updateIndex, campaign);
			writeCurrentCacheManifest();
			
			return true;
		}
			
		return false;
	}
	
	public boolean writeCurrentCacheManifest () {
		JSONObject manifestToWrite = UnityAdsUtils.createJsonFromCampaigns(_cachedCampaigns);
		
		if (manifestToWrite != null) {
			return UnityAdsUtils.writeFile(getFileForManifest(), manifestToWrite.toString());
		}
		else {
			return UnityAdsUtils.writeFile(getFileForManifest(), "");
		}
	}
	
	
	/* INTERNAL METHODS */
	
	private void createCampaignsFromManifest () {
		if (_manifestJson == null) return;		
		
		_cachedCampaigns = UnityAdsUtils.createCampaignsFromJson(_manifestJson);
	}
	
	private boolean readCacheManifest () {		
		File manifest = getFileForManifest();
		_manifestContent = UnityAdsUtils.readFile(manifest);
		
		if (_manifestContent != null) {
			try {
				_manifestJson = new JSONObject(_manifestContent);
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problem creating manifest json: " + e.getMessage());
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	private File getFileForManifest () {
		return new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.CACHE_MANIFEST_FILENAME);
	}
}