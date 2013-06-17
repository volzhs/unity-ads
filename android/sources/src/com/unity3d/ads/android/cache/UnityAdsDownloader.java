package com.unity3d.ads.android.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Vector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.properties.UnityAdsProperties;

public class UnityAdsDownloader {
	
	private static ArrayList<UnityAdsCampaign> _downloadList = null;
	private static ArrayList<IUnityAdsDownloadListener> _downloadListeners = null;
	private static boolean _isDownloading = false;
	private static enum UnityAdsDownloadEventType { DownloadCompleted, DownloadCancelled };
	private static Vector<CacheDownload> _cacheDownloads = null;
	
	public static void addDownload (UnityAdsCampaign downloadCampaign) {
		if (_downloadList == null) _downloadList = new ArrayList<UnityAdsCampaign>();
		
		if (!isInDownloads(downloadCampaign.getVideoUrl())) {
			_downloadList.add(downloadCampaign);
		}
		
		if (!_isDownloading) {
			_isDownloading = true;
			cacheNextFile();
		}
	}
	
	public static void addListener (IUnityAdsDownloadListener listener) {
		if (_downloadListeners == null) _downloadListeners = new ArrayList<IUnityAdsDownloadListener>();		
		if (!_downloadListeners.contains(listener))
			_downloadListeners.add(listener);
	}
	
	public static void removeListener (IUnityAdsDownloadListener listener) {
		if (_downloadListeners == null) return;		
		if (_downloadListeners.contains(listener)) {
			_downloadListeners.remove(listener);
		}
	}
	
	public static void stopAllDownloads () {
		if (_cacheDownloads != null) {
			UnityAdsUtils.Log("UnityAdsDownloader->stopAllDownloads()", UnityAdsDownloader.class);
			for (CacheDownload cd : _cacheDownloads) {
				cd.cancel(true);
			}
		}
	}
	
	public static void clearData () {
		if (_cacheDownloads != null) {
			_cacheDownloads.clear();
			_cacheDownloads = null;
		}
		
		_isDownloading = false;
		
		if (_downloadListeners != null) {
			_downloadListeners.clear();
			_downloadListeners = null;
		}
	}
	
	
	/* INTERNAL METHODS */
	
	private static void removeDownload (UnityAdsCampaign campaign) {
		if (_downloadList == null) return;
		
		int removeIndex = -1;
		
		for (int i = 0; i < _downloadList.size(); i++) {
			if (_downloadList.get(i).equals(campaign)) {
				removeIndex = i;
				break;
			}
		}
		
		if (removeIndex > -1)
			_downloadList.remove(removeIndex);
	}
	
	private static boolean isInDownloads (String downloadUrl) {
		if (_downloadList != null) {
			for (UnityAdsCampaign download : _downloadList) {
				if (download != null && download.getVideoUrl() != null && download.getVideoUrl().equals(downloadUrl))
					return true;
				else if (download == null || download.getVideoUrl() == null)
					_downloadList.remove(download);
			}
		}
		
		return false;
	}
	
	private static void sendToListeners (UnityAdsDownloadEventType type, String downloadUrl) {
		if (_downloadListeners == null) return;

		@SuppressWarnings("unchecked")
		ArrayList<IUnityAdsDownloadListener> tmpListeners = (ArrayList<IUnityAdsDownloadListener>)_downloadListeners.clone();
		
		for (IUnityAdsDownloadListener listener : tmpListeners) {
			switch (type) {
				case DownloadCompleted:
					listener.onFileDownloadCompleted(downloadUrl);
					break;
				case DownloadCancelled:
					listener.onFileDownloadCancelled(downloadUrl);
					break;
			}
		}
	}
	
	private static void cacheCampaign (UnityAdsCampaign campaign) {
		if (UnityAdsProperties.CURRENT_ACTIVITY == null || UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext() == null) return;
		
		ConnectivityManager cm = (ConnectivityManager)UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	    
		if (cm != null && cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
			UnityAdsUtils.Log("Starting download for: " + campaign.getVideoFilename(), UnityAdsDownloader.class);
			
			if (campaign != null && campaign.getVideoUrl() != null && campaign.getVideoUrl().length() > 0) {
				CacheDownload cd = new CacheDownload(campaign);
				addToCacheDownloads(cd);
				cd.execute(campaign.getVideoUrl());
			}
			else {
				removeDownload(campaign);
			}
	    }
		else {
			UnityAdsUtils.Log("No WIFI detected, not downloading: " + campaign.getVideoUrl(), UnityAdsDownloader.class);
			removeDownload(campaign);
			sendToListeners(UnityAdsDownloadEventType.DownloadCancelled, campaign.getVideoUrl());
			cacheNextFile(); 
		}
	}
	
