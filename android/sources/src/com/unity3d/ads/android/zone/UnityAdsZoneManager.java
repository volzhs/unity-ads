package com.unity3d.ads.android.zone;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsZoneManager {

	private Map<String, UnityAdsZone> _zones = null; 
	private UnityAdsZone _currentZone = null;
	
	public UnityAdsZoneManager(JSONArray zoneArray) {
		_zones = new HashMap<String, UnityAdsZone>();
		
		for(int i = 0; i < zoneArray.length(); ++i) {
			try {
				JSONObject jsonZone = zoneArray.getJSONObject(i);
				UnityAdsZone zone = null;
				if(jsonZone.getBoolean(UnityAdsConstants.UNITY_ADS_ZONE_INCENTIVIZED_KEY)) {
					zone = new UnityAdsIncentivizedZone(jsonZone);
				} else {
					zone = new UnityAdsZone(jsonZone);
				}
				
				if(_currentZone == null && zone.isDefault()) {
					_currentZone = zone;
				}
				
				_zones.put(zone.getZoneId(), zone);
			} catch(JSONException e) {
				UnityAdsUtils.Log("Failed to parse zone", this);
			}
		} 
	}
	
	public UnityAdsZone getCurrentZone() {
		return _currentZone;
	}
	
	public boolean setCurrentZone(String zoneId) {
		if(_zones.containsKey(zoneId)) {
			_currentZone = _zones.get(zoneId);
			return true;
		}
		return false;
	}
	
	public int zoneCount() {
		return _zones.size();
	}
	
	public void clear() {
		_currentZone = null;
		_zones.clear();
		_zones = null;
	}
	
}
