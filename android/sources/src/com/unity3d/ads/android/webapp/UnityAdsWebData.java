package com.unity3d.ads.android.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.campaign.UnityAdsRewardItem;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;

public class UnityAdsWebData {
	
	private JSONObject _campaignJson = null;
	private ArrayList<UnityAdsCampaign> _campaigns = null;
	private IUnityAdsWebDataListener _listener = null;
	private ArrayList<UnityAdsUrlLoader> _urlLoaders = null;
	private ArrayList<UnityAdsUrlLoader> _failedUrlLoaders = null;
	private UnityAdsUrlLoader _currentLoader = null;
	private UnityAdsRewardItem _defaultRewardItem = null;
	private ArrayList<UnityAdsRewardItem> _rewardItems = null;
	private UnityAdsRewardItem _currentRewardItem = null;
	private int _totalUrlsSent = 0;
	private int _totalLoadersCreated = 0;
	private int _totalLoadersHaveRun = 0;
	
	private boolean _isLoading = false;
	
	public static enum UnityAdsVideoPosition { Start, FirstQuartile, MidPoint, ThirdQuartile, End;
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
		@Override
		public String toString () {
			String output = name().toString().toLowerCase();
			return output;
		}
		
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
		if (UnityAdsUtils.isDebuggable(UnityAdsProperties.BASE_ACTIVITY) && UnityAdsProperties.TEST_DATA != null) {
			campaignDataReceived(UnityAdsProperties.TEST_DATA);
			return true;
		}
		
		String url = UnityAdsProperties.getCampaignQueryUrl();
		String[] parts = url.split("\\?");
		
		UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(parts[0], parts[1], UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET, UnityAdsRequestType.VideoPlan, 0);
		if (UnityAdsProperties.CURRENT_ACTIVITY != null)
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(ulc);
		
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
			
			String queryParams = String.format("%s=%s", UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_REWARDITEM_KEY, getCurrentRewardItemKey());
			
			if (UnityAdsProperties.GAMER_SID != null)
				queryParams = String.format("%s&%s=%s", queryParams, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMERSID_KEY, UnityAdsProperties.GAMER_SID);
			
			UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(viewUrl, queryParams, UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST, UnityAdsRequestType.VideoViewed, 0);
			if (UnityAdsProperties.CURRENT_ACTIVITY != null)
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(ulc);
			
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
			analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_REWARDITEM_KEY, getCurrentRewardItemKey());
			
			if (UnityAdsProperties.GAMER_SID != null)
				analyticsUrl = String.format("%s&%s=%s", analyticsUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMERSID_KEY, UnityAdsProperties.GAMER_SID);
			
