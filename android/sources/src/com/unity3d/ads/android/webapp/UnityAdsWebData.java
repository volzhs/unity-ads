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

import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;

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
		return _videoPlanCampaigns;
	}

	public boolean initVideoPlan (ArrayList<String> cachedCampaignIds) {
		JSONObject json = new JSONObject();
		JSONArray data = getCachedCampaignIdsArray(cachedCampaignIds);
		String dataString = "gameId=" + UnityAdsProperties.UNITY_ADS_APP_ID + "&openUdid=someudid&device=iphone&iosVersion=6.0";
		
		
		if (data != null)
			dataString = json.toString();
			
		UnityAdsUrlLoader loader = new UnityAdsUrlLoader(UnityAdsProperties.WEBDATA_URL + "?" + dataString, UnityAdsRequestType.VideoPlan);
		addLoader(loader);
		startNextLoader();
		checkFailedUrls();
		
		return true;
	}
	
	public boolean sendCampaignViewed (UnityAdsCampaign campaign, UnityAdsVideoPosition position) {
		if (campaign == null) return false;
		
		JSONObject json = new JSONObject();
		
		try {
			json.put("did", UnityAdsUtils.getDeviceId(UnityAdsProperties.CURRENT_ACTIVITY));
			json.put("c", campaign.getCampaignId());
			json.put("pos", position.toString());
		}
		catch (Exception e) {			
		}
		
		String dataString = "";
		dataString = json.toString();
		
		UnityAdsUrlLoader loader = new UnityAdsUrlLoader(UnityAdsProperties.WEBDATA_URL + "?v=" + dataString, UnityAdsRequestType.VideoViewed);
		addLoader(loader);
		startNextLoader();		
		
		return false;
	}
	
	public void stopAllRequests () {
		_urlLoaders.clear();
		
		if (_currentLoader != null)
			_currentLoader.cancel(true);
	}
	
	public String getVideoPlan () {
		return _videoPlan.toString();
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
	
	private JSONArray getCachedCampaignIdsArray (ArrayList<String> cachedCampaignIds) {
		JSONArray campaignIds = null;
		
		if (cachedCampaignIds != null && cachedCampaignIds.size() > 0) {
			campaignIds = new JSONArray();
			
			for (String id : cachedCampaignIds) {
				campaignIds.put(id);
			}
		}
		
		return campaignIds;
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
		File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.PENDING_REQUESTS_FILENAME);
		
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
		
		File pendingRequestFile = new File(UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.PENDING_REQUESTS_FILENAME);
		UnityAdsUtils.writeFile(pendingRequestFile, fileContent);
	}
	
	private void videoPlanReceived (String json) {
		try {
			_videoPlan = new JSONObject(json);
			JSONObject data = null;
			
			if (_videoPlan.has("data")) {
				try {
					data = _videoPlan.getJSONObject("data");
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Malformed data JSON");
				}
				
				_videoPlanCampaigns = UnityAdsUtils.createCampaignsFromJson(data);
			}	
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON!");
		}
		
		if (_listener != null)
			_listener.onWebDataCompleted();
		
		Log.d(UnityAdsProperties.LOG_NAME, _videoPlanCampaigns.toString());
	}
	
	private void videoPlanFailed () {
		if (_listener != null)
			_listener.onWebDataFailed();		
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
				Log.d(UnityAdsProperties.LOG_NAME, "Problems with url: " + e.getMessage());
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
				Log.d(UnityAdsProperties.LOG_NAME, "Problems opening connection: " + e.getMessage());
			}
			
			if (_urlConnection != null) {
				_downloadLength = _urlConnection.getContentLength();
				
				try {
					_input = new BufferedInputStream(_url.openStream());
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems opening stream: " + e.getMessage());
				}
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					Log.d(UnityAdsProperties.LOG_NAME, "Reading data from: " + _url.toString());
					while ((count = _input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / _downloadLength));
						_urlData = _urlData.concat(new String(data));
						
						if (isCancelled())
							return null;
					}
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems loading url: " + e.getMessage());
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
				Log.d(UnityAdsProperties.LOG_NAME, "Problems closing connection: " + e.getMessage());
			}	
		}
	}
}
