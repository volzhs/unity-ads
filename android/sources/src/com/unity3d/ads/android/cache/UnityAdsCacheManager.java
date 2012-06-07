package com.unity3d.ads.android.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsCampaign;
import com.unity3d.ads.android.UnityAdsProperties;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class UnityAdsCacheManager {
	private String _externalStorageDir = null;
	private ArrayList<JSONObject> _videoFileDownloads = null;
	private JSONObject _videoPlan = null;
	private IUnityAdsCacheListener _listener = null;
	
	private int _videosToDownload = 0;
	
	public UnityAdsCacheManager () {
		_externalStorageDir = Environment.getExternalStorageDirectory().toString();
		createCacheDir();
		Log.d(UnityAdsProperties.LOG_NAME, "External storagedir: " + _externalStorageDir);
	}
	
	public void setCacheListener (IUnityAdsCacheListener listener) {
		_listener = listener;
	}
	
	public String getCacheDir () {
		return _externalStorageDir + "/" + UnityAdsProperties.CACHE_DIR_NAME;
	}
	
	public void cacheFile (String url, String id) {
		CacheDownload cd = new CacheDownload();
		cd.execute(url, id);
	}
	
	public boolean isFileCached (String fileName) {
		File videoFile = new File (fileName);
		File cachedVideoFile = new File (getCacheDir() + "/" + videoFile.getName());
		
		return cachedVideoFile.exists();
	}
		
	public void updateCache (ArrayList<UnityAdsCampaign> fromList) {
		if (fromList != null) {
			for (UnityAdsCampaign campaign : fromList) {
				if (!isFileCached(campaign.getVideoFilename())) {
					_videosToDownload++;
					cacheFile(campaign.getVideoUrl(), campaign.getCampaignId());
				}					
			}
		}
		
		// TODO: Delete files
	}
	
	
	// INTERNAL METHODS
	
	private File createCacheDir () {
		File tdir = new File (_externalStorageDir + "/" + UnityAdsProperties.CACHE_DIR_NAME);
		tdir.mkdirs();
		return tdir;
	}
	
	private FileOutputStream getOutputStreamFor (String fileName) {
		File tdir = createCacheDir();
		File outf = new File (tdir, fileName);
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(outf);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Problems creating FOS: " + fileName);
		}
		
		return fos;
	}
	
	
	
	
	
	/* ALL BELOW HERE SUSPECTED TO BE REMOVED OR REWRITTEN */
	
	public void initCache (JSONObject cacheManifest, JSONObject videoPlan) {
		_videoPlan = videoPlan;
		updateCampaigns(videoPlan);
		createCampaignDownloadList(videoPlan);
		
		if (_listener != null && (_videoFileDownloads == null || _videoFileDownloads.size() == 0)) {
        	Log.d(UnityAdsProperties.LOG_NAME, "Reporting caching done!");
        	_listener.onCachedCampaignsAvailable();
		}
		
		downloadVideoFiles();
	}
	

	public void removeCachedFile (String fileName) {
		// TODO: Check that the file being removed is not needed anymore by other campaigns
		File videoFile = new File (fileName);
		File cachedVideoFile = new File (getCacheDir() + "/" + videoFile.getName());
		
		if (cachedVideoFile.exists()) {
			if (!cachedVideoFile.delete()) {
				Log.d(UnityAdsProperties.LOG_NAME, "Could not delete: " + cachedVideoFile.getAbsolutePath());
			}
		}
	}
	
	/* INTERNAL METHODS */
	
	private JSONObject getCampaign (String id) {
		if (_videoPlan != null && _videoPlan.has("va")) {
			JSONArray va = null;
			JSONObject campaign = null;
			String campaignId = null;
			
			try {
				va = _videoPlan.getJSONArray("va");
			}
			catch (Exception e) {
				return null;
			}
			
			for (int i = 0; i < va.length(); i++) {
				try {
					campaign = va.getJSONObject(i);
				}
				catch (Exception e) {
					continue;
				}
				
				if (campaign != null && campaign.has("id")) {
					try {
						campaignId = campaign.getString("id");
					}
					catch (Exception e) {
						continue;
					}
				}
				
				if (id.equals(campaignId))
					return campaign;
			}
		}
		
		return null;
	}
	
	private void updateCampaigns (JSONObject videoPlan) {
		if (videoPlan.has("cs")) {
			JSONArray cs = null;
			JSONObject campaignResponse = null;
			JSONObject campaign = null;
			String campaignStatus = null;
			String campaignId = null;			
			
			try {
				cs = videoPlan.getJSONArray("cs");
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
				return;
			}
			
			for (int i = 0; i < cs.length(); i++) {
				try {
					campaignResponse = cs.getJSONObject(i);
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
					return;
				}
				
				if (campaignResponse.has("s")) {
					try {
						campaignStatus = campaignResponse.getString("s");
					}
					catch (Exception e) {
						Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
						continue;
					}
				}
				
				if (campaignResponse.has("id")) {
					try {
						campaignId = campaignResponse.getString("id");
					}
					catch (Exception e) {
						Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
						continue;
					}
					
					campaign = UnityAds.cachemanifest.getCampaign(campaignId);
					
					if ("old".equals(campaignStatus)) {
						if (campaign != null && campaign.has("v")) {
							String fileName = "";
							
							try {
								fileName = campaign.getString("v");
							}
							catch (Exception e) {
								Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
								continue;
							}
							
							removeCachedFile(fileName);
						}
						
						UnityAds.cachemanifest.removeCampaignFromManifest(campaign);
					}
					else if ("update".equals(campaignStatus)){
						UnityAds.cachemanifest.updateCampaignInManifest(campaignResponse);
					}
				}
			}
		}
	}
	
	private void createCampaignDownloadList (JSONObject videoPlan) {		
		if (videoPlan.has("va")) {
			JSONArray va = null;
			JSONObject campaignData = null;
			String videoFileName = null;
			
			try {
				va = videoPlan.getJSONArray("va");
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
				return;
			}
			
			for (int i = 0; i < va.length(); i++) {
				try {
					campaignData = va.getJSONObject(i); 
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
					return;
				}
				
				try {
					videoFileName = campaignData.getString("v");
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Invalid JSON: " + e.getMessage());
					continue;
				}
				
				String campaignId = null;
				
				try {
					campaignId = campaignData.getString("id");
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "No ID for campaign: " + e.getMessage());
					continue;
				}
				
				File videoFile = new File (videoFileName);
				File possiblyCachedVideoFile = new File (getCacheDir() + "/" + videoFile.getName());
				
				Log.d(UnityAdsProperties.LOG_NAME, campaignId + ", " + getCampaign(campaignId).toString() + ", " + UnityAds.cachemanifest.getCampaign(campaignId));
				
				// Skip download if file exists
				if (!possiblyCachedVideoFile.exists()) {
					if (_videoFileDownloads == null)
						_videoFileDownloads = new ArrayList<JSONObject>();
					
					_videoFileDownloads.add(campaignData);
					Log.d(UnityAdsProperties.LOG_NAME, "Adding to downloadlist: " + videoFileName);
				}
				// If file exists but campaign not in manifest, add it.				
				else if (campaignId != null && getCampaign(campaignId) != null && UnityAds.cachemanifest.getCampaign(campaignId) == null) {
					Log.d(UnityAdsProperties.LOG_NAME, "File already exists, but data not in manifest: " + campaignId);
					UnityAds.cachemanifest.addCampaignToManifest(getCampaign(campaignId));
				}
				else {
					Log.d(UnityAdsProperties.LOG_NAME, "File already exists: " + videoFileName);
				}
			}
		}
	}
	
	private void downloadVideoFiles () {
		if (_videoFileDownloads != null) {
			for (JSONObject campaign : _videoFileDownloads) {
				if (campaign.has("v") && campaign.has("id")) {
					try {
						cacheFile(campaign.getString("v"), campaign.getString("id"));
					}
					catch (Exception e) {						
					}
				}				
			}
		}
	}
		

	
	
	/* INTERNAL CLASSES */
	
	private class CacheDownload extends AsyncTask<String, Integer, String> {
		private URL _downloadUrl = null;
		private String _campaignId = null;
		
		@Override
	    protected String doInBackground(String... sUrl) {
			//URL url = null;
			URLConnection connection = null;
			int downloadLength = 0;
			_campaignId = sUrl[1];
			
			try {
				_downloadUrl = new URL(sUrl[0]);
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problems with url: " + e.getMessage());
			}
			
			try {
				connection = _downloadUrl.openConnection();
				connection.connect();
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problems opening connection: " + e.getMessage());
			}
			
			if (connection != null) {
				downloadLength = connection.getContentLength();
				InputStream input = null;
				OutputStream output = null;
				
				try {
					input = new BufferedInputStream(_downloadUrl.openStream());
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems opening stream: " + e.getMessage());
				}
				
				File target = new File(sUrl[0]);
				output = getOutputStreamFor(target.getName());
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					while ((count = input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / downloadLength));
						output.write(data, 0, count);
					}
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems downloading file: " + e.getMessage());
				}
				
				try {
					output.flush();
					output.close();
					input.close();
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Problems closing connection: " + e.getMessage());
				}
			}
						
			return null;
		}
		
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        
	        if (progress[0] == 100) {
		        Log.d(UnityAdsProperties.LOG_NAME, "DOWNLOADED: " + _downloadUrl.toString() + ", CID: " + _campaignId + ", CAMPAIGN: " + getCampaign(_campaignId).toString());
		       
		        // TODO: Make checks to update campaign if it exists already
		        UnityAds.cachemanifest.addCampaignToManifest(getCampaign(_campaignId));
		        
		        Log.d(UnityAdsProperties.LOG_NAME, "" + _videoFileDownloads);
		        
		        if (_videoFileDownloads != null) {
			        _videoFileDownloads.remove(0);
			        
			        if (_videoFileDownloads.size() == 0 && _listener != null) {
			        	Log.d(UnityAdsProperties.LOG_NAME, "Reporting caching done!");
			        	_listener.onCachedCampaignsAvailable();
			        }
		        }
	        }
	    }
	}
}
