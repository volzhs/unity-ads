package com.unity3d.ads.android.cache;

import java.io.BufferedInputStream;
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
	private VideoPlanLoader _videoPlanLoader = null;
	private ArrayList<UnityAdsCampaign> _videoPlanCampaigns = null;
	private IUnityAdsWebDataListener _listener = null;
	
	public UnityAdsWebData () {	
	}
	
	public void setWebDataListener (IUnityAdsWebDataListener listener) {
		_listener = listener;
	}
	
	public ArrayList<UnityAdsCampaign> getVideoPlanCampaigns () {
		return _videoPlanCampaigns;
	}
	
	public int getCampaignAmount () {
		if (_videoPlanCampaigns == null) return 0;
		return _videoPlanCampaigns.size();
	}
	
	public UnityAdsCampaign getCampaignById (String campaignId) {
		for (UnityAdsCampaign currentCampaign : _videoPlanCampaigns) {
			if (currentCampaign.getCampaignId().equals(campaignId))
				return currentCampaign;
		}
		
		return null;
	}
	
	public boolean initVideoPlan (ArrayList<String> cachedCampaignIds) {		
		JSONObject data = new JSONObject();
		JSONArray campaignIds = null;
		
		if (cachedCampaignIds != null && cachedCampaignIds.size() > 0) {
			campaignIds = new JSONArray();
			
			for (String id : cachedCampaignIds) {
				campaignIds.put(id);
			}
		}
		
		try {
			data.put("c", campaignIds);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON");
			return false;
		}
		
		String cachedCampaignData = null;
		
		if (data != null && campaignIds != null && campaignIds.length() > 0) {
			cachedCampaignData = data.toString();
			Log.d(UnityAdsProperties.LOG_NAME, cachedCampaignData);
		}
					
		// TODO: Send campaign ID's with the request
		
		_videoPlanLoader = new VideoPlanLoader();
		_videoPlanLoader.execute(UnityAdsProperties.WEBDATA_URL);
		
		/*
		JSONArray videos = new JSONArray();
		JSONObject tmpvideo = null;
		
		try {
			tmpvideo = new JSONObject();
			tmpvideo.put("v", "http://quake.everyplay.fi/~bluesun/testvideos/video5.mp4");
			tmpvideo.put("s", "Ready");
			tmpvideo.put("id", "a5");
			videos.put(tmpvideo);
			
			tmpvideo = new JSONObject();
			tmpvideo.put("v", "http://quake.everyplay.fi/~bluesun/testvideos/video2.mp4");
			tmpvideo.put("s", "blaa2");
			tmpvideo.put("id", "a2");
			videos.put(tmpvideo);
	
			
			tmpvideo = new JSONObject();
			tmpvideo.put("v", "http://quake.everyplay.fi/~bluesun/testvideos/video3.mp4");
			tmpvideo.put("s", "blaa3");
			tmpvideo.put("id", "a3");
			videos.put(tmpvideo);			
		
			_videoPlan = new JSONObject();
			_videoPlan.put("va", videos);
			
			Log.d(UnityAdsProperties.LOG_NAME, _videoPlan.toString(4));
			
			_videoPlanCampaigns = UnityAdsUtils.createCampaignsFromJson(_videoPlan);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Great error!");
			return false;
		}*/
		
		return true;
	}
	
	private void videoPlanReceived (String json) {
		try {
			_videoPlan = new JSONObject(json);
			_videoPlanCampaigns = UnityAdsUtils.createCampaignsFromJson(_videoPlan);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON!");
		}
		
		if (_listener != null)
			_listener.onWebDataCompleted();
	}
	
	
	/* INTERNAL CLASSES */
	
	private class VideoPlanLoader extends AsyncTask<String, Integer, String> {
		private URL _videoPlanUrl = null;
		private URLConnection _urlConnection = null;
		private int _downloadLength = 0;
		private InputStream _input = null;
		private String _urlData = "";
		
		@Override
		protected String doInBackground(String... params) {
			try {
				_videoPlanUrl = new URL(params[0]);				
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problems with url: " + e.getMessage());
			}
			
			try {
				_urlConnection = _videoPlanUrl.openConnection();
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
					_input = new BufferedInputStream(_videoPlanUrl.openStream());
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems opening stream: " + e.getMessage());
				}
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					while ((count = _input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / _downloadLength));
						_urlData = _urlData.concat(new String(data));
						
						if (isCancelled())
							return null;
					}
				}
				catch (Exception e) {
					closeAndFlushConnection();
					Log.d(UnityAdsProperties.LOG_NAME, "Problems downloading file: " + e.getMessage());
					return null;
				}
			}
			
			return null;
		}

		protected void onCancelled(Object result) {
			closeAndFlushConnection();
		}

		@Override
		protected void onPostExecute(String result) {
			if (!isCancelled()) {
				closeAndFlushConnection();
				videoPlanReceived(_urlData);
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
