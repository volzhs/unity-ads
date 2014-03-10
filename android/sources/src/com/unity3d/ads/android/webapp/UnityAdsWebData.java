package com.unity3d.ads.android.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.data.UnityAdsDevice;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.zone.UnityAdsIncentivizedZone;
import com.unity3d.ads.android.zone.UnityAdsZone;
import com.unity3d.ads.android.zone.UnityAdsZoneManager;

public class UnityAdsWebData {
	
	private JSONObject _campaignJson = null;
	private ArrayList<UnityAdsCampaign> _campaigns = null;
	private IUnityAdsWebDataListener _listener = null;
	private ArrayList<UnityAdsUrlLoader> _urlLoaders = null;
	private ArrayList<UnityAdsUrlLoader> _failedUrlLoaders = null;
	private UnityAdsUrlLoader _currentLoader = null;
	private static UnityAdsZoneManager _zoneManager = null;
	private int _totalUrlsSent = 0;
	private int _totalLoadersCreated = 0;
	private int _totalLoadersHaveRun = 0;
	
	private boolean _isLoading = false;
	private boolean _initInProgress = false;
	
	public static enum UnityAdsVideoPosition { Start, FirstQuartile, MidPoint, ThirdQuartile, End;
		@SuppressLint("DefaultLocale")
		@Override
		public String toString () {
			String output = null;
			switch (this) {
				case FirstQuartile:
					output = "first_quartile";
					break;
				case MidPoint:
					output = "mid_point";
					break;
				case ThirdQuartile:
					output = "third_quartile";
					break;
				case End:
					output = "video_end";
					break;
				case Start:
					output = "video_start";
					break;
				default:
					output = name().toString().toLowerCase();
					break;					
			}
			
			return output;
		}
	};
	
	private static enum UnityAdsRequestType { Analytics, VideoPlan, VideoViewed, Unsent;
		@SuppressLint("DefaultLocale")
		@Override
		public String toString () {
			String output = name().toString().toLowerCase();
			return output;
		}
		
		@SuppressLint("DefaultLocale")
		public static UnityAdsRequestType getValueOf (String value) {
			if (VideoPlan.toString().equals(value.toLowerCase()))
				return VideoPlan;
			else if (VideoViewed.toString().equals(value.toLowerCase()))
				return VideoViewed;
			else if (Unsent.toString().equals(value.toLowerCase()))
				return Unsent;
			
			return null;
		}
	};
	
	public UnityAdsWebData () {	
	}
	
	public void setWebDataListener (IUnityAdsWebDataListener listener) {
		_listener = listener;
	}
	
	public ArrayList<UnityAdsCampaign> getVideoPlanCampaigns () {
		return _campaigns;
	}
	
	public UnityAdsCampaign getCampaignById (String campaignId) {
		if (campaignId != null && _campaigns != null) {
			for (int i = 0; i < _campaigns.size(); i++) {
				if (_campaigns.get(i) != null && _campaigns.get(i).getCampaignId() != null && _campaigns.get(i).getCampaignId().equals(campaignId))
					return _campaigns.get(i);
			}
		}
		
		return null;
	}
	
	public ArrayList<UnityAdsCampaign> getViewableVideoPlanCampaigns () {
		ArrayList<UnityAdsCampaign> viewableCampaigns = null;
		UnityAdsCampaign currentCampaign = null; 
		
		if (_campaigns != null) {
			viewableCampaigns = new ArrayList<UnityAdsCampaign>();
			for (int i = 0; i < _campaigns.size(); i++) {
				currentCampaign = _campaigns.get(i);
				if (currentCampaign != null && !currentCampaign.getCampaignStatus().equals(UnityAdsCampaignStatus.VIEWED))
					viewableCampaigns.add(currentCampaign);
			}
		}
		
		return viewableCampaigns;
	}

