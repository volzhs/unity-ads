package com.unity3d.ads.android.view;

import org.json.JSONObject;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.video.UnityAdsVideoPlayView;
import com.unity3d.ads.android.video.IUnityAdsVideoPlayerListener;
import com.unity3d.ads.android.webapp.UnityAdsWebBridge;
import com.unity3d.ads.android.webapp.UnityAdsWebView;
import com.unity3d.ads.android.webapp.IUnityAdsWebViewListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class UnityAdsMainView extends RelativeLayout implements 	IUnityAdsWebViewListener, 
																		IUnityAdsVideoPlayerListener {

	public static enum UnityAdsMainViewState { WebView, VideoPlayer };
	public static enum UnityAdsMainViewAction { VideoStart, VideoEnd, BackButtonPressed };
	
	// Views
	public UnityAdsVideoPlayView videoplayerview = null;
	public UnityAdsWebView webview = null;

	// Listener
	private IUnityAdsMainViewListener _listener = null;

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
				UnityAdsProperties.CURRENT_ACTIVITY.addContentView(this, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
			
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
		webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, data);
	}
	
	public void setViewState (UnityAdsMainViewState state) {
		switch (state) {
			case WebView:
				removeFromMainView(webview);
				addView(webview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
				focusToView(webview);
				break;
			case VideoPlayer:
				if (videoplayerview == null) {
					createVideoPlayerView();
					removeFromMainView(webview);
					addView(webview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
				}
				break;
		}
	}
	
	
	/* PRIVATE METHODS */
	
	private void init () {
		UnityAdsUtils.Log("Init", this);
		createVideoPlayerView();
		createWebView();
	}
	
	private void destroyVideoPlayerView () {
		UnityAdsUtils.Log("Destroying player", this);
		removeFromMainView(videoplayerview);
		videoplayerview = null;
	}
	
	private void createVideoPlayerView () {
		videoplayerview = new UnityAdsVideoPlayView(UnityAdsProperties.CURRENT_ACTIVITY.getBaseContext(), this);
		videoplayerview.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
		addView(videoplayerview);
	}
	
	private void createWebView () {
		webview = new UnityAdsWebView(UnityAdsProperties.CURRENT_ACTIVITY, this, new UnityAdsWebBridge(UnityAds.instance));
		addView(webview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
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
		sendActionToListener(UnityAdsMainViewAction.BackButtonPressed);
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
		UnityAdsProperties.CURRENT_ACTIVITY.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
		videoplayerview.setKeepScreenOn(false);
		destroyVideoPlayerView();
		setViewState(UnityAdsMainViewState.WebView);
		onEventPositionReached(UnityAdsVideoPosition.End);
		
		JSONObject params = new JSONObject();
		
		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not create JSON", this);
		}
		
		webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);

		
		UnityAdsProperties.CURRENT_ACTIVITY.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
		UnityAdsProperties.SELECTED_CAMPAIGN = null;
		
		sendActionToListener(UnityAdsMainViewAction.VideoEnd);
	}
	
	// IUnityAdsWebViewListener
	@Override
	public void onWebAppLoaded () {
		webview.initWebApp(UnityAds.webdata.getData());
	}
}