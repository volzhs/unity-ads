package com.unity3d.ads.android.webapp;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

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
	
	private static enum UnityAdsRequestType { VideoPlan, VideoViewed, Unsent;
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
		if (campaignId != null) {
			for (int i = 0; i < _campaigns.size(); i++) {
				if (_campaigns.get(i).getCampaignId().equals(campaignId))
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
		String url = UnityAdsProperties.getCampaignQueryUrl();
		
		String[] parts = url.split("\\?");
		
		UnityAdsUtils.Log(parts.toString(), this);
		
		UnityAdsUrlLoader loader = new UnityAdsUrlLoader(parts[0], parts[1], UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET, UnityAdsRequestType.VideoPlan, 0);
		UnityAdsUtils.Log("VIDEOPLAN_URL: " + loader.getUrl(), this);
		addLoader(loader);
		startNextLoader();
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
			
			UnityAdsUrlLoader loader = new UnityAdsUrlLoader(viewUrl, queryParams, UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST, UnityAdsRequestType.VideoViewed, 0);
			addLoader(loader);
			startNextLoader();
			progressSent = true;
		}
		
		return progressSent;
	}
	
	public void stopAllRequests () {
		_urlLoaders.clear();
		
		if (_currentLoader != null)
			_currentLoader.cancel(true);
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
		switch (loader.getRequestType()) {
			case VideoPlan:
				campaignDataReceived(loader.getData());
				break;
			case VideoViewed:
				break;
			case Unsent:
				break;
		}
		
		_totalUrlsSent++;
		
		UnityAdsUtils.Log("Total urls sent: " + _totalUrlsSent, this);
		
		_isLoading = false;
		startNextLoader();
	}
	
	private void urlLoadFailed (UnityAdsUrlLoader loader) {
		switch (loader.getRequestType()) {
			case VideoViewed:
			case Unsent:
				writeFailedUrl(loader);
				break;
			case VideoPlan:
				campaignDataFailed();
				break;
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
			UnityAdsUrlLoader loader = null;
			
			try {
				pendingRequestsJson = new JSONObject(contents);
				pendingRequestsArray = pendingRequestsJson.getJSONArray("data");
				
				if (pendingRequestsArray != null && pendingRequestsArray.length() > 0) {
					for (int i = 0; i < pendingRequestsArray.length(); i++) {
						JSONObject failedUrl = pendingRequestsArray.getJSONObject(i);
						loader = new UnityAdsUrlLoader(
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_URL_KEY), 
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_BODY_KEY),
								failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_METHODTYPE_KEY),
								UnityAdsRequestType.getValueOf(failedUrl.getString(UnityAdsConstants.UNITY_ADS_FAILED_URL_REQUESTTYPE_KEY)), 
								failedUrl.getInt(UnityAdsConstants.UNITY_ADS_FAILED_URL_RETRIES_KEY) + 1
								);
						
						if (loader.getRetries() <= UnityAdsProperties.MAX_NUMBER_OF_ANALYTICS_RETRIES)
							addLoader(loader);
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
				
				UnityAdsUtils.Log("Parsed total of " + _campaigns.size() + " campaigns", this);

				
				// Parse default reward item
				if (validData) {
					_defaultRewardItem = new UnityAdsRewardItem(data.getJSONObject(UnityAdsConstants.UNITY_ADS_REWARD_ITEM_KEY));
					if (!_defaultRewardItem.hasValidData()) {
						campaignDataFailed();
						return;
					}
					
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
			UnityAdsUtils.Log("Malformed JSON: " + json, this);
			campaignDataFailed();
			return;
		}
			
		if (_campaigns != null)
			UnityAdsUtils.Log(_campaigns.toString(), this);
		
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
	
	private class UnityAdsUrlLoader extends AsyncTask<String, Integer, String> {
		private URL _url = null;
		private HttpURLConnection _connection = null;
		private int _downloadLength = 0;
		private InputStream _input = null;
		private String _urlData = "";
		private UnityAdsRequestType _requestType = null;
		private String _finalUrl = null;
		private int _retries = 0;
		private String _httpMethod = UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_GET;
		private String _queryParams = null;
		private String _baseUrl = null;
		
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
				UnityAdsUtils.Log("Problems with url: " + e.getMessage(), this);
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
		
		@Override
		protected String doInBackground(String... params) {
			try {
				if (_url.toString().startsWith("https://")) {
					_connection = (HttpsURLConnection)_url.openConnection();
				}
				else {
					_connection = (HttpURLConnection)_url.openConnection();
				}
				
				_connection.setConnectTimeout(10000);
				_connection.setReadTimeout(10000);
				_connection.setRequestMethod(_httpMethod);
				_connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
				_connection.setDoInput(true);
				
				if (_httpMethod.equals(UnityAdsConstants.UNITY_ADS_REQUEST_METHOD_POST))
					_connection.setDoOutput(true);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems opening connection: " + e.getMessage(), this);
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
					}
				}
				
				try {
					UnityAdsUtils.Log("Connection response: " + _connection.getResponseCode() + ", " + _connection.getResponseMessage() + ", " + _connection.getURL().toString(), this);
					_input = _connection.getInputStream();
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems opening stream: " + e.getMessage(), this);
				}
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					_totalLoadersHaveRun++;
					_downloadLength = _connection.getContentLength();
					UnityAdsUtils.Log("Total urlLoaders that have started running: " + _totalLoadersHaveRun, this);
					UnityAdsUtils.Log("Reading data from: " + _url.toString(), this);
					while ((count = _input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / _downloadLength));
						_urlData = _urlData.concat(new String(data));
						
						if (isCancelled())
							return null;
					}
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems loading url: " + e.getMessage(), this);
					cancel(true);
					return null;
				}
			}
			
			return null;
		}

		protected void onCancelled(Object result) {
			closeAndFlushConnection();
			urlLoadFailed(this);
		}

		@Override
		protected void onPostExecute(String result) {
			if (!isCancelled()) {
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
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems closing connection: " + e.getMessage(), this);
			}	
		}
	}
}