	public boolean initCampaigns () {
		if(_initInProgress) {
			return true;
		}

		if (UnityAdsUtils.isDebuggable(UnityAdsProperties.getBaseActivity()) && UnityAdsProperties.TEST_DATA != null) {
			campaignDataReceived(UnityAdsProperties.TEST_DATA);
			return true;
		}

		_initInProgress = true;

		String url = UnityAdsProperties.getCampaignQueryUrl();
		String[] parts = url.split("\\?");
		
		UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(parts[0], parts[1], UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET, UnityAdsRequestType.VideoPlan, 0);
		if (UnityAdsProperties.getCurrentActivity() != null)
			UnityAdsProperties.getCurrentActivity().runOnUiThread(ulc);
		
		checkFailedUrls();			

		return true;
	}
	
	public boolean sendCampaignViewProgress (UnityAdsCampaign campaign, UnityAdsVideoPosition position) {
		boolean progressSent = false;
		if (campaign == null) return progressSent;

		UnityAdsUtils.Log("VP: " + position.toString() + ", " + UnityAdsProperties.UNITY_ADS_GAMER_ID, this);
		
		if (position != null && UnityAdsProperties.UNITY_ADS_GAMER_ID != null) {			
			String viewUrl = String.format("%s%s", UnityAdsProperties.UNITY_ADS_BASE_URL, UnityAdsConstants.UNITY_ADS_ANALYTICS_TRACKING_PATH);
			viewUrl = String.format("%s%s/video/%s/%s", viewUrl, UnityAdsProperties.UNITY_ADS_GAMER_ID, position.toString(), campaign.getCampaignId());
			viewUrl = String.format("%s/%s", viewUrl, UnityAdsProperties.UNITY_ADS_GAME_ID);
			
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			String queryParams = String.format("%s=%s", UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_ZONE_KEY, currentZone.getZoneId());
			
			try {
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICEID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(), "UTF-8"));
				
				if (!UnityAdsDevice.getAndroidId().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
					queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ANDROIDID_KEY, URLEncoder.encode(UnityAdsDevice.getAndroidId(), "UTF-8"));

				if (!UnityAdsDevice.getMacAddress().equals(UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN))
					queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_MACADDRESS_KEY, URLEncoder.encode(UnityAdsDevice.getMacAddress(), "UTF-8"));
				
