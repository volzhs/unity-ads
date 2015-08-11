package com.unity3d.ads.android.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.cache.UnityAdsCache;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.video.IUnityAdsVideoPlayerListener;
import com.unity3d.ads.android.webapp.IUnityAdsWebBridgeListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.zone.UnityAdsIncentivizedZone;
import com.unity3d.ads.android.zone.UnityAdsZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsFullscreenActivity extends Activity implements IUnityAdsWebBridgeListener, IUnityAdsVideoPlayerListener {

	private Boolean _preventVideoDoubleStart = false;
	private UnityAdsMainView _mainView = null;
	private int _pausedPosition = 0;
	private boolean _rewatch = false;
	private boolean _finishOperationsDone = false;

	private UnityAdsMainView getMainView () {
		return _mainView;
	}

	private void setupViews () {
		if (getMainView() != null) {
			UnityAdsDeviceLog.debug("View was not destroyed, trying to destroy it");
			_mainView = null;
		}

		if (getMainView() == null) {
			_mainView = new UnityAdsMainView(this, this);
		}
	}

	/* CLOSING */
	private void finishActivity() {
		if (_finishOperationsDone) return;
		_finishOperationsDone = true;

		UnityAdsDeviceLog.debug("Running finish operations on Unity Ads activity");
		if (UnityAdsWebData.getZoneManager() != null) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (!currentZone.openAnimated()) {
				overridePendingTransition(0, 0);
			}
		}

		final JSONObject data = new JSONObject();

		try  {
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_CLOSE);
		}
		catch (Exception e) {
			return;
		}

		if(getMainView() != null && UnityAdsMainView.webview != null) {
			UnityAdsMainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE, data);
		}

		UnityAdsViewUtils.removeViewFromParent(UnityAdsMainView.webview);

		if(getMainView() != null) {
			UnityAdsViewUtils.removeViewFromParent(getMainView());

			if (getMainView().videoplayerview != null)
				getMainView().videoplayerview.clearVideoPlayer();

			UnityAdsViewUtils.removeViewFromParent(getMainView().videoplayerview);
			UnityAdsProperties.SELECTED_CAMPAIGN = null;
			getMainView().videoplayerview = null;
		}

		if (UnityAds.getListener() != null)
			UnityAds.getListener().onHide();

		// Refresh campaigns or cache next video
		if (!UnityAdsWebData.refreshCampaignsIfNeeded()) {
			ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();
			if (viewableCampaigns != null && viewableCampaigns.size() > 0) {
				UnityAdsCampaign nextCampaign = viewableCampaigns.get(0);

				if (!UnityAdsCache.isCampaignCached(nextCampaign) && nextCampaign.allowCacheVideo()) {
					UnityAdsCache.cacheCampaign(nextCampaign);
				}
			}
		}
	}

	private void changeOrientation () {
		int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

		// With Samsung Galaxy S4 Android 5.0 "Do not keep activities" -option statics might get nulled and all state is wrong
		if(UnityAdsWebData.getZoneManager() == null) {
			UnityAdsDeviceLog.error("Static state lost, finishing activity");
			finish();
			return;
		}

		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.useDeviceOrientationForVideo()) {
			targetOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		}

		setRequestedOrientation(targetOrientation);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		UnityAdsDeviceLog.entered();
		super.onCreate(savedInstanceState);

		// Restore application context always on activity create. Fix for Android 5.0 on Samsung Galaxy S4.
		UnityAdsProperties.APPLICATION_CONTEXT = getApplicationContext();

		UnityAds.changeActivity(this);

		// If WebView is gone for some reason (seen on Android 5.0 Samsung Galaxy S4), reinitialize WebView
		if (UnityAdsMainView.webview == null) UnityAdsMainView.initWebView();

		setupViews();
		setContentView(getMainView());
		changeOrientation();
		create();
		_preventVideoDoubleStart = false;
	}

	private void create() {
		Boolean dataOk = true;
		final JSONObject data = new JSONObject();
		final String view = UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE;

		try  {
			UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();

			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_OPEN);
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ZONE_KEY, zone.getZoneId());

			if(zone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone)zone).itemManager();
				data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_REWARD_ITEM_KEY, itemManager.getCurrentItem().getKey());
			}
		}
		catch (Exception e) {
			dataOk = false;
		}

		if (dataOk) {
			UnityAdsDeviceLog.debug("Setting up WebView with view:" + view + " and data:" + data.toString());

			if (getMainView() != null) {
				UnityAdsMainView.webview.setWebViewCurrentView(view, data);
				getMainView().setViewState(UnityAdsMainView.UnityAdsMainViewState.WebView);

				UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
				if (currentZone.noOfferScreen()) {
					playVideo(false);
				}

				if (UnityAds.getListener() != null)
					UnityAds.getListener().onShow();
			}
			else {
				UnityAdsDeviceLog.error("mainview null after open, closing");
				finish();
			}
		}
	}

	@Override
	public void onStart() {
		UnityAdsDeviceLog.entered();
		super.onStart();
	}

	@Override
	public void onRestart() {
		UnityAdsDeviceLog.entered();
		super.onRestart();
	}

	@Override
	public void onResume() {
		super.onResume();
		UnityAdsDeviceLog.entered();
		resumeVideo();
	}

	private void resumeVideo () {
		if (_pausedPosition > 0) {
			try {
				getMainView().videoplayerview.seekTo(_pausedPosition);
			}
			catch (Exception e) {
				UnityAdsDeviceLog.debug("Unexpected error while seeking video");
			}

			resetPausedPosition();
		}
	}

	@Override
	public void onPause() {
		UnityAdsDeviceLog.entered();
		pauseVideo();
		if (isFinishing()) finishActivity();
		super.onPause();
	}

	@Override
	public void onStop() {
		UnityAdsDeviceLog.entered();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		UnityAdsDeviceLog.entered();
		if (isFinishing()) finishActivity();
		super.onDestroy();
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				UnityAdsDeviceLog.entered();
				if (getMainView().videoplayerview != null) {
					UnityAdsDeviceLog.debug("Seconds: " + getMainView().videoplayerview.getSecondsUntilBackButtonAllowed());
				}

				if ((UnityAdsProperties.SELECTED_CAMPAIGN != null && UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
					finish();
				}
				else if (getMainView().getViewState() != UnityAdsMainView.UnityAdsMainViewState.VideoPlayer) {
					finish();
				}
				else if (getMainView().videoplayerview != null && getMainView().videoplayerview.getSecondsUntilBackButtonAllowed() == 0) {
					finish();
				}
				else if (UnityAdsWebData.getZoneManager().getCurrentZone().disableBackButtonForSeconds() == 0) {
					finish();
				}
				else {
					UnityAdsDeviceLog.debug("Prevented back-button");
				}

				return true;
		}

		return false;
	}

	/* IUnityAdsWebBrigeListener */

	@Override
	public void onPlayVideo(JSONObject data) {
		UnityAdsDeviceLog.entered();
		if (data.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY)) {
			String campaignId = null;

			try {
				campaignId = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY);
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Could not get campaignId");
			}

			if (campaignId != null) {
				if (UnityAdsWebData.getCampaignById(campaignId) != null) {
					UnityAdsProperties.SELECTED_CAMPAIGN = UnityAdsWebData.getCampaignById(campaignId);
				}

				if (UnityAdsProperties.SELECTED_CAMPAIGN != null &&
						UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() != null &&
						UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId().equals(campaignId)) {

					Boolean rewatch = false;

					try {
						rewatch = data.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_REWATCH_KEY);
					}
					catch (Exception e) {
						UnityAdsDeviceLog.debug("Couldn't get rewatch property");
					}

					if (rewatch) {
						resetPausedPosition();
					}

					UnityAdsDeviceLog.debug("Selected campaign=" + UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() + " isViewed: " + UnityAdsProperties.SELECTED_CAMPAIGN.isViewed());
					if (UnityAdsProperties.SELECTED_CAMPAIGN != null && (rewatch || !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
						if(rewatch) {
							_preventVideoDoubleStart = false;
						}

						playVideo(rewatch);
					}
				}
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
		String msg = "WebView requested PauseVideo";
		if (data != null) msg += " " + data.toString();
		UnityAdsDeviceLog.debug(msg);
		pauseVideo();
	}

	private void pauseVideo () {
		if (getMainView() != null && getMainView().videoplayerview != null && getMainView().videoplayerview.isPlaying()) {
			int videoRewindTime = 500;
			_pausedPosition = getMainView().videoplayerview.getCurrentPosition() - videoRewindTime;
			if (_pausedPosition < 0) resetPausedPosition();
			getMainView().videoplayerview.pauseVideo();
		}
	}

	private void resetPausedPosition () {
		_pausedPosition = 0;
	}

	@Override
	public void onCloseAdsView(JSONObject data) {
		String msg = "WebView requested CloseAdsView";
		if (data != null) msg += " " + data.toString();
		UnityAdsDeviceLog.debug(msg);
		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		});
	}

	@Override
	public void onWebAppLoadComplete(JSONObject data) {
		String msg = "WebView reported WebAppLoadComplete";
		if (data != null) msg += " " + data.toString();
		UnityAdsDeviceLog.debug(msg);
		UnityAdsDeviceLog.entered();
	}

	@Override
	public void onWebAppInitComplete(JSONObject data) {
		UnityAdsDeviceLog.entered();
	}

	@SuppressWarnings("ResourceType")
	@Override
	public void onOrientationRequest(JSONObject data) {
		setRequestedOrientation(data.optInt("orientation", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
	}

	/* LAUNCH INTENT */

	@Override
	public void onLaunchIntent(JSONObject data) {
		try {
			Intent intent = parseLaunchIntent(data);

			if(intent == null) {
				UnityAdsDeviceLog.error("No suitable intent to launch");
				UnityAdsDeviceLog.debug("Intent JSON: " + data.toString());
				return;
			}

			startActivity(intent);
		} catch(Exception e) {
			UnityAdsDeviceLog.error("Failed to launch intent: " + e.getMessage());
		}
	}

	private Intent parseLaunchIntent(JSONObject data) {
		try {
			if(data.has("packageName") && !data.has("className") && !data.has("action") && !data.has("mimeType")) {
				PackageManager pm = getPackageManager();
				Intent intent = pm.getLaunchIntentForPackage(data.getString("packageName"));

				if(intent != null && data.has("flags")) {
					intent.addFlags(data.getInt("flags"));
				}

				return intent;
			}

			Intent intent = new Intent();

			if(data.has("className") && data.has("packageName")) {
				intent.setClassName(data.getString("packageName"), data.getString("className"));
			}

			if(data.has("action")) {
				intent.setAction(data.getString("action"));
			}

			if(data.has("uri")) {
				intent.setData(Uri.parse(data.getString("uri")));
			}

			if(data.has("mimeType")) {
				intent.setType(data.getString("mimeType"));
			}

			if(data.has("categories")) {
				JSONArray array = data.getJSONArray("categories");

				if(array.length() > 0) {
					for(int i = 0; i < array.length(); i++) {
						intent.addCategory(array.getString(i));
					}
				}
			}

			if(data.has("flags")) {
				intent.setFlags(data.getInt("flags"));
			}

			if(data.has("extras")) {
				JSONArray array = data.getJSONArray("extras");

				for(int i = 0; i < array.length(); i++) {
					JSONObject item = array.getJSONObject(i);

					String key = item.getString("key");
					Object value = item.get("value");

					if(value instanceof String) {
						intent.putExtra(key, (String)value);
					} else if(value instanceof Integer) {
						intent.putExtra(key, ((Integer)value).intValue());
					} else if(value instanceof Double) {
						intent.putExtra(key, ((Double)value).doubleValue());
					} else if(value instanceof Boolean) {
						intent.putExtra(key, ((Boolean)value).booleanValue());
					} else {
						UnityAdsDeviceLog.error("Unable to parse launch intent extra " + key);
					}
				}
			}

			return intent;
		} catch(JSONException e) {
			UnityAdsDeviceLog.error("Exception while parsing intent json: " + e.getMessage());
			return null;
		}
	}

	/* PLAY STORE */

	@Override
	public void onOpenPlayStore(JSONObject data) {
		UnityAdsDeviceLog.entered();

		if (data != null) {
			UnityAdsDeviceLog.debug(data.toString());
			String playStoreId = null;
			String clickUrl = null;
			Boolean bypassAppSheet = false;

			if (data.has(UnityAdsConstants.UNITY_ADS_PLAYSTORE_ITUNESID_KEY)) {
				try {
					playStoreId = data.getString(UnityAdsConstants.UNITY_ADS_PLAYSTORE_ITUNESID_KEY);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Could not fetch playStoreId");
				}
			}

			if (data.has(UnityAdsConstants.UNITY_ADS_PLAYSTORE_CLICKURL_KEY)) {
				try {
					clickUrl = data.getString(UnityAdsConstants.UNITY_ADS_PLAYSTORE_CLICKURL_KEY);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Could not fetch clickUrl");
				}
			}

			if (data.has(UnityAdsConstants.UNITY_ADS_PLAYSTORE_BYPASSAPPSHEET_KEY)) {
				try {
					bypassAppSheet = data.getBoolean(UnityAdsConstants.UNITY_ADS_PLAYSTORE_BYPASSAPPSHEET_KEY);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Could not fetch bypassAppSheet");
				}
			}

			if (playStoreId != null && !bypassAppSheet) {
				openPlayStoreAsIntent(playStoreId);
			}
			else if (clickUrl != null ){
				openPlayStoreInBrowser(clickUrl);
			}
		}
	}

	private void openPlayStoreAsIntent (String playStoreId) {
		UnityAdsDeviceLog.debug("Opening playstore activity with storeId: " + playStoreId);

		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + playStoreId)));
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Couldn't create PlayStore intent!");
		}
	}

	private void openPlayStoreInBrowser (String url) {
		UnityAdsDeviceLog.debug("Opening playStore in browser: " + url);

		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Couldn't create browser intent!");
		}
	}

	/* VIDEO PLAYBACK */

	@SuppressWarnings("SameParameterValue")
	private void playVideo(boolean rewatch) {
		if(_preventVideoDoubleStart) {
			UnityAdsDeviceLog.debug("Prevent double create of video playback");
			return;
		}

		_preventVideoDoubleStart = true;
		_rewatch = rewatch;
		UnityAdsDeviceLog.debug("Running threaded");
		UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
		playVideoRunner.setVideoPlayerListener(this);
		UnityAdsUtils.runOnUiThread(playVideoRunner);
	}

	private class UnityAdsPlayVideoRunner implements Runnable {
		private IUnityAdsVideoPlayerListener _listener = null;

		public void setVideoPlayerListener (IUnityAdsVideoPlayerListener listener) {
			_listener = listener;
		}

		@Override
		public void run() {
			UnityAdsDeviceLog.entered();
			if (UnityAdsProperties.SELECTED_CAMPAIGN != null) {
				UnityAdsDeviceLog.debug("Selected campaign found");
				JSONObject data = new JSONObject();

				try {
					data.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
				}
				catch (Exception e) {
					UnityAdsDeviceLog.error("Couldn't create data JSON");
					return;
				}

				String playUrl;
				if (!UnityAdsCache.isCampaignCached(UnityAdsProperties.SELECTED_CAMPAIGN)) {
					playUrl = UnityAdsProperties.SELECTED_CAMPAIGN.getVideoStreamUrl();
					UnityAdsProperties.SELECTED_CAMPAIGN_CACHED = false;
				} else {
					playUrl = UnityAdsCache.getCacheDirectory() + "/" + UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename();
					UnityAdsProperties.SELECTED_CAMPAIGN_CACHED = true;
				}

				getMainView().setViewState(UnityAdsMainView.UnityAdsMainViewState.VideoPlayer);
				getMainView().videoplayerview.setListener(_listener);
				UnityAdsDeviceLog.debug("Start videoplayback with: " + playUrl);
				getMainView().videoplayerview.playVideo(playUrl, UnityAdsProperties.SELECTED_CAMPAIGN_CACHED);
			}
			else
				UnityAdsDeviceLog.error("Campaign is null");
		}
	}

	private void finishPlayback () {
		if (getMainView().videoplayerview != null) {
			getMainView().videoplayerview.setKeepScreenOn(false);
		}

		resetPausedPosition();
		getMainView().destroyVideoPlayerView();
		getMainView().setViewState(UnityAdsMainView.UnityAdsMainViewState.WebView);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	/* IUnityAdsVideoPlayerListener */

	@Override
	public void onVideoPlaybackStarted () {
		UnityAdsDeviceLog.entered();
		JSONObject params = new JSONObject();

		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		} catch (Exception e) {
			UnityAdsDeviceLog.error("Could not create JSON");
		}

		if (UnityAds.getListener() != null && !_rewatch)
			UnityAds.getListener().onVideoStarted();

		ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAdsWebData.getViewableVideoPlanCampaigns();
		if(viewableCampaigns.size() > 1) {
			UnityAdsCampaign nextCampaign = viewableCampaigns.get(1);

			if(UnityAdsCache.isCampaignCached(UnityAdsProperties.SELECTED_CAMPAIGN) && !UnityAdsCache.isCampaignCached(nextCampaign) && nextCampaign.allowCacheVideo()) {
				UnityAdsCache.cacheCampaign(nextCampaign);
			}
		}

		getMainView().bringChildToFront(getMainView().videoplayerview);
		changeOrientation();

		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsDeviceLog.debug("Could not set campaign");
		}

		if (UnityAdsMainView.webview != null) {
			UnityAdsMainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_COMPLETED, params);
		}
	}

	@Override
	public void onEventPositionReached (UnityAdsWebData.UnityAdsVideoPosition position) {
		if (UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignStatus().equals(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED)) {
			boolean success = UnityAdsWebData.sendCampaignViewProgress(UnityAdsProperties.SELECTED_CAMPAIGN, position);
			if (!success) UnityAdsDeviceLog.debug("Sending campaign view progress failed!");
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		UnityAdsDeviceLog.entered();
		finishPlayback();
		onEventPositionReached(UnityAdsWebData.UnityAdsVideoPosition.End);

		JSONObject params = new JSONObject();

		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Could not create JSON");
		}

		UnityAdsMainView.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);

		UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_COUNT++;
		if (UnityAds.getListener() != null && UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) {
			UnityAdsDeviceLog.info("Unity Ads video completed");
			UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED);
			UnityAds.getListener().onVideoCompleted(getRewardItemKey(), false);
		}
	}

	@Override
	public void onVideoPlaybackError () {
		finishPlayback();

		UnityAdsDeviceLog.entered();
		UnityAdsWebData.sendAnalyticsRequest(UnityAdsConstants.UNITY_ADS_ANALYTICS_EVENTTYPE_VIDEOERROR, UnityAdsProperties.SELECTED_CAMPAIGN);

		JSONObject errorParams = new JSONObject();
		JSONObject spinnerParams = new JSONObject();
		JSONObject params = new JSONObject();

		try {
			errorParams.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_VIDEOPLAYBACKERROR);
			spinnerParams.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Could not create JSON");
		}

		if(UnityAdsMainView.webview != null) {
			UnityAdsMainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_COMPLETED, params);
			UnityAdsMainView.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_SHOWERROR, errorParams);
			UnityAdsMainView.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);
		}

		if(UnityAdsProperties.SELECTED_CAMPAIGN != null) {
			UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED);
			UnityAdsProperties.SELECTED_CAMPAIGN = null;
		}
	}

	@Override
	public void onVideoSkip () {
		finishPlayback();
		JSONObject params = new JSONObject();

		try {
			params.put(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_CAMPAIGNID_KEY, UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId());
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Could not create JSON");
		}

		UnityAdsMainView.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_VIDEOCOMPLETED, params);
		UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_COUNT++;

		if (UnityAds.getListener() != null && UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) {
			UnityAdsDeviceLog.info("Unity Ads video skipped");
			UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED);
			UnityAds.getListener().onVideoCompleted(getRewardItemKey(), true);
		}
	}

	private static String getRewardItemKey() {
		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		String rewardItemKey = null;
		if (currentZone != null && currentZone.isIncentivized()) {
			rewardItemKey = ((UnityAdsIncentivizedZone)currentZone).itemManager().getCurrentItem().getKey();
		}

		return rewardItemKey == null ? "" : rewardItemKey;
	}
}