			UnityAdsUrlLoaderCreator ulc = new UnityAdsUrlLoaderCreator(viewUrl, analyticsUrl, UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET, UnityAdsRequestType.Analytics, 0);
			if (UnityAdsProperties.CURRENT_ACTIVITY != null)
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(ulc);
		}
	}
	
	public void clearData () {
		if (_campaigns != null) {
			_campaigns.clear();
			_campaigns = null;
		}
		
		if (_defaultRewardItem != null) {
			_defaultRewardItem.clearData();
			_defaultRewardItem = null;
		}
		
		if (_rewardItems != null) {
			for (UnityAdsRewardItem rewardItem : _rewardItems)
				rewardItem.clearData();
			
			_rewardItems.clear();
			_rewardItems = null;
		}
		
		if (_currentRewardItem != null) {
			_currentRewardItem.clearData();
			_currentRewardItem = null;
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
	
	
	// Multiple reward items
	
	public ArrayList<UnityAdsRewardItem> getRewardItems () {
		return _rewardItems;
	}
	
	public UnityAdsRewardItem getDefaultRewardItem () {
		return _defaultRewardItem;
	}
	
	public String getCurrentRewardItemKey () {
		if (_currentRewardItem != null)
			return _currentRewardItem.getKey();
		
		return null;
	}
	
	public UnityAdsRewardItem getRewardItemByKey (String rewardItemKey) {
		if (_rewardItems != null) {
			for (UnityAdsRewardItem rewardItem : _rewardItems) {
				if (rewardItem.getKey().equals(rewardItemKey))
					return rewardItem;
			}
		}
		
		if (_defaultRewardItem != null && _defaultRewardItem.getKey().equals(rewardItemKey))
			return _defaultRewardItem;
		
		return null;
	}
	
	public void setCurrentRewardItem (UnityAdsRewardItem rewardItem) {
		if (_currentRewardItem != null && !_currentRewardItem.equals(_currentRewardItem))
			_currentRewardItem = rewardItem;
	}
	
	
	/* INTERNAL METHODS */
	
	private void addLoader (UnityAdsUrlLoader loader) {
		if (_urlLoaders == null)
			_urlLoaders = new ArrayList<UnityAdsWebData.UnityAdsUrlLoader>();
		
		_urlLoaders.add(loader);
	}
	
	private void startNextLoader () {		
		if (_urlLoaders.size() > 0 && !_isLoading) {
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
						
						if (UnityAdsProperties.CURRENT_ACTIVITY != null)
							UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(ulc);
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
		
		if (_failedUrlLoaders != null && _failedUrlLoaders.size() > 0) {
			File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsConstants.PENDING_REQUESTS_FILENAME);
			UnityAdsUtils.writeFile(pendingRequestFile, failedUrlsJson.toString());
		}
	}
	
	private void campaignDataReceived (String json) {
		Boolean validData = true;
		
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
				if (!data.has(UnityAdsConstants.UNITY_ADS_REWARD_ITEM_KEY)) validData = false;
				
				// Parse basic properties
				UnityAdsProperties.WEBVIEW_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_URL_KEY);
				UnityAdsProperties.ANALYTICS_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_ANALYTICS_URL_KEY);
				UnityAdsProperties.UNITY_ADS_BASE_URL = data.getString(UnityAdsConstants.UNITY_ADS_URL_KEY);
				UnityAdsProperties.UNITY_ADS_GAMER_ID = data.getString(UnityAdsConstants.UNITY_ADS_GAMER_ID_KEY);
				
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
				
				// Parse default reward item
				if (validData) {
					_defaultRewardItem = new UnityAdsRewardItem(data.getJSONObject(UnityAdsConstants.UNITY_ADS_REWARD_ITEM_KEY));
					if (!_defaultRewardItem.hasValidData()) {
						campaignDataFailed();
						return;
					}
					
					UnityAdsUtils.Log("Parsed default rewardItem: " + _defaultRewardItem.getName() + ", " + _defaultRewardItem.getKey(), this);
					_currentRewardItem = _defaultRewardItem;
				}
				
				// Parse possible multiple reward items
				if (validData && data.has(UnityAdsConstants.UNITY_ADS_REWARD_ITEMS_KEY)) {
					JSONArray rewardItems = data.getJSONArray(UnityAdsConstants.UNITY_ADS_REWARD_ITEMS_KEY);
					UnityAdsRewardItem currentRewardItem = null;
					
					for (int i = 0; i < rewardItems.length(); i++) {
						currentRewardItem = new UnityAdsRewardItem(rewardItems.getJSONObject(i));
						if (currentRewardItem.hasValidData()) {
							if (_rewardItems == null)
								_rewardItems = new ArrayList<UnityAdsRewardItem>();
							
							_rewardItems.add(currentRewardItem);
						}
					}
					
					UnityAdsUtils.Log("Parsed total of " + _rewardItems.size() + " reward items", this);
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
			if (UnityAdsProperties.CURRENT_ACTIVITY != null)
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new UnityAdsCancelUrlLoaderRunner(this));
		}
		
		@Override
		protected String doInBackground(String... params) {
			Boolean panicCancel = false;

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
				panicCancel = true;
			}
			
			if (panicCancel) {
				cancelInMainThread();
				panicCancel = false;
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
						panicCancel = true;
					}
					
					if (panicCancel) {
						cancelInMainThread();
						panicCancel = false;
					}
				}
				
				try {
					UnityAdsUtils.Log("Connection response: " + _connection.getResponseCode() + ", " + _connection.getResponseMessage() + ", " + _connection.getURL().toString() + " : " + _queryParams, this);
					_input = _connection.getInputStream();
					_binput = new BufferedInputStream(_input);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems opening stream: " + e.getMessage(), this);
					panicCancel = true;
				}
				
				if (panicCancel) {
					cancelInMainThread();
					panicCancel = false;
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
					panicCancel = true;
					return null;
				}
				
				if (panicCancel) {
					cancelInMainThread();
					panicCancel = false;
				}
			}
			
			return null;
		}

		protected void onCancelled(Object result) {
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
