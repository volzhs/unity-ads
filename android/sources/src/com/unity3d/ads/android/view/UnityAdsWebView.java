package com.unity3d.ads.android.view;

import java.lang.reflect.Method;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAdsProperties;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class UnityAdsWebView extends WebView {

	private String _url = "http://ads-dev.local/webapp.html";	
	private JSONObject _videoPlan = null;
	private Activity _currentActivity = null;
	private IUnityAdsWebViewListener _listener = null;
	
	private static enum UnityAdsUrl { UnityAds;
		@Override		
		public String toString () {
			String retVal = null;
			
			switch (this) {
				case UnityAds:
					retVal = "applifierimpact://";
			}
			
			return retVal;
		}
	}; 
	
	
	public UnityAdsWebView(Activity activity, JSONObject videoPlan, IUnityAdsWebViewListener listener) {
		super(activity);
		init(activity, _url, videoPlan, listener);
	}

	public UnityAdsWebView(Activity activity, String url, JSONObject videoPlan, IUnityAdsWebViewListener listener) {
		super(activity);
		init(activity, url, videoPlan, listener);
	}
	
	
	/* INTENRAL METHODS */
	
	private void init (Activity activity, String url, JSONObject videoPlan, IUnityAdsWebViewListener listener) {
		_listener = listener;
		_url = url;
		_videoPlan = videoPlan;
		_currentActivity = activity;
		setupUnityAdsView();
		loadUrl(_url);
	}
	
	private void setupUnityAdsView ()  {
		getSettings().setJavaScriptEnabled(true);
		
		if (_url != null && _url.indexOf("_raw.html") != -1) {
			getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			Log.d(UnityAdsProperties.LOG_NAME, "startup() -> LOAD_NO_CACHE");
		}
		else {
			getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
		}
		
		String appCachePath = getContext().getCacheDir().toString();
		
		getSettings().setSupportZoom(false);
		getSettings().setBuiltInZoomControls(false);
		getSettings().setLightTouchEnabled(false);
		getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		getSettings().setSupportMultipleWindows(false);
		
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);		

		setClickable(true);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setInitialScale(0);
		
		setBackgroundColor(Color.TRANSPARENT);
		setBackgroundDrawable(null);
		setBackgroundResource(0);
		
		setWebViewClient(new UnityAdsViewClient());
		setWebChromeClient(new UnityAdsViewChromeClient());
			
		if (appCachePath != null) {
			boolean appCache = true;
  
			if (Integer.parseInt(android.os.Build.VERSION.SDK) <= 7) {
				appCache = false;
				//webapp.addLogMessage("AppCache is set to: FALSE", -1, "internal");
			}
  
			getSettings().setAppCacheEnabled(appCache);
			getSettings().setDomStorageEnabled(true);
			getSettings().setAppCacheMaxSize(1024*1024*10);
			getSettings().setAppCachePath(appCachePath);
			getSettings().setAllowFileAccess(true);
		}
		
		// WebView background will go white in SDK >= 11 if you don't set webview's
		// layer-type to software.
		try
		{
			Method layertype = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
			layertype.invoke(this, 1, null);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Could not invoke setLayerType");
		}
	}
	
	
	/* OVERRIDE METHODS */
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
		    	if (_listener != null)
		    		_listener.onBackButtonClicked();
		    	return true;
		}
    	
    	return false;
    } 
	
	/* SUBCLASSES */
	
	private class UnityAdsViewChromeClient extends WebChromeClient {
		public void onConsoleMessage(String message, int lineNumber, String sourceID) {
			//webapp.addLogMessage(message, lineNumber, sourceID);
			// TODO: Log console messages
		}
		
		public void onReachedMaxAppCacheSize(long spaceNeeded, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
			quotaUpdater.updateQuota(spaceNeeded * 2);
		}
	}
	
	private class UnityAdsViewClient extends WebViewClient {
		@Override
		public void onPageFinished (WebView webview, String url) {
			super.onPageFinished(webview, url);
			Log.d(UnityAdsProperties.LOG_NAME, "Finished url: "  + url);
			
			// TODO: Webview ready, init
			
			/*
			if (webapp.getWebState() != ApplifierWebState.Ready && webview != null) {
				webapp.initWebApp((UnityAdsView)webview);				
				webapp.processJavascriptCommandLog();
			}*/
		}
		
		@Override
		public boolean shouldOverrideUrlLoading (WebView view, String url) {
			boolean shouldOverride = false;
			
			if (view == null || url == null) return true;
			
			if (url.startsWith(UnityAdsUrl.UnityAds.toString())) {
				Log.d(UnityAdsProperties.LOG_NAME, "Applifier URL!!");
				
				if (url.endsWith("playVideo")) {
					if (_listener != null)
						_listener.onPlayVideoClicked();
				}
				else if (url.endsWith("videoCompleted")) {
					if (_listener != null)
						_listener.onVideoCompletedClicked();
				}
				else if (url.endsWith("close")) {
					if (_listener != null)
						_listener.onCloseButtonClicked();
				}
				
				shouldOverride = true;
			}

			return shouldOverride;
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {		
			Log.e(UnityAdsProperties.LOG_NAME, "UnityAdsViewClient.onReceivedError() -> " + errorCode + " (" + failingUrl + ") " + description);
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		@Override
		public void onLoadResource(WebView view, String url) {
			super.onLoadResource(view, url);
		}	
	}
}
