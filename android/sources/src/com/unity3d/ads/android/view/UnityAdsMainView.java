package com.unity3d.ads.android.view;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.video.UnityAdsVideoPlayView;
import com.unity3d.ads.android.video.IUnityAdsVideoPlayerListener;
import com.unity3d.ads.android.webapp.UnityAdsInstrumentation;
import com.unity3d.ads.android.webapp.UnityAdsWebBridge;
import com.unity3d.ads.android.webapp.UnityAdsWebView;
import com.unity3d.ads.android.webapp.IUnityAdsWebViewListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class UnityAdsMainView extends RelativeLayout implements 	IUnityAdsWebViewListener, 
																		IUnityAdsVideoPlayerListener {

	public static enum UnityAdsMainViewState { WebView, VideoPlayer };
	public static enum UnityAdsMainViewAction { VideoStart, VideoEnd, VideoSkipped, BackButtonPressed, RequestRetryVideoPlay };
	private static final int FILL_PARENT = -1;
	
	// Views
	public UnityAdsVideoPlayView videoplayerview = null;
	public UnityAdsWebView webview = null;

	// Listener
	private IUnityAdsMainViewListener _listener = null;
	private UnityAdsMainViewState _currentState = UnityAdsMainViewState.WebView;

	
	public UnityAdsMainView(Context context, IUnityAdsMainViewListener listener) {
		super(context);
		_listener = listener;
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
	
	public void openAds (String view, JSONObject data) {
		if (UnityAdsProperties.CURRENT_ACTIVITY != null && UnityAdsProperties.CURRENT_ACTIVITY.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
			webview.setWebViewCurrentView(view, data);
			
			if (this.getParent() != null && (ViewGroup)this.getParent() != null)
				((ViewGroup)this.getParent()).removeView(this);
			
			if (this.getParent() == null)
				UnityAdsProperties.CURRENT_ACTIVITY.addContentView(this, new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
			
			setViewState(UnityAdsMainViewState.WebView);
		}
		else {
			UnityAdsUtils.Log("Cannot open, wrong activity", this);
		}
	}
	
	public void closeAds (JSONObject data) {		
		if (this.getParent() != null) {
			ViewGroup vg = (ViewGroup)this.getParent();
			if (vg != null)
				vg.removeView(this);
		}
		
		destroyVideoPlayerView();
		UnityAdsProperties.SELECTED_CAMPAIGN = null;
	}
	
	public void setViewState (UnityAdsMainViewState state) {
		if (!_currentState.equals(state)) {
			_currentState = state;
			
			switch (state) {
				case WebView:
					removeFromMainView(webview);
					addView(webview, new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
					focusToView(webview);
					break;
				case VideoPlayer:
					if (videoplayerview == null) {
						createVideoPlayerView();
						bringChildToFront(webview);
						focusToView(webview);
					}
					break;
			}
		}
	}
	
	public UnityAdsMainViewState getViewState () {
		return _currentState;
	}
	
	public void afterVideoPlaybackOperations () {
		if (videoplayerview != null) {
			videoplayerview.setKeepScreenOn(false);
		}
		
		destroyVideoPlayerView();
		setViewState(UnityAdsMainViewState.WebView);		
		UnityAdsProperties.CURRENT_ACTIVITY.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				onBackButtonClicked(this);
		    	return true;
		}
    	
    	return false;
    }
	
    protected void onAttachedToWindow() {
    	super.onAttachedToWindow();
    	focusToView(this);
    }
	
	/* PRIVATE METHODS */
	
	private void init () {
		UnityAdsUtils.Log("Init", this);
		this.setId(1001);
		createWebView();
	}
	
	private void destroyVideoPlayerView () {
		UnityAdsUtils.Log("Destroying player", this);
		
		if (videoplayerview != null)
			videoplayerview.clearVideoPlayer();
		
		removeFromMainView(videoplayerview);
		videoplayerview = null;
	}
	
	private void createVideoPlayerView () {
		videoplayerview = new UnityAdsVideoPlayView(UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext(), this);
		videoplayerview.setLayoutParams(new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
		videoplayerview.setId(1002);
		addView(videoplayerview);
	}
	
	private void createWebView () {
		webview = new UnityAdsWebView(UnityAdsProperties.CURRENT_ACTIVITY, this, new UnityAdsWebBridge(UnityAds.instance));
		webview.setId(1003);
		addView(webview, new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
	}
	
	private void removeFromMainView (View view) {
		if (view != null) {
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
			
			ViewGroup vg = (ViewGroup)view.getParent();
			if (vg != null)
				vg.removeView(view);
		}
	}
	
	private void focusToView (View view) {
		if (view != null) {
			view.setFocusable(true);
			view.setFocusableInTouchMode(true);
			view.requestFocus();
		}
	}
	
	private void sendActionToListener (UnityAdsMainViewAction action) {
		if (_listener != null) {
			_listener.onMainViewAction(action);
		}		
	}
	
	
	// IUnityAdsViewListener
	@Override
	public void onBackButtonClicked (View view) {
		UnityAdsUtils.Log("Current state: " + _currentState.toString(), this);
		
		if (videoplayerview != null) {
			UnityAdsUtils.Log("Seconds: " + videoplayerview.getSecondsUntilBackButtonAllowed(), this);
		}
		
		if ((UnityAdsProperties.SELECTED_CAMPAIGN != null &&
			UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) ||
			_currentState != UnityAdsMainViewState.VideoPlayer || 
			(_currentState == UnityAdsMainViewState.VideoPlayer && videoplayerview != null && videoplayerview.getSecondsUntilBackButtonAllowed() == 0)) {
			sendActionToListener(UnityAdsMainViewAction.BackButtonPressed);
		}
		else {
			UnityAdsUtils.Log("Prevented back-button", this);
		}
	}
	
	// IUnityAdsVideoPlayerListener
	@Override
	public void onVideoPlaybackStarted () {
		UnityAdsUtils.Log("onVideoPlaybackStarted", this);
		
		JSONObject params = new JSONObject();
		JSONObject spinnerParams = new JSONObject();
		
		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
			spinnerParams.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not create JSON", this);
		}
		
		sendActionToListener(UnityAdsMainViewAction.VideoStart);
		bringChildToFront(videoplayerview);
		
		// SENSOR_LANDSCAPE
		int targetOrientation = 6;
		
		if (Build.VERSION.SDK_INT < 9)
			targetOrientation = 0;
		
		if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS != null && 
			UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION) && 
			UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION).equals(true)) {
			UnityAdsProperties.CURRENT_ACTIVITY.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			
			// UNSPECIFIED
			targetOrientation = -1;
		}
		
		UnityAdsProperties.CURRENT_ACTIVITY.setRequestedOrientation(targetOrientation);
		
		focusToView(videoplayerview);
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_HIDESPINNER, spinnerParams);
		webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_COMPLETED, params);
	}
	
	@Override
	public void onEventPositionReached (UnityAdsVideoPosition position) {
		if (UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignStatus().equals(UnityAdsCampaignStatus.VIEWED))
			UnityAds.webdata.sendCampaignViewProgress(UnityAdsProperties.SELECTED_CAMPAIGN, position);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		UnityAdsUtils.Log("onCompletion", this);
		afterVideoPlaybackOperations();
		onEventPositionReached(UnityAdsVideoPosition.End);
		
		JSONObject params = new JSONObject();
		
		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not create JSON", this);
		}
		
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);
		sendActionToListener(UnityAdsMainViewAction.VideoEnd);
	}
	
	public void onVideoPlaybackError () {
		afterVideoPlaybackOperations();
		
		UnityAdsUtils.Log("onVideoPlaybackError", this);		
		UnityAds.webdata.sendAnalyticsRequest(UnityAdsConstants.UNITY_ADS_ANALYTICS_EVENTTYPE_VIDEOERROR, UnityAdsProperties.SELECTED_CAMPAIGN);
		
		JSONObject errorParams = new JSONObject();
		JSONObject spinnerParams = new JSONObject();
		JSONObject params = new JSONObject();
		
		try {
			errorParams.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_VIDEOPLAYBACKERROR);
			spinnerParams.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not create JSON", this);
		}
		
		webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_COMPLETED, params);
		
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_SHOWERROR, errorParams);
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_HIDESPINNER, spinnerParams);
		
		UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
		UnityAdsProperties.SELECTED_CAMPAIGN = null;
	}
	
	public void onVideoSkip () {
		Map<String, Object> values = null;
		values = new HashMap<String, Object>();
		values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_BUFFERINGDURATION_KEY, videoplayerview.getBufferingDuration());
		values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VALUE_KEY, UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOABORT_SKIP);
		UnityAdsInstrumentation.gaInstrumentationVideoAbort(UnityAdsProperties.SELECTED_CAMPAIGN, values);
				
		afterVideoPlaybackOperations();
		JSONObject params = new JSONObject();
		
		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not create JSON", this);
		}
		
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);
		sendActionToListener(UnityAdsMainViewAction.VideoSkipped);
	}
	
	// IUnityAdsWebViewListener
	@Override
	public void onWebAppLoaded () {
		webview.initWebApp(UnityAds.webdata.getData());
	}
}