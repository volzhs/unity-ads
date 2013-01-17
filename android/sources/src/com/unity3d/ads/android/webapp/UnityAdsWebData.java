package com.unity3d.ads.android.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;

public class UnityAdsWebData {
	
	private JSONObject _videoPlan = null;
	private ArrayList<UnityAdsCampaign> _videoPlanCampaigns = null;
	private IUnityAdsWebDataListener _listener = null;
	private ArrayList<UnityAdsUrlLoader> _urlLoaders = null;
	private ArrayList<UnityAdsUrlLoader> _failedUrlLoaders = null;
	private UnityAdsUrlLoader _currentLoader = null;
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
					output = "view";
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
		return _videoPlanCampaigns;
	}
	
	public UnityAdsCampaign getCampaignById (String campaignId) {
		if (campaignId != null) {
			for (int i = 0; i < _videoPlanCampaigns.size(); i++) {
				if (_videoPlanCampaigns.get(i).getCampaignId().equals(campaignId))
					return _videoPlanCampaigns.get(i);
			}
		}
		
		return null;
	}
	
	public ArrayList<UnityAdsCampaign> getViewableVideoPlanCampaigns () {
		ArrayList<UnityAdsCampaign> viewableCampaigns = null;
		UnityAdsCampaign currentCampaign = null; 
		
		if (_videoPlanCampaigns != null) {
			viewableCampaigns = new ArrayList<UnityAdsCampaign>();
			for (int i = 0; i < _videoPlanCampaigns.size(); i++) {
				currentCampaign = _videoPlanCampaigns.get(i);
				if (currentCampaign != null && !currentCampaign.getCampaignStatus().equals(UnityAdsCampaignStatus.VIEWED))
					viewableCampaigns.add(currentCampaign);
			}
		}
		
		return viewableCampaigns;
	}

	public boolean initVideoPlan () {
		String url = UnityAdsProperties.getCampaignQueryUrl();
		UnityAdsUrlLoader loader = new UnityAdsUrlLoader(url, UnityAdsRequestType.VideoPlan);
		Log.d(UnityAdsConstants.LOG_NAME, "VIDEOPLAN_URL: " + loader.getUrl());
		addLoader(loader);
		startNextLoader();
		//checkFailedUrls();
		
		return true;
	}
	
	public boolean sendCampaignViewProgress (UnityAdsCampaign campaign, UnityAdsVideoPosition position) {
		if (campaign == null) return false;

		Log.d(UnityAdsConstants.LOG_NAME, "VP: " + position.toString() + ", " + getGamerId());
		
		if (position != null && getGamerId() != null && (position.equals(UnityAdsVideoPosition.Start)  || position.equals(UnityAdsVideoPosition.End))) {
			
			String viewUrl = String.format("%s%s", UnityAdsProperties.UNITY_ADS_BASE_URL, UnityAdsConstants.UNITY_ADS_ANALYTICS_TRACKING_PATH);
			viewUrl = String.format("%s/%s/%s/%s", viewUrl, UnityAdsProperties.UNITY_ADS_GAMER_ID, position.toString(), campaign.getCampaignId());
			viewUrl = String.format("%s?%s=%s", viewUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_GAMEID_KEY, UnityAdsProperties.UNITY_ADS_GAME_ID);
			viewUrl = String.format("%s&%s=%s", viewUrl, UnityAdsConstants.UNITY_ADS_ANALYTICS_QUERYPARAM_REWARDITEM_KEY, getCurrentRewardItemKey());
			UnityAdsUrlLoader loader = new UnityAdsUrlLoader(viewUrl, UnityAdsRequestType.VideoViewed);
			addLoader(loader);
			startNextLoader();
			return true;
		}
		
		return false;
	}
	
	public void stopAllRequests () {
		_urlLoaders.clear();
		
		if (_currentLoader != null)
			_currentLoader.cancel(true);
	}
	
	public JSONObject getData () {
		return _videoPlan;
	}
	
	public String getVideoPlan () {
		if (_videoPlan != null)
			return _videoPlan.toString();
		
		return null;
	}
	
	public String getGamerId () {
		if (_videoPlan != null) {
			if (_videoPlan.has("data")) {				
				JSONObject dataObj = null;
				try {
					dataObj = _videoPlan.getJSONObject("data");
				}
				catch (Exception e) {
					Log.d(UnityAdsConstants.LOG_NAME, "Malformed JSON");
					return null;
				}
				
				if (dataObj != null) {
					try {						
						return dataObj.getString("gamerId");
					}
					catch (Exception e) {
						Log.d(UnityAdsConstants.LOG_NAME, "Malformed JSON");
					}
				}
			}
		}
			
		return null;
	}
	
	public String getCurrentRewardItemKey () {
		return "currentReward";
	}
	
	
	/* INTERNAL METHODS */
	
	private void addLoader (UnityAdsUrlLoader loader) {
		if (_urlLoaders == null)
			_urlLoaders = new ArrayList<UnityAdsWebData.UnityAdsUrlLoader>();
		
		_urlLoaders.add(loader);
	}
	
	private void startNextLoader () {
		if (_urlLoaders.size() > 0 && !_isLoading) {
			_isLoading = true;
			_currentLoader = (UnityAdsUrlLoader)_urlLoaders.remove(0).execute();
		}			
	}
	
	private void urlLoadCompleted (UnityAdsUrlLoader loader) {
		switch (loader.getRequestType()) {
			case VideoPlan:
				videoPlanReceived(loader.getData());
				break;
			case VideoViewed:
				break;
			case Unsent:
				break;
		}
		
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
				videoPlanFailed();
				break;
		}
		
		_isLoading = false;
		startNextLoader();
	}
	
	private void checkFailedUrls () {
		File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsConstants.PENDING_REQUESTS_FILENAME);
		
		if (pendingRequestFile.exists()) {
			String contents = UnityAdsUtils.readFile(pendingRequestFile, true);
			String[] failedUrls = contents.split("\\r?\\n");
			String[] splittedLine = null;
			
			for (String line : failedUrls) {
				splittedLine = line.split("  ");
				UnityAdsUrlLoader loader = new UnityAdsUrlLoader(splittedLine[0], UnityAdsRequestType.getValueOf(splittedLine[1]));
				addLoader(loader);
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
		
		String fileContent = "";
		
		for (UnityAdsUrlLoader failedLoader : _failedUrlLoaders) {
			fileContent = fileContent.concat(failedLoader.getUrl() + "  " + failedLoader.getRequestType().toString() + "\n");
		}
		
		File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsConstants.PENDING_REQUESTS_FILENAME);
		UnityAdsUtils.writeFile(pendingRequestFile, fileContent);
	}
	
	private void videoPlanReceived (String json) {
		Boolean validData = true;
		
		try {
			_videoPlan = new JSONObject(json);
			JSONObject data = null;
			
			if (_videoPlan.has(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY)) {
				try {
					data = _videoPlan.getJSONObject(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY);
				}
				catch (Exception e) {
					Log.d(UnityAdsConstants.LOG_NAME, "Malformed data JSON");
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
						_videoPlanCampaigns = deserializeCampaigns(campaigns);
				}
			}
			else {
				videoPlanFailed();
				return;
			}
		}
		catch (Exception e) {
			Log.d(UnityAdsConstants.LOG_NAME, "Malformed JSON!");
			videoPlanFailed();
			return;
		}
			
		if (_videoPlanCampaigns != null)
			Log.d(UnityAdsConstants.LOG_NAME, _videoPlanCampaigns.toString());
		
		if (_listener != null && validData && _videoPlanCampaigns != null && _videoPlanCampaigns.size() > 0) {
			Log.d(UnityAdsConstants.LOG_NAME, "WebDataCompleted: " + json);
			_listener.onWebDataCompleted();
			return;
		}
		else {
			videoPlanFailed();
			return;
		}
	}
	
	private void videoPlanFailed () {
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
						Log.d(UnityAdsConstants.LOG_NAME, "Adding campaign to cache");
						retList.add(campaign);
					}
				}
				catch (Exception e) {
					Log.d(UnityAdsConstants.LOG_NAME, "Problem with the campaign, skipping.");
				}
			}
			
			return retList;
		}
		
		return null;
	}
	
	
	/* INTERNAL CLASSES */
	
	private class UnityAdsUrlLoader extends AsyncTask<String, Integer, String> {
		private URL _url = null;
		private URLConnection _urlConnection = null;
		private int _downloadLength = 0;
		private InputStream _input = null;
		private String _urlData = "";
		private UnityAdsRequestType _requestType = null;
		
		public UnityAdsUrlLoader (String url, UnityAdsRequestType requestType) {
			super();
			try {
				_url = new URL(url);
			}
			catch (Exception e) {
				Log.d(UnityAdsConstants.LOG_NAME, "Problems with url: " + e.getMessage());
			}
			_requestType = requestType;
		}
		
		public String getUrl () {
			return _url.toString();
		}
		
		public String getData () {
			return _urlData;
		}
		
		public UnityAdsRequestType getRequestType () {
			return _requestType;
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				_urlConnection = _url.openConnection();
				_urlConnection.setConnectTimeout(10000);
				_urlConnection.setReadTimeout(10000);
				_urlConnection.connect();
			}
			catch (Exception e) {
				Log.d(UnityAdsConstants.LOG_NAME, "Problems opening connection: " + e.getMessage());
			}
			
			if (_urlConnection != null) {
				_downloadLength = _urlConnection.getContentLength();
				
				try {
					_input = new BufferedInputStream(_url.openStream());
				}
				catch (Exception e) {
					Log.d(UnityAdsConstants.LOG_NAME, "Problems opening stream: " + e.getMessage());
				}
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					Log.d(UnityAdsConstants.LOG_NAME, "Reading data from: " + _url.toString());
					while ((count = _input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / _downloadLength));
						_urlData = _urlData.concat(new String(data));
						
						if (isCancelled())
							return null;
					}
				}
				catch (Exception e) {
					Log.d(UnityAdsConstants.LOG_NAME, "Problems loading url: " + e.getMessage());
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
				Log.d(UnityAdsConstants.LOG_NAME, "Problems closing connection: " + e.getMessage());
			}	
		}
	}
}
