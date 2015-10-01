package com.unity3d.ads.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.video.UnityAdsVideoPlayView;
import com.unity3d.ads.android.webapp.IUnityAdsWebBridgeListener;
import com.unity3d.ads.android.webapp.IUnityAdsWebViewListener;
import com.unity3d.ads.android.webapp.UnityAdsWebBridge;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.webapp.UnityAdsWebView;

import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsMainView extends RelativeLayout {

	public enum UnityAdsMainViewState { WebView, VideoPlayer }
	private static final int FILL_PARENT = -1;
	
	// Views
	public UnityAdsVideoPlayView videoplayerview = null;
	public static UnityAdsWebView webview = null;

	// Listener
	private IUnityAdsWebBridgeListener _webBridgeListener = null;
	private UnityAdsMainViewState _currentState = UnityAdsMainViewState.WebView;

	
	public UnityAdsMainView(Context context, IUnityAdsWebBridgeListener webBridgeListener) {
		super(context);
		_webBridgeListener = webBridgeListener;
		init();
	}
	
	
	public UnityAdsMainView(Context context) {
		super(context);
		init();
	}

	public UnityAdsMainView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public UnityAdsMainView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);		
		init();
	}
	
	
	/* PUBLIC METHODS */

	public void setViewState (UnityAdsMainViewState state) {
		if (!_currentState.equals(state)) {
			_currentState = state;
			
			switch (state) {
				case WebView:
					UnityAdsViewUtils.removeViewFromParent(webview);
					addView(webview, new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
					break;
				case VideoPlayer:
					if (videoplayerview == null) {
						createVideoPlayerView();
						bringChildToFront(webview);
					}
					break;
			}
		}
	}
	
	public UnityAdsMainViewState getViewState () {
		return _currentState;
	}

	public static void initWebView () {
		if (webview != null) {
			UnityAdsViewUtils.removeViewFromParent(webview);
			webview.destroy();
			webview = null;
		}

		UnityAdsDeviceLog.debug("Initing WebView");

		webview = new UnityAdsWebView(UnityAdsProperties.APPLICATION_CONTEXT, new IUnityAdsWebViewListener() {
			@Override
			public void onWebAppLoaded() {
				webview.initWebApp(UnityAdsWebData.getData());
			}
		}, new UnityAdsWebBridge(new IUnityAdsWebBridgeListener() {
			@Override
			public void onPlayVideo(JSONObject data) { }

			@Override
			public void onPauseVideo(JSONObject data) {	}

			@Override
			public void onCloseAdsView(JSONObject data) { }

			@Override
			public void onWebAppLoadComplete(JSONObject data) {	}

			@Override
			public void onWebAppInitComplete(JSONObject data) {
				String msg = "WebView reported WebAppInitComplete";
				if (data != null) msg += " " + data.toString();
				UnityAdsDeviceLog.debug(msg);
				Boolean dataOk = true;

				if(UnityAdsWebData.hasViewableAds()) {
					JSONObject setViewData = new JSONObject();

					try {
						setViewData.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_INITCOMPLETE);
					}
					catch (Exception e) {
						dataOk = false;
					}

					if (dataOk) {
						UnityAdsMainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE, setViewData);

						UnityAdsUtils.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (!UnityAdsProperties.isAdsReadySent() && UnityAds.getListener() != null) {
									UnityAdsDeviceLog.debug("Unity Ads ready.");
									UnityAdsProperties.setAdsReadySent(true);
									UnityAds.getListener().onFetchCompleted();
								}
							}
						});
					}
				}
			}

			@Override
			public void onOrientationRequest(JSONObject data) {	}

			@Override
			public void onOpenPlayStore(JSONObject data) { }

			@Override
			public void onLaunchIntent(JSONObject data) { }
		}));
	}
	
	/* PRIVATE METHODS */

	private void init () {
		UnityAdsDeviceLog.entered();
		webview.setWebBridgeListener(_webBridgeListener);
		post(new Runnable() {
			@Override
			public void run() {
				placeWebView();
			}
		});
	}
	
	public void destroyVideoPlayerView () {
		UnityAdsDeviceLog.entered();   	
		
		if (videoplayerview != null)
			videoplayerview.clearVideoPlayer();
		
		UnityAdsViewUtils.removeViewFromParent(videoplayerview);
		videoplayerview = null;
	}

	private void createVideoPlayerView () {
		videoplayerview = new UnityAdsVideoPlayView(getContext());
		videoplayerview.setLayoutParams(new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
		addView(videoplayerview);
	}
	
	private void placeWebView() {
		if (webview != null) {
			if (webview.getParent() != null) UnityAdsViewUtils.removeViewFromParent(webview);
			addView(webview, new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
		}
	}
}