	private static void cacheNextFile () {
		if (_downloadList != null && _downloadList.size() > 0) {
			cacheCampaign(_downloadList.get(0));
		}
		else if (_downloadList != null) {
			_isDownloading = false;
			UnityAdsUtils.Log("All downloads completed.", UnityAdsDownloader.class);
		}
	}
	
	private static FileOutputStream getOutputStreamFor (String fileName) {
		File tdir = UnityAdsUtils.createCacheDir();
		File outf = new File (tdir, fileName);
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(outf);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Problems creating FOS: " + fileName, UnityAdsDownloader.class);
			return null;
		}
		
		return fos;
	}
	
	private static void addToCacheDownloads (CacheDownload cd) {
		if (_cacheDownloads == null) 
			_cacheDownloads = new Vector<UnityAdsDownloader.CacheDownload>();
		
		_cacheDownloads.add(cd);
	}
	
	private static void removeFromCacheDownloads (CacheDownload cd) {
		if (_cacheDownloads != null)
			_cacheDownloads.remove(cd);
	}
	
	
	/* INTERNAL CLASSES */
	
	private static class CacheDownload extends AsyncTask<String, Integer, String> {
		private URL _downloadUrl = null;
		private InputStream _input = null;
		private OutputStream _output = null;
		private int _downloadLength = 0;
		private URLConnection _urlConnection = null;
		private boolean _cancelled = false;
		private UnityAdsCampaign _campaign = null;
		
		public CacheDownload (UnityAdsCampaign campaign) {
			_campaign = campaign;
		}
		
		@Override
	    protected String doInBackground(String... sUrl) {
			long startTime = System.currentTimeMillis();
			long duration = 0;
			
			try {
				_downloadUrl = new URL(sUrl[0]);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems with url: " + e.getMessage(), this);
				onCancelled();
				return null;
			}
			
			try {
				_urlConnection = _downloadUrl.openConnection();
				_urlConnection.setConnectTimeout(10000);
				_urlConnection.setReadTimeout(10000);
				_urlConnection.connect();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems opening connection: " + e.getMessage(), this);
			}
			
			if (_urlConnection != null) {
				_downloadLength = _urlConnection.getContentLength();
				
				try {
					_input = new BufferedInputStream(_downloadUrl.openStream());
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems opening stream: " + e.getMessage(), this);
				}
				
				_output = getOutputStreamFor(_campaign.getVideoFilename());
				if (_output == null)
					onCancelled();
				
				byte data[] = new byte[1024];
				long total = 0;
				int count = 0;
				
				try {
					while ((count = _input.read(data)) != -1) {
						total += count;
						publishProgress((int)(total * 100 / _downloadLength));
						_output.write(data, 0, count);
						
						if (_cancelled) {
							return null;
						}
					}
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Problems downloading file: " + e.getMessage(), this);
					cancelDownload();
					cacheNextFile();
					return null;
				}
				
				closeAndFlushConnection();
				duration = System.currentTimeMillis() - startTime;
				UnityAdsUtils.Log("File: " + _campaign.getVideoFilename() + " of size: " + total + " downloaded in: " + duration + "ms", this);
			}
						
			return null;
		}
		
		@Override
		protected void onCancelled () {
			UnityAdsUtils.Log("Force stopping download!", this);
			_cancelled = true;
			cancelDownload();
		}

		@Override
		protected void onPostExecute(String result) {
        	if (!_cancelled) {
    			removeDownload(_campaign);
            	removeFromCacheDownloads(this);
            	cacheNextFile();
            	sendToListeners(UnityAdsDownloadEventType.DownloadCompleted, _downloadUrl.toString());
    			super.onPostExecute(result);
        	}
		}

		@Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        
	        if (progress[0] == 100) {
	        }
	    }
	    
	    private void closeAndFlushConnection () {			
			try {
				_output.flush();
				_output.close();
				_input.close();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Problems closing connection: " + e.getMessage(), this);
			}	    	
	    }
	    
	    private void cancelDownload () {
	    	UnityAdsUtils.Log("Download cancelled for: " + _downloadUrl.toString(), this);
			closeAndFlushConnection();
			UnityAdsUtils.removeFile(_campaign.getVideoFilename());
        	removeDownload(_campaign);
        	removeFromCacheDownloads(this);
        	sendToListeners(UnityAdsDownloadEventType.DownloadCancelled, _downloadUrl.toString());
	    }
	}
}