				if(UnityAdsDevice.ADVERTISING_TRACKING_INFO != null) {
					queryParams = String.format("%s&%s=%d", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_TRACKINGENABLED_KEY, UnityAdsDevice.isLimitAdTrackingEnabled() ? 0 : 1);
					String rawAdvertisingTrackingId = UnityAdsDevice.getAdvertisingTrackingId();
					if(rawAdvertisingTrackingId != null) {
						String advertisingTrackingId = UnityAdsUtils.Md5(rawAdvertisingTrackingId).toLowerCase();
						queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_ADVERTISINGTRACKINGID_KEY, URLEncoder.encode(advertisingTrackingId, "UTF-8"));
						queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_RAWADVERTISINGTRACKINGID_KEY, URLEncoder.encode(rawAdvertisingTrackingId, "UTF-8"));					
					}
				}
				
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_PLATFORM_KEY, "android");
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_GAMEID_KEY, URLEncoder.encode(UnityAdsProperties.UNITY_ADS_GAME_ID, "UTF-8"));
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SDKVERSION_KEY, URLEncoder.encode(UnityAdsConstants.UNITY_ADS_VERSION, "UTF-8"));
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SOFTWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getSoftwareVersion(), "UTF-8"));
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_HARDWAREVERSION_KEY, URLEncoder.encode(UnityAdsDevice.getHardwareVersion(), "UTF-8"));
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_DEVICETYPE_KEY, UnityAdsDevice.getDeviceType());
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_CONNECTIONTYPE_KEY, URLEncoder.encode(UnityAdsDevice.getConnectionType(), "UTF-8"));
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENSIZE_KEY, UnityAdsDevice.getScreenSize());
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_INIT_QUERYPARAM_SCREENDENSITY_KEY, UnityAdsDevice.getScreenDensity());
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems creating campaigns query: " + e.getMessage() + e.getStackTrace().toString(), UnityAdsProperties.class);
			}
			
			if(currentZone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone)currentZone).itemManager();
			    queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_REWARDITEM_KEY, itemManager.getCurrentItem().getKey());
			}
			
			if (currentZone.getGamerSid() != null) {
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMERSID_KEY, currentZone.getGamerSid());
			}
			
			UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(viewUrl, queryParams, UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST, UnityAdsRequestType.VideoViewed, 0);
			if (UnityAdsProperties.getCurrentActivity() != null)
				UnityAdsProperties.getCurrentActivity().runOnUiThread(ulc);
			
			progressSent = true;
		}
		
		return progressSent;
	}
	
	public void sendAnalyticsRequest (String eventType, UnityAdsCampaign campaign) {
		if (campaign != null) {
			String viewUrl = String.format("%s",  UnityAdsProperties.ANALYTICS_BASE_URL);
			String analyticsUrl = String.format("%s=%s", UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMEID_KEY, UnityAdsProperties.UNITY_ADS_GAME_ID);
			analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_EVENTTYPE_KEY, eventType);
			analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_TRACKINGID_KEY, UnityAdsProperties.UNITY_ADS_GAMER_ID);
			analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_PROVIDERID_KEY, campaign.getCampaignId());
			
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_ZONE_KEY, currentZone.getZoneId());
			
			if(currentZone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone)currentZone).itemManager();
				analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_REWARDITEM_KEY, itemManager.getCurrentItem().getKey());
			}		
			
			if (currentZone.getGamerSid() != null)
				analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMERSID_KEY, currentZone.getGamerSid());
			
			UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(viewUrl, analyticsUrl, UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET, UnityAdsRequestType.Analytics, 0);
			if (UnityAdsProperties.getCurrentActivity() != null)
				UnityAdsProperties.getCurrentActivity().runOnUiThread(ulc);
		}
	}
	
	public void clearData () {
		if (_campaigns != null) {
			_campaigns.clear();
			_campaigns = null;
		}
		
		if (_zoneManager != null) {
			_zoneManager.clear();
			_zoneManager = null;
		}
		
		_campaignJson = null;
	}
	
	public void stopAllRequests () {
		if (_urlLoaders != null) {
			_urlLoaders.clear();
			_urlLoaders = null;
		}
		
		if (_failedUrlLoaders != null) {
			_failedUrlLoaders.clear();
			_failedUrlLoaders = null;
		}
		
		if (_currentLoader != null) {
			_currentLoader.cancel(true);
			_currentLoader = null;
		}
	}
	
	public JSONObject getData () {
		return _campaignJson;
	}
	
	public String getVideoPlan () {
		if (_campaignJson != null)
			return _campaignJson.toString();
		
		return null;
	}	
	
	public static UnityAdsZoneManager getZoneManager() {
		return _zoneManager;
	}
	
	/* INTERNAL METHODS */
	
	private void addLoader (UnityAdsUrlLoader loader) {
		if (_urlLoaders == null)
			_urlLoaders = new ArrayList<UnityAdsWebData.UnityAdsUrlLoader>();
		
		_urlLoaders.add(loader);
	}
	
	private void startNextLoader () {		
		if (_urlLoaders != null && _urlLoaders.size() > 0 && !_isLoading) {
			UnityAdsUtils.Log("Starting next URL loader", this);
			_isLoading = true;
			_currentLoader = (UnityAdsUrlLoader)_urlLoaders.remove(0).execute();
		}			
	}
	
	private void urlLoadCompleted (UnityAdsUrlLoader loader) {
		if (loader != null && loader.getRequestType() != null) {
			switch (loader.getRequestType()) {
				case VideoPlan:
					campaignDataReceived(loader.getData());
					break;
				case VideoViewed:
					break;
				case Unsent:
					break;
				case Analytics:
					break;
			}
			
			loader.clear();
		}
		else {
			UnityAdsUtils.Log("Got broken urlLoader!", this);
		}
		
		_totalUrlsSent++;
		
		UnityAdsUtils.Log("Total urls sent: " + _totalUrlsSent, this);
		
		_isLoading = false;
		startNextLoader();
	}
	
	private void urlLoadFailed (UnityAdsUrlLoader loader) {
		if (loader != null && loader.getRequestType() != null) {
			switch (loader.getRequestType()) {
				case Analytics:
				case VideoViewed:
				case Unsent:
					writeFailedUrl(loader);
					break;
				case VideoPlan:
					campaignDataFailed();
					break;
			}
			
			loader.clear();
		}
		else {
			UnityAdsUtils.Log("Got broken urlLoader!", this);
		}
		
		_isLoading = false;
		startNextLoader();
	}
	
	private void checkFailedUrls () {
		File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsConstants.PENDING_REQUESTS_FILENAME);
		
		if (pendingRequestFile.exists()) {
			String contents = UnityAdsUtils.readFile(pendingRequestFile, true);
			JSONObject pendingRequestsJson = null;
			JSONArray pendingRequestsArray = null;
			//UnityAdsUrlLoader loader = null;
			
			try {
				pendingRequestsJson = new JSONObject(contents);
				pendingRequestsArray = pendingRequestsJson.getJSONArray("data");
				
				if (pendingRequestsArray != null && pendingRequestsArray.length() > 0) {
					for (int i = 0; i < pendingRequestsArray.length(); i++) {
						JSONObject failedUrl = pendingRequestsArray.getJSONObject(i);
						
						UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_URL_KEY), 
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_BODY_KEY), 
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_METHODTYPE_KEY), 
								UnityAdsRequestType.getValueOf(failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_REQUESTTYPE_KEY)), 
								failedUrl.getInt(UnityAdsConstants.UNITY_ADS_FAILED_URL_RETRIES_KEY) + 1);
						
						if (UnityAdsProperties.getCurrentActivity() != null)
							UnityAdsProperties.getCurrentActivity().runOnUiThread(ulc);
					}
				}
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems while sending some of the failed urls.", this);
			}

			UnityAdsUtils.removeFile(pendingRequestFile.toString());
		}
		
		startNextLoader();
	}
	
	private void writeFailedUrl (UnityAdsUrlLoader loader) {
		if (loader == null) return;
		if (_failedUrlLoaders == null)
			_failedUrlLoaders = new ArrayList<UnityAdsWebData.UnityAdsUrlLoader>();
		
		if (!_failedUrlLoaders.contains(loader)) {
			_failedUrlLoaders.add(loader);
		}
		
		JSONObject failedUrlsJson = new JSONObject();
		JSONArray failedUrlsArray = new JSONArray();
		
		try {
			JSONObject failedUrl = null;
			for (UnityAdsUrlLoader failedLoader : _failedUrlLoaders) {
				failedUrl = new JSONObject();
				failedUrl.put(UnityAdsConstants.UNITY_ADS_FAILED_URL_URL_KEY, failedLoader.getBaseUrl());
				failedUrl.put(UnityAdsConstants.UNITY_ADS_FAILED_URL_REQUESTTYPE_KEY, failedLoader.getRequestType());
				failedUrl.put(UnityAdsConstants.UNITY_ADS_FAILED_URL_METHODTYPE_KEY, failedLoader.getHTTPMethod());
				failedUrl.put(UnityAdsConstants.UNITY_ADS_FAILED_URL_BODY_KEY, failedLoader.getQueryParams());				
				failedUrl.put(UnityAdsConstants.UNITY_ADS_FAILED_URL_RETRIES_KEY, failedLoader.getRetries());
				
				failedUrlsArray.put(failedUrl);
			}
			
			failedUrlsJson.put("data", failedUrlsArray);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Error collecting failed urls", this);
		}
		
		if (_failedUrlLoaders != null && _failedUrlLoaders.size() > 0 && UnityAdsUtils.canUseExternalStorage()) {
			File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsConstants.PENDING_REQUESTS_FILENAME);
			UnityAdsUtils.writeFile(pendingRequestFile, failedUrlsJson.toString());
		}
	}
	
	private void campaignDataReceived (String json) {
		Boolean validData = true;

		_initInProgress = false;

		try {
			_campaignJson = new JSONObject(json);
			JSONObject data = null;
			
			if (_campaignJson.has(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY)) {
				try {
					data = _campaignJson.getJSONObject(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Malformed data JSON", this);
				}
				
				if (!data.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_URL_KEY)) validData = false;
				if (!data.has(UnityAdsConstants.UNITY_ADS_ANALYTICS_URL_KEY)) validData = false;
				if (!data.has(UnityAdsConstants.UNITY_ADS_URL_KEY)) validData = false;
				if (!data.has(UnityAdsConstants.UNITY_ADS_GAMER_ID_KEY)) validData = false;
				if (!data.has(UnityAdsConstants.UNITY_ADS_CAMPAIGNS_KEY)) validData = false;
				if (!data.has(UnityAdsConstants.UNITY_ADS_ZONES_KEY)) validData = false;
				
				// Parse basic properties
				UnityAdsProperties.WEBVIEW_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_URL_KEY);
				UnityAdsProperties.ANALYTICS_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_ANALYTICS_URL_KEY);
				UnityAdsProperties.UNITY_ADS_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_URL_KEY);
				UnityAdsProperties.UNITY_ADS_GAMER_ID = data.getString(UnityAdsConstants.UNITY_ADS_GAMER_ID_KEY);
				
				// Refresh campaigns after "n" endscreens
				if (data.has(UnityAdsConstants.UNITY_ADS_CAMPAIGN_REFRESH_VIEWS_KEY)) {
					UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_COUNT = 0;
					UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_MAX = data.getInt(UnityAdsConstants.UNITY_ADS_CAMPAIGN_REFRESH_VIEWS_KEY);
				}
				
				// Refresh campaigns after "n" seconds
				if (data.has(UnityAdsConstants.UNITY_ADS_CAMPAIGN_REFRESH_SECONDS_KEY)) {
					UnityAdsProperties.CAMPAIGN_REFRESH_SECONDS = data.getInt(UnityAdsConstants.UNITY_ADS_CAMPAIGN_REFRESH_SECONDS_KEY);
				}
				
				// Parse campaigns
				if (validData) {
					JSONArray campaigns = data.getJSONArray(UnityAdsConstants.UNITY_ADS_CAMPAIGNS_KEY);
					if (campaigns != null)
						_campaigns = deserializeCampaigns(campaigns);
				}
				
				// Fall back, if campaigns were not found just set it to size 0
				if (_campaigns == null)
					_campaigns = new ArrayList<UnityAdsCampaign>();
				
				UnityAdsUtils.Log("Parsed total of " + _campaigns.size() + " campaigns", this);
				
				// Zone parsing
				if (validData) {
					if(_zoneManager != null) {
						_zoneManager.clear();
						_zoneManager = null;
					}
					_zoneManager = new UnityAdsZoneManager(data.getJSONArray(UnityAdsConstants.UNITY_ADS_ZONES_KEY));
				}
			}
			else {
				campaignDataFailed();
				return;
			}
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Malformed JSON: " + e.getMessage(), this);
			
			if (e.getStackTrace() != null) {
				for (StackTraceElement element : e.getStackTrace()) {
					UnityAdsUtils.Log("Malformed JSON: " + element.toString(), this);
				}
			}
			
			campaignDataFailed();
			return;
		}
		
		if (_listener != null && validData && _campaigns != null && _campaigns.size() > 0) {
			UnityAdsUtils.Log("WebDataCompleted: " + json, this);
			_listener.onWebDataCompleted();
			return;
		}
		else {
			campaignDataFailed();
			return;
		}
	}
	
	private void campaignDataFailed () {
		if (_listener != null)
			_listener.onWebDataFailed();		
	}
	
	private ArrayList<UnityAdsCampaign> deserializeCampaigns (JSONArray campaignsArray) {
		if (campaignsArray != null && campaignsArray.length() > 0) {			
			UnityAdsCampaign campaign = null;
			ArrayList<UnityAdsCampaign> retList = new ArrayList<UnityAdsCampaign>();
			
			for (int i = 0; i < campaignsArray.length(); i++) {
				try {
					JSONObject jsonCampaign = campaignsArray.getJSONObject(i);
					campaign = new UnityAdsCampaign(jsonCampaign);
					
					if (campaign.hasValidData()) {
						UnityAdsUtils.Log("Adding campaign to cache", this);
						retList.add(campaign);
					}
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problem with the campaign, skipping.", this);
				}
			}
			
			return retList;
		}
		
		return null;
	}
	
	
	/* INTERNAL CLASSES */
	
	private class UnityAdsUrlLoaderCreator implements Runnable {
		private String _url = null;
		private String _queryParams = null;
		private String _requestMethod = null;
		private UnityAdsRequestType _requestType = null;
		private int _retries = 0;
		
		public UnityAdsUrlLoaderCreator (String urlPart1, String urlPart2, String requestMethod, UnityAdsRequestType requestType, int retries) {
			_url = urlPart1;
			_queryParams = urlPart2;
			_requestMethod = requestMethod;
			_requestType = requestType;
			_retries = retries;
		}
		public void run () {
			UnityAdsUrlLoader loader = new UnityAdsUrlLoader(_url, _queryParams, _requestMethod, _requestType, _retries);
			UnityAdsUtils.Log("URL: " + loader.getUrl(), this);
			
			if (_retries <= UnityAdsProperties.MAX_NUMBER_OF_ANALYTICS_RETRIES)
				addLoader(loader);
			
			startNextLoader();
		}
	}
	
	private class UnityAdsCancelUrlLoaderRunner implements Runnable {
		private UnityAdsUrlLoader _loader = null;
		public UnityAdsCancelUrlLoaderRunner (UnityAdsUrlLoader loader) {
			_loader = loader;
		}
		public void run () {
			try {
				_loader.cancel(true);
				//_loader.clear();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Cancelling urlLoader got exception: " + e.getMessage(), this);
			}
		}
	}
	
	private class UnityAdsUrlLoader extends AsyncTask<String, Integer, String> {
		private URL _url = null;
		private HttpURLConnection _connection = null;
		private int _downloadLength = 0;
		private InputStream _input = null;
		private BufferedInputStream _binput = null;
		private String _urlData = "";
		private UnityAdsRequestType _requestType = null;
		private String _finalUrl = null;
		private int _retries = 0;
		private String _httpMethod = UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET;
		private String _queryParams = null;
		private String _baseUrl = null;
		private Boolean _done = false;
		
		public UnityAdsUrlLoader (String url, String queryParams, String httpMethod, UnityAdsRequestType requestType, int existingRetries) {
			super();
			try {
				_finalUrl = url;
				_baseUrl = url;
				
				if (httpMethod.equals(UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET) && queryParams != null && queryParams.length() > 2) {
					_finalUrl += "?" + queryParams;
				}
				
				_url = new URL(_finalUrl);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems with url! Error-message: " + e.getMessage(), this);
			}
			
			_queryParams = queryParams;
			_httpMethod = httpMethod;
			_totalLoadersCreated++;
			UnityAdsUtils.Log("Total urlLoaders created: " + _totalLoadersCreated, this);
			_requestType = requestType;
			_retries = existingRetries;
		}
		
		public int getRetries () {
			return _retries;
		}
		
		public String getUrl () {
			return _url.toString();
		}
		
		public String getBaseUrl () {
			return _baseUrl;
		}
		
		public String getData () {
			return _urlData;
		}
		
		public String getQueryParams () {
			return _queryParams;
		}
		
		public String getHTTPMethod () {
			return _httpMethod;
		}
		
		public UnityAdsRequestType getRequestType () {
			return _requestType;
		}
		
		public void clear () {
			_url = null;
			_downloadLength = 0;
			_urlData = "";
			_requestType = null;
			_finalUrl = null;
			_retries = 0;
			_httpMethod = null;
			_queryParams = null;
			_baseUrl = null;
		}
		
		private void cancelInMainThread () {
			if (UnityAdsProperties.getCurrentActivity() != null)
				UnityAdsProperties.getCurrentActivity().runOnUiThread(new UnityAdsCancelUrlLoaderRunner(this));
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				if (_url.toString().startsWith("https://")) {
					_connection = (HttpsURLConnection)_url.openConnection();
				}
				else {
					_connection = (HttpURLConnection)_url.openConnection();
				}

				_connection.setConnectTimeout(20000);
				_connection.setReadTimeout(30000);
				_connection.setRequestMethod(_httpMethod);
				_connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
				_connection.setDoInput(true);
				
				if (_httpMethod.equals(UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST))
					_connection.setDoOutput(true);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems opening connection: " + e.getMessage(), this);
				cancelInMainThread();
				return null;
			}
			
			if (_connection != null) {				
				if (_httpMethod.equals(UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST)) {
					try {
						PrintWriter pout = new PrintWriter(new OutputStreamWriter(_connection.getOutputStream(), "UTF-8"), true);
						pout.print(_queryParams);
						pout.flush();
					}
					catch (Exception e) {
						UnityAdsUtils.Log("Problems writing post-data: " + e.getMessage() + ", " + e.getStackTrace(), this);
						cancelInMainThread();
						return null;
					}
				}
				
				try {
					UnityAdsUtils.Log("Connection response: " + _connection.getResponseCode() + ", " + _connection.getResponseMessage() + ", " + _connection.getURL().toString() + " : " + _queryParams, this);
					_input = _connection.getInputStream();
					_binput = new BufferedInputStream(_input);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems opening stream: " + e.getMessage(), this);
					cancelInMainThread();
					return null;
				}
				
				long total = 0;
				_downloadLength = _connection.getContentLength();
				
				try {
					_totalLoadersHaveRun++;
					UnityAdsUtils.Log("Total urlLoaders that have started running: " + _totalLoadersHaveRun, this);
					UnityAdsUtils.Log("Reading data from: " + _url.toString() + " Content-length: " + _downloadLength, this);
					
					ByteArrayBuffer baf = new ByteArrayBuffer(1024 * 20);
					int current = 0;
					
					while ((current = _binput.read()) != -1) {
						total++;
						baf.append((byte)current);
						
						if (isCancelled())
							return null;
					}
					
					_urlData = new String(baf.toByteArray());
					UnityAdsUtils.Log("Read total of: " + total, this);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems loading url! Error-message: " + e.getMessage(), this);
					cancelInMainThread();
					return null;
				}
			}
			
			return null;
		}

		@Override
		protected void onCancelled() {
			_done = true;
			closeAndFlushConnection();
			urlLoadFailed(this);
		}

		@Override
		protected void onPostExecute(String result) {
			if (!isCancelled() && !_done) {
				_done = true;
				closeAndFlushConnection();
				urlLoadCompleted(this);
 			}
			
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}
		
		private void closeAndFlushConnection () {
			try {
				_input.close();
				_input = null;
				_binput.close();
				_binput = null;
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems closing streams: " + e.getMessage(), this);
			}	
		}
	}
}
