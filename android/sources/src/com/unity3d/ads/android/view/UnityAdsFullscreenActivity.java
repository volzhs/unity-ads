package com.unity3d.ads.android.view;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.webapp.IUnityAdsWebBridgeListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.zone.UnityAdsIncentivizedZone;
import com.unity3d.ads.android.zone.UnityAdsZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UnityAdsFullscreenActivity extends Activity implements IUnityAdsMainViewListener, IUnityAdsWebBridgeListener {

	private String _currentView = null;
	private Boolean _preventVideoDoubleStart = false;

	private boolean isAdsReadySent () {
		return UnityAdsProperties.UNITY_ADS_READY_SENT;
	}

	private void setAdsReadySent (boolean sent) {
		UnityAdsProperties.UNITY_ADS_READY_SENT = sent;
	}

	private void setupViews () {
		if (UnityAds.mainview != null) {
			UnityAdsDeviceLog.debug("View was not destroyed, trying to destroy it");
			UnityAds.mainview.webview.destroy();
			UnityAds.mainview = null;
		}

		if (UnityAds.mainview == null) {
			UnityAds.mainview = new UnityAdsMainView(this, this, this);
		}
	}

	private void sendReadyEvent () {
		if (!isAdsReadySent() && UnityAds.getListener() != null) {
			if (!isAdsReadySent()) {
				UnityAdsDeviceLog.debug("Unity Ads ready.");
				UnityAds.getListener().onFetchCompleted();
				setAdsReadySent(true);
			}
		}
	}

	private void open (final String view) {
		Boolean dataOk = true;
		final JSONObject data = new JSONObject();

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

		UnityAdsDeviceLog.debug("DataOk: " + dataOk);

		if (dataOk && view != null) {
			UnityAdsDeviceLog.debug("Opening with view:" + view + " and data:" + data.toString());

			if (UnityAds.mainview != null) {
				// TODO: ASK WHAT IS THIS ?
				/*
				if(!UnityAds.mainview.webview.isWebAppLoadComplete()) {
					UnityAds.mainview.webview.waitForWebAppLoadComplete();
				}*/
				if (UnityAds.mainview != null) {
					UnityAds.mainview.webview.setWebViewCurrentView(view, data);
					UnityAds.mainview.setViewState(UnityAdsMainView.UnityAdsMainViewState.WebView);
					UnityAds.mainview.openAds(view, data);

					UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
					if (currentZone.noOfferScreen()) {
						UnityAdsDeviceLog.debug("SHOULD PLAY VIDEO");
						playVideo();
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
	}

	/* CLOSING */

	@Override
	public void finish () {
		super.finish();
		if (UnityAdsWebData.getZoneManager() != null) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			if (!currentZone.openAnimated()) {
				overridePendingTransition(0, 0);
			}
		}

		hideOperations();
	}

	private static void hideOperations() {
		final JSONObject data = new JSONObject();

		try  {
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_CLOSE);
		}
		catch (Exception e) {
			return;
		}

		if(UnityAds.mainview != null && UnityAds.mainview.webview != null) {
			UnityAds.mainview.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE, data);
		}

		if(UnityAds.mainview != null) {
			// TODO: FIX DUPLICATE CODE
			if (UnityAds.mainview.getParent() != null) {
				ViewGroup vg = (ViewGroup)UnityAds.mainview.getParent();
				if (vg != null)
					vg.removeView(UnityAds.mainview);
			}

			if (UnityAds.mainview.videoplayerview != null)
				UnityAds.mainview.videoplayerview.clearVideoPlayer();

			// TODO: FIX DUPLICATE CODE
			if (UnityAds.mainview != null) {
				UnityAds.mainview.removeView(UnityAds.mainview.videoplayerview);
			}

			UnityAds.mainview.videoplayerview = null;

			UnityAdsProperties.SELECTED_CAMPAIGN = null;
			UnityAds.mainview.webview.destroy();
			UnityAds.mainview.webview = null;
			UnityAds.mainview = null;
		}

		if (UnityAds.getListener() != null)
			UnityAds.getListener().onHide();

		// TODO: Refresh campaigns
		//refreshCampaignsOrCacheNextVideo();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		UnityAdsDeviceLog.entered();   	
		super.onCreate(savedInstanceState);
		UnityAds.changeActivity(this);

		String view = UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START;

		if (view != null) {
			_currentView = view;
			setupViews();
			open(_currentView);
			setContentView(UnityAds.mainview);
		}

		_preventVideoDoubleStart = false;
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
		UnityAdsDeviceLog.entered();   	
		super.onResume();
	//	UnityAds.changeActivity(this);
   	//	UnityAds.checkMainview();
	}

	@Override
	public void onPause() {
		UnityAdsDeviceLog.entered();
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
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		return false;
	}

	@Override
	public void onMainViewAction(UnityAdsMainView.UnityAdsMainViewAction action) {
		switch (action) {
			case BackButtonPressed:
				finish();
				break;
			case VideoStart:
				if (UnityAds.getListener() != null)
					UnityAds.getListener().onVideoStarted();

				ArrayList<UnityAdsCampaign> viewableCampaigns = UnityAds.webdata.getViewableVideoPlanCampaigns();

				if(viewableCampaigns.size() > 1) {
					UnityAdsCampaign nextCampaign = viewableCampaigns.get(1);

					if(UnityAds.cachemanager.isCampaignCached(UnityAdsProperties.SELECTED_CAMPAIGN, true) && !UnityAds.cachemanager.isCampaignCached(nextCampaign, true) && nextCampaign.allowCacheVideo()) {
						UnityAds.cachemanager.cacheNextVideo(nextCampaign);
					}
				}

				// TODO: We need this?
				//cancelPauseScreenTimer();
				break;
			case VideoEnd:
				UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_COUNT++;
				if (UnityAds.getListener() != null && UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) {
					UnityAdsDeviceLog.info("Unity Ads video completed");
					UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED);
					UnityAds.getListener().onVideoCompleted(UnityAds.getCurrentRewardItemKey(), false);
				}
				break;
			case VideoSkipped:
				UnityAdsProperties.CAMPAIGN_REFRESH_VIEWS_COUNT++;
				if (UnityAds.getListener() != null && UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) {
					UnityAdsDeviceLog.info("Unity Ads video skipped");
					UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaign.UnityAdsCampaignStatus.VIEWED);
					UnityAds.getListener().onVideoCompleted(UnityAds.getCurrentRewardItemKey(), true);
				}
				break;
			case RequestRetryVideoPlay:
				UnityAdsDeviceLog.debug("Retrying video play, because something went wrong.");
				_preventVideoDoubleStart = false;
				playVideo(300);
				break;
		}
	}

	// IUnityAdsWebBrigeListener
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
				if (UnityAds.webdata != null && UnityAds.webdata.getCampaignById(campaignId) != null) {
					UnityAdsProperties.SELECTED_CAMPAIGN = UnityAds.webdata.getCampaignById(campaignId);
				}

				if (UnityAdsProperties.SELECTED_CAMPAIGN != null &&
						UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() != null &&
						UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId().equals(campaignId)) {

					Boolean rewatch = false;

					try {
						rewatch = data.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_REWATCH_KEY);
					}
					catch (Exception e) {
					}

					UnityAdsDeviceLog.debug("Selected campaign=" + UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() + " isViewed: " + UnityAdsProperties.SELECTED_CAMPAIGN.isViewed());
					if (UnityAdsProperties.SELECTED_CAMPAIGN != null && (rewatch || !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
						if(rewatch) {
							_preventVideoDoubleStart = false;
						}

						playVideo();
					}
				}
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
	}

	@Override
	public void onCloseAdsView(JSONObject data) {
		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		});
	}

	@Override
	public void onWebAppLoadComplete(JSONObject data) {
		UnityAdsDeviceLog.entered();
	}

	@Override
	public void onWebAppInitComplete(JSONObject data) {
		UnityAdsDeviceLog.entered();
		Boolean dataOk = true;

		if(UnityAds.webdata != null && UnityAds.webdata.hasViewableAds()) {
			JSONObject setViewData = new JSONObject();

			try {
				setViewData.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_INITCOMPLETE);
			}
			catch (Exception e) {
				dataOk = false;
			}

			if (dataOk) {
				UnityAds.mainview.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, setViewData);

				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						sendReadyEvent();
					}
				});
			}
		}
	}

	@Override
	public void onOrientationRequest(JSONObject data) {
		setRequestedOrientation(data.optInt("orientation", -1));
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

			Activity currentActivity = UnityAdsProperties.getCurrentActivity();
			if(currentActivity == null) {
				UnityAdsDeviceLog.error("Unable to launch intent: current activity is null");
				return;
			}

			currentActivity.startActivity(intent);
		} catch(Exception e) {
			UnityAdsDeviceLog.error("Failed to launch intent: " + e.getMessage());
		}
	}

	private static Intent parseLaunchIntent(JSONObject data) {
		try {
			if(data.has("packageName") && !data.has("className") && !data.has("action") && !data.has("mimeType")) {
				Activity currentActivity = UnityAdsProperties.getCurrentActivity();
				if(currentActivity == null) {
					UnityAdsDeviceLog.error("Unable to parse data to generate intent: current activity is null");
					return null;
				}

				PackageManager pm = currentActivity.getPackageManager();
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

	private static void openPlayStoreAsIntent (String playStoreId) {
		UnityAdsDeviceLog.debug("Opening playstore activity with storeId: " + playStoreId);

		if (UnityAdsProperties.getCurrentActivity() != null) {
			try {
				UnityAdsProperties.getCurrentActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + playStoreId)));
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Couldn't start PlayStore intent!");
			}
		}
	}

	private static void openPlayStoreInBrowser (String url) {
		UnityAdsDeviceLog.debug("Opening playStore in browser: " + url);

		if (UnityAdsProperties.getCurrentActivity() != null) {
			try {
				UnityAdsProperties.getCurrentActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Couldn't start browser intent!");
			}
		}
	}

	/* VIDEO PLAYBACK */

	private void playVideo () {
		playVideo(0);
	}

	private void playVideo (long delay) {
		if(_preventVideoDoubleStart) {
			UnityAdsDeviceLog.debug("Prevent double start of video playback");
			return;
		}
		_preventVideoDoubleStart = true;

		UnityAdsDeviceLog.debug("Running threaded");
		UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
		UnityAdsUtils.runOnUiThread(playVideoRunner, delay);
	}

	private static class UnityAdsPlayVideoRunner implements Runnable {
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

				UnityAds.mainview.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_SHOWSPINNER, data);

				// TODO: WHAT?
				//createPauseScreenTimer();

				String playUrl;
				if (!UnityAds.cachemanager.isCampaignCached(UnityAdsProperties.SELECTED_CAMPAIGN, true)) {
					playUrl = UnityAdsProperties.SELECTED_CAMPAIGN.getVideoStreamUrl();
					UnityAdsProperties.SELECTED_CAMPAIGN_CACHED = false;
				} else {
					playUrl = UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename();
					UnityAdsProperties.SELECTED_CAMPAIGN_CACHED = true;
				}

				UnityAds.mainview.setViewState(UnityAdsMainView.UnityAdsMainViewState.VideoPlayer);
				UnityAdsDeviceLog.debug("Start videoplayback with: " + playUrl);
				UnityAds.mainview.videoplayerview.playVideo(playUrl);
			}
			else
				UnityAdsDeviceLog.error("Campaign is null");
		}
	}
}
