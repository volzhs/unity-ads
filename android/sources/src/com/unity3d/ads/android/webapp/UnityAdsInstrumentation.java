package com.unity3d.ads.android.webapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.data.UnityAdsDevice;
import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsInstrumentation {

	private static ArrayList<Map<String, JSONObject>> _unsentEvents = null;
	
	private static JSONObject mapToJSON (Map<String, Object> mapWithValues) {
		if (mapWithValues != null) {
			JSONObject retJsonObject = new JSONObject();
			
			Set<String> keySet = mapWithValues.keySet();
			Iterator<String> i = keySet.iterator();
			while (i.hasNext()) {
				String key = i.next();
				
				if (mapWithValues.containsKey(key) && mapWithValues.get(key) != null) {
					try {
						retJsonObject.put(key, mapWithValues.get(key));
					}
					catch (Exception e) {
						UnityAdsDeviceLog.error("Could not add value: " + key);
					}
				}
			}
			
			return retJsonObject;
		}
		
		return null;
	}
	
	private static JSONObject mergeJSON (JSONObject json1, JSONObject json2) {
		if (json1 != null && json2 != null) {
			@SuppressWarnings("rawtypes")
			Iterator keyIterator = json2.keys();
			while (keyIterator.hasNext()) {
				try {
					String key = keyIterator.next().toString();
					Object value = json2.get(key);
					json1.put(key, value);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Problems creating JSON");
				}
			}
			
			return json1;
		}
		
		if (json1 != null)
			return json1;		
		else if (json2 != null)
			return json2;
		
		return null;
	}
	
	private static JSONObject getBasicGAVideoProperties (UnityAdsCampaign campaignPlaying) {
		if (campaignPlaying != null) {
			String videoPlayType = UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOPLAY_HLSL;
			
			if (campaignPlaying.shouldCacheVideo() && UnityAdsUtils.canUseExternalStorage()) {
				videoPlayType = UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOPLAY_CACHED;
			}
			
			String connectionType = UnityAdsDevice.getConnectionType();
			
			JSONObject retJsonObject = new JSONObject();
			
			try {
				retJsonObject.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOPLAYBACKTYPE_KEY, videoPlayType);
				retJsonObject.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_CONNECTIONTYPE_KEY, connectionType);
				retJsonObject.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_CAMPAIGNID_KEY, campaignPlaying.getCampaignId());
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Could not create instrumentation JSON");
				return null;
			}
			
			return retJsonObject;
		}
		
		return null;
	}
	
	private static void handleUnsentEvents () {
		sendGAInstrumentationEvents();
	}
	
	private static void sendGAInstrumentationEvents () {
		JSONObject finalData = null;
		JSONArray wrapArray = new JSONArray();
		JSONObject finalEvents = new JSONObject();
		
		if (_unsentEvents != null) {
			for (Map<String, JSONObject> map : _unsentEvents) {
				finalData = new JSONObject();
				String eventType = map.keySet().iterator().next();
				JSONObject data = map.get(eventType);
				
				try {
					finalData.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_KEY, eventType);
					finalData.put("data", data);
				}
				catch (Exception e) {
					continue;
				}
				
				wrapArray.put(finalData);
				
				try {
					finalEvents.put("events", wrapArray);
				}
				catch (Exception e) {
				}
			}
			
			if (UnityAds.mainview != null && UnityAds.mainview.webview != null && UnityAds.mainview.webview.isWebAppLoaded()) {
				UnityAdsDeviceLog.debug("Sending to webapp");
				UnityAds.mainview.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_KEY, finalEvents);
				_unsentEvents.clear();
				_unsentEvents = null;
			}
		}
	}
	
	private static void sendGAInstrumentationEvent (String eventType, JSONObject data) {
		
		JSONObject finalData = new JSONObject();
		JSONArray wrapArray = new JSONArray();
		JSONObject events = new JSONObject();
		
		try {
			finalData.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_KEY, eventType);
			finalData.put("data", data);
			wrapArray.put(finalData);
			events.put("events", wrapArray);
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Couldn't create final data");
		}
		
		if (UnityAds.mainview != null && UnityAds.mainview.webview != null && UnityAds.mainview.webview.isWebAppLoaded()) {
			UnityAdsDeviceLog.debug("Sending to webapp");
			UnityAds.mainview.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_KEY, events);
		}
		else {
			UnityAdsDeviceLog.debug("WebApp not initialized, could not send event!");
			
			if (_unsentEvents == null) {
				_unsentEvents = new ArrayList<Map<String,JSONObject>>();
			}
			
			Map <String, JSONObject> tmpData = new HashMap<String, JSONObject>();
			tmpData.put(eventType, data);
			_unsentEvents.add(tmpData);
		}
	}
	
	public static void gaInstrumentationVideoPlay (UnityAdsCampaign campaignPlaying, Map<String, Object> additionalValues) {
		JSONObject data = getBasicGAVideoProperties(campaignPlaying);
		data = mergeJSON(data, mapToJSON(additionalValues));
		handleUnsentEvents();
		sendGAInstrumentationEvent(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_VIDEOPLAY, data);
	}
	
	public static void gaInstrumentationVideoError (UnityAdsCampaign campaignPlaying, Map<String, Object> additionalValues) {
		JSONObject data = getBasicGAVideoProperties(campaignPlaying);
		data = mergeJSON(data, mapToJSON(additionalValues));
		handleUnsentEvents();
		sendGAInstrumentationEvent(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_VIDEOERROR, data);
	}
	
	public static void gaInstrumentationVideoAbort (UnityAdsCampaign campaignPlaying, Map<String, Object> additionalValues) {
		JSONObject data = getBasicGAVideoProperties(campaignPlaying);
		data = mergeJSON(data, mapToJSON(additionalValues));
		handleUnsentEvents();
		sendGAInstrumentationEvent(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_VIDEOABORT, data);		
	}
	
	public static void gaInstrumentationVideoCaching (UnityAdsCampaign campaignPlaying, Map<String, Object> additionalValues) {
		JSONObject data = getBasicGAVideoProperties(campaignPlaying);
		data = mergeJSON(data, mapToJSON(additionalValues));
		handleUnsentEvents();
		sendGAInstrumentationEvent(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_TYPE_VIDEOCACHING, data);				
	}
}
