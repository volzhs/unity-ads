package com.unity3d.ads.android;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.UnityAdsRewardItem;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsMainView;
import com.unity3d.ads.android.view.IUnityAdsMainViewListener;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewAction;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewState;
import com.unity3d.ads.android.webapp.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;


public class UnityAds implements IUnityAdsCacheListener, 
										IUnityAdsWebDataListener, 
										IUnityAdsWebBrigeListener,
										IUnityAdsMainViewListener {
	
	// Reward item HashMap keys
	public static final String UNITY_ADS_REWARDITEM_PICTURE_KEY = "picture";
	public static final String UNITY_ADS_REWARDITEM_NAME_KEY = "name";
	
	// Unity Ads developer options keys
	public static final String UNITY_ADS_OPTION_NOOFFERSCREEN_KEY = "noOfferScreen";
	public static final String UNITY_ADS_OPTION_OPENANIMATED_KEY = "openAnimated";
	public static final String UNITY_ADS_OPTION_GAMERSID_KEY = "sid";

	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	
	// Temporary data
	private boolean _initialized = false;
	private boolean _showingAds = false;
	private boolean _adsReadySent = false;
	private boolean _webAppLoaded = false;
	private boolean _openRequestFromDeveloper = false;
	private Map<String, Object> _developerOptions = null;
	private AlertDialog _alertDialog = null;
		
	// Main View
	private UnityAdsMainView _mainView = null;
	
	// Listeners
	private IUnityAdsListener _adsListener = null;
	
	
	public UnityAds (Activity activity, String gameId) {
		init(activity, gameId, null);
	}
	
	public UnityAds (Activity activity, String gameId, IUnityAdsListener listener) {
		init(activity, gameId, listener);
	}
	
	
	/* PUBLIC STATIC METHODS */
	
	public static boolean isSupported () {
		if (Build.VERSION.SDK_INT < 9) {
			return false;
		}
		
		return false;
	}
	
	public static void setDebugMode (boolean debugModeEnabled) {
		UnityAdsProperties.UNITY_ADS_DEBUG_MODE = debugModeEnabled;
	}
	
	public static void setTestMode (boolean testModeEnabled) {
		UnityAdsProperties.TESTMODE_ENABLED = testModeEnabled;
	}
	
	public static String getSDKVersion () {
		return UnityAdsConstants.UNITY_ADS_VERSION;
	}
	
	
	/* PUBLIC METHODS */
	
	public void setListener (IUnityAdsListener listener) {
		_adsListener = listener;
	}
	
	public void changeActivity (Activity activity) {
		if (activity == null) return;
		
		if (!activity.equals(UnityAdsProperties.CURRENT_ACTIVITY)) {
			UnityAdsProperties.CURRENT_ACTIVITY = activity;
			
			// Not the most pretty way to detect when the fullscreen activity is ready
			if (activity.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				String view = _mainView.webview.getWebViewCurrentView();
				if (_openRequestFromDeveloper) {
					view = UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START;
					UnityAdsUtils.Log("changeActivity: This open request is from the developer, setting start view", this);
				}
				
				open(view);
				_openRequestFromDeveloper = false;
			}
			else {
				UnityAdsProperties.BASE_ACTIVITY = activity;
			}
		}
	}
	
	public boolean hide () {
		if (_showingAds) {
			close();
			return true;
		}
		
		return false;
	}
	
	public boolean show (Map<String, Object> options) {
		if (canShow()) {
			_developerOptions = options;
			
			if (_developerOptions != null) {
				if (_developerOptions.containsKey(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY) && _developerOptions.get(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY).equals(true)) {
					if (webdata.getViewableVideoPlanCampaigns().size() > 0) {
						UnityAdsCampaign selectedCampaign = webdata.getViewableVideoPlanCampaigns().get(0);
						UnityAdsProperties.SELECTED_CAMPAIGN = selectedCampaign;
					}
				}
				if (_developerOptions.containsKey(UNITY_ADS_OPTION_GAMERSID_KEY) && _developerOptions.get(UNITY_ADS_OPTION_GAMERSID_KEY) != null) {
					UnityAdsProperties.GAMER_SID = "" + _developerOptions.get(UNITY_ADS_OPTION_GAMERSID_KEY);
				}
			}
			
			return show();
		}
		
		return false;
	}
	
	public boolean show () {
		if (canShow()) {
			startAdsFullscreenActivity();
			_showingAds = true;
			_openRequestFromDeveloper = true;
			return _showingAds;
		}

		return false;
	}
	
	public boolean canShowAds () {
		return _mainView != null && _mainView.webview != null && _mainView.webview.isWebAppLoaded() && _webAppLoaded && webdata != null && webdata.getViewableVideoPlanCampaigns().size() > 0;
	}
	
	public boolean canShow () {
		return !_showingAds && _mainView != null && _mainView.webview != null && _mainView.webview.isWebAppLoaded() && _webAppLoaded && webdata != null && webdata.getVideoPlanCampaigns().size() > 0;
	}

	public void stopAll () {
		UnityAdsUtils.Log("stopAll()", this);
		UnityAdsDownloader.stopAllDownloads();
		webdata.stopAllRequests();
		UnityAdsProperties.BASE_ACTIVITY = null;
		UnityAdsProperties.CURRENT_ACTIVITY = null;
		UnityAdsProperties.SELECTED_CAMPAIGN = null;
	}
	
	
	/* PUBLIC MULTIPLE REWARD ITEM SUPPORT */
	
	public boolean hasMultipleRewardItems () {
		if (webdata.getRewardItems() != null && webdata.getRewardItems().size() > 0)
			return true;
		
		return false;
	}
	
	public ArrayList<String> getRewardItemKeys () {
		if (webdata.getRewardItems() != null && webdata.getRewardItems().size() > 0) {
			ArrayList<UnityAdsRewardItem> rewardItems = webdata.getRewardItems();
			ArrayList<String> rewardItemKeys = new ArrayList<String>();
			for (UnityAdsRewardItem rewardItem : rewardItems) {
				rewardItemKeys.add(rewardItem.getKey());
			}
			
			return rewardItemKeys;
		}
		
		return null;
	}
	
	public String getDefaultRewardItemKey () {
		if (webdata != null && webdata.getDefaultRewardItem() != null)
			return webdata.getDefaultRewardItem().getKey();
		
		return null;
	}
	
	public String getCurrentRewardItemKey () {
		if (webdata != null && webdata.getCurrentRewardItemKey() != null)
			return webdata.getCurrentRewardItemKey();
			
		return null;
	}
	
	public boolean setRewardItemKey (String rewardItemKey) {
		UnityAdsRewardItem rewardItem = webdata.getRewardItemByKey(rewardItemKey);
		
		if (rewardItem != null) {
			webdata.setCurrentRewardItem(rewardItem);
			return true;
		}
		
		return false;
	}
	
	public void setDefaultRewardItemAsRewardItem () {
		if (webdata != null && webdata.getDefaultRewardItem() != null) {
			webdata.setCurrentRewardItem(webdata.getDefaultRewardItem());
		}
	}
	
	public Map<String, String> getRewardItemDetailsWithKey (String rewardItemKey) {
		UnityAdsRewardItem rewardItem = webdata.getRewardItemByKey(rewardItemKey);
		if (rewardItem != null) {
			return rewardItem.getDetails();
		}
		
		return null;
	}
	
	
	/* LISTENER METHODS */
	
	// IUnityAdsMainViewListener
	public void onMainViewAction (UnityAdsMainViewAction action) {
		switch (action) {
			case BackButtonPressed:
				close();
				break;
			case VideoStart:
				if (_adsListener != null)
					_adsListener.onVideoStarted();
				break;
			case VideoEnd:
				if (_adsListener != null)
					_adsListener.onVideoCompleted();
				break;
		}
	}
	
	
	// IUnityAdsCacheListener
	@Override
	public void onCampaignUpdateStarted () {	
		UnityAdsUtils.Log("Campaign updates started.", this);
	}
	
	@Override
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;
				
		UnityAdsUtils.Log("Got onCampaignReady: " + campaignHandler.getCampaign().toString(), this);
		
		if (canShowAds())
			sendAdsReadyEvent();
	}
	
	@Override
	public void onAllCampaignsReady () {
		UnityAdsUtils.Log("Listener got \"All campaigns ready.\"", this);
	}
	
	// IUnityAdsWebDataListener
	@Override
	public void onWebDataCompleted () {
		JSONObject jsonData = null;
		boolean dataFetchFailed = false;
		String nativeSdkVersion = null;
		
		if (webdata.getData() != null && webdata.getData().has(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY)) {
			try {
				jsonData = webdata.getData().getJSONObject(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY);
			}
			catch (Exception e) {
				dataFetchFailed = true;
			}
			
			if (!dataFetchFailed) {
				if (jsonData.has(UnityAdsConstants.UNITY_ADS_NATIVESDKVERSION_KEY)) {
					try {
						nativeSdkVersion = jsonData.getString(UnityAdsConstants.UNITY_ADS_NATIVESDKVERSION_KEY);
					}
					catch (Exception e) {
						dataFetchFailed = true;
					}
				}
			}
		}
		
		if (nativeSdkVersion != null && !dataFetchFailed && UnityAdsUtils.isDebuggable(UnityAdsProperties.CURRENT_ACTIVITY)) {
			if (!nativeSdkVersion.equals(UnityAdsConstants.UNITY_ADS_VERSION)) {
				_alertDialog = new AlertDialog.Builder(UnityAdsProperties.CURRENT_ACTIVITY).create();
				_alertDialog.setTitle("Unity Ads");
				_alertDialog.setMessage("You are not running the latest version of Unity Ads android. Please update your version (this dialog won't appear in release builds).");
				_alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						_alertDialog.dismiss();
					}
				});
				
				_alertDialog.show();
			}
		}
		
		setup();
	}
	
	@Override
	public void onWebDataFailed () {
		if (_adsListener != null)
			_adsListener.onFetchFailed();
	}
	
	
	// IUnityAdsWebBrigeListener
	@Override
	public void onPlayVideo(JSONObject data) {
		UnityAdsUtils.Log("onPlayVideo", this);
		if (data.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY)) {
			String campaignId = null;
			
			try {
				campaignId = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get campaignId", this);
			}
			
			if (campaignId != null) {
				UnityAdsProperties.SELECTED_CAMPAIGN = webdata.getCampaignById(campaignId);
				Boolean rewatch = false;
				
				try {
					rewatch = data.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_REWATCH_KEY);
				}
				catch (Exception e) {
				}
				
				UnityAdsUtils.Log("onPlayVideo: Selected campaign=" + UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() + " isViewed: " + UnityAdsProperties.SELECTED_CAMPAIGN.isViewed(), this);
				if (UnityAdsProperties.SELECTED_CAMPAIGN != null && (rewatch || !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
					playVideo();
				}
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
	}

	@Override
	public void onCloseAdsView(JSONObject data) {
		hide();
	}
	
	@Override
	public void onWebAppInitComplete (JSONObject data) {
		UnityAdsUtils.Log("WebApp init complete", this);
		_webAppLoaded = true;
		Boolean dataOk = true;
		
		if (canShowAds()) {
			JSONObject setViewData = new JSONObject();
			
			try {
				setViewData.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_INITCOMPLETE);
				setViewData.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
			}
			catch (Exception e) {
				dataOk = false;
			}
			
			if (dataOk) {
				_mainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, setViewData);
				sendAdsReadyEvent();			
			}
		}
	}
	
	public void onOpenPlayStore (JSONObject data) {
	    UnityAdsUtils.Log("onOpenPlayStore", this);
		if (UnityAdsProperties.SELECTED_CAMPAIGN != null && UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId() != null) {
			try {
			    UnityAdsUtils.Log("Opening playstore activity with storeId: " + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId(), this);
				UnityAdsProperties.CURRENT_ACTIVITY.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId())));
			} 
			catch (android.content.ActivityNotFoundException anfe) {
			    UnityAdsUtils.Log("Could not open PlayStore activity, opening in browser with storeId: " + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId(), this);
				UnityAdsProperties.CURRENT_ACTIVITY.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId())));
			}
		}
		else {
		    UnityAdsUtils.Log("Selected campaign (" + UnityAdsProperties.SELECTED_CAMPAIGN + ") or couldn't get storeId", this);
		}
	}
	

	/* PRIVATE METHODS */
	
	private void init (Activity activity, String gameId, IUnityAdsListener listener) {
		instance = this;
		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
		UnityAdsProperties.BASE_ACTIVITY = activity;
		UnityAdsProperties.CURRENT_ACTIVITY = activity;
		
		//UnityAdsUtils.Log("Is debuggable=" + UnityAdsUtils.isDebuggable(activity), this);
		
		if (_initialized) return; 
		
		cachemanager = new UnityAdsCacheManager();
		cachemanager.setDownloadListener(this);
		webdata = new UnityAdsWebData();
		webdata.setWebDataListener(this);

		if (webdata.initCampaigns()) {
			_initialized = true;
		}
	}
	
	private void close () {
		UnityAdsCloseRunner closeRunner = new UnityAdsCloseRunner();
		UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(closeRunner);
	}
	
	private void open (String view) {
		Boolean dataOk = true;			
		JSONObject data = new JSONObject();
		
		try  {
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_OPEN);
			data.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
		}
		catch (Exception e) {
			dataOk = false;
		}

		UnityAdsUtils.Log("open() dataOk: " + dataOk, this);
		
		if (dataOk && view != null) {
			UnityAdsUtils.Log("open() opening with view:" + view + " and data:" + data.toString(), this);
			_mainView.openAds(view, data);
			
			if (_developerOptions != null && _developerOptions.containsKey(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY)  && _developerOptions.get(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY).equals(true))
				playVideo();
			
			if (_adsListener != null)
				_adsListener.onShow();
		}
	}

	private void setup () {
		initCache();
		setupViews();
	}
	
	private void initCache () {
		if (_initialized) {
			UnityAdsUtils.Log("Init cache", this);
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(webdata.getVideoPlanCampaigns());				
		}
	}
	
	private void sendAdsReadyEvent () {
		if (!_adsReadySent && _adsListener != null) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					UnityAdsUtils.Log("Unity Ads ready!", this);
					_adsReadySent = true;
					_adsListener.onFetchCompleted();
				}
			});
		}
	}

	private void setupViews () {
		_mainView = new UnityAdsMainView(UnityAdsProperties.CURRENT_ACTIVITY, this);
	}

	private void playVideo () {
		UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
		UnityAdsUtils.Log("Running threaded", this);
		UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(playVideoRunner);
	}
	
	private void startAdsFullscreenActivity () {
		Intent newIntent = new Intent(UnityAdsProperties.CURRENT_ACTIVITY, com.unity3d.ads.android.view.UnityAdsFullscreenActivity.class);
		int flags = Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK;
		
		if (_developerOptions != null && _developerOptions.containsKey(UNITY_ADS_OPTION_OPENANIMATED_KEY) && _developerOptions.get(UNITY_ADS_OPTION_OPENANIMATED_KEY).equals(true))
			flags = Intent.FLAG_ACTIVITY_NEW_TASK;
		
		newIntent.addFlags(flags);
		UnityAdsProperties.BASE_ACTIVITY.startActivity(newIntent);
	}
	
	
	/* INTERNAL CLASSES */

	// FIX: Could these 2 classes be moved to MainView
	
	private class UnityAdsCloseRunner implements Runnable {
		JSONObject _data = null;
		@Override
		public void run() {
			_showingAds = false;
			
			if (UnityAdsProperties.CURRENT_ACTIVITY.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				Boolean dataOk = true;			
				JSONObject data = new JSONObject();
				
				try  {
					data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_CLOSE);
				}
				catch (Exception e) {
					dataOk = false;
				}

				UnityAdsUtils.Log("dataOk: " + dataOk, this);
				
				if (dataOk) {
					_data = data;
					_mainView.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE, data);
					Timer testTimer = new Timer();
					testTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									_mainView.closeAds(_data);
									UnityAdsProperties.CURRENT_ACTIVITY.finish();
									
									if (_developerOptions == null || !_developerOptions.containsKey(UNITY_ADS_OPTION_OPENANIMATED_KEY) || _developerOptions.get(UNITY_ADS_OPTION_OPENANIMATED_KEY).equals(false))
										UnityAdsProperties.CURRENT_ACTIVITY.overridePendingTransition(0, 0);
									
									if (_adsListener != null)
										_adsListener.onHide();
								}
							});
						}
					}, 100);
				}
			}
			
			// Reset developer options when Unity Ads closes
			_developerOptions = null;
		}
	}
	
	private class UnityAdsPlayVideoRunner implements Runnable {
		@Override
		public void run() {			
			UnityAdsUtils.Log("Running videoplayrunner", this);
			if (UnityAdsProperties.SELECTED_CAMPAIGN != null) {
				UnityAdsUtils.Log("Selected campaign found", this);
				JSONObject data = new JSONObject();
				
				try {
					data.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Couldn't create data JSON", this);
					return;
				}
				
				_mainView.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_SHOWSPINNER, data);
				
				String playUrl = UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename();
				if (!UnityAdsUtils.isFileInCache(UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename()))
					playUrl = UnityAdsProperties.SELECTED_CAMPAIGN.getVideoStreamUrl(); 

				_mainView.setViewState(UnityAdsMainViewState.VideoPlayer);
				UnityAdsUtils.Log("Start videoplayback with: " + playUrl, this);
				_mainView.videoplayerview.playVideo(playUrl);
			}			
			else
				UnityAdsUtils.Log("Campaign is null", this);
		}		
	}
}
