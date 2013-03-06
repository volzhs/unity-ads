using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAds : MonoBehaviour {
	
	public bool showTestButton = false;
	public string gameId = "";
	public bool debugModeEnabled = false;
	public bool testModeEnabled = false;
	public bool openAnimated = false;
	public bool noOfferscreen = false;
	
	private static bool _campaignsAvailable = false;
	private static bool _initRun = false;
	private static bool _adsOpen = false;
	private static bool _gotAwake = false;
	private static string _gameObjectName = null;
	private static float _savedTimeScale = 1f;
	private static string _gamerSID = "";
	
	public delegate void UnityAdsCampaignsAvailable();
	private static UnityAdsCampaignsAvailable _campaignsAvailableDelegate;
	public static void setCampaignsAvailableDelegate (UnityAdsCampaignsAvailable action) {
		_campaignsAvailableDelegate = action;
	}

	public delegate void UnityAdsCampaignsFetchFailed();
	private static UnityAdsCampaignsFetchFailed _campaignsFetchFailedDelegate;
	public static void setCampaignsFetchFailedDelegate (UnityAdsCampaignsFetchFailed action) {
		_campaignsFetchFailedDelegate = action;
	}

	public delegate void UnityAdsOpen();
	private static UnityAdsOpen _adsOpenDelegate;
	public static void setOpenDelegate (UnityAdsOpen action) {
		_adsOpenDelegate = action;
	}
	
	public delegate void UnityAdsClose();
	private static UnityAdsClose _adsCloseDelegate;
	public static void setCloseDelegate (UnityAdsClose action) {
		_adsCloseDelegate = action;
	}

	public delegate void UnityAdsVideoCompleted(string rewardItemKey);
	private static UnityAdsVideoCompleted _videoCompletedDelegate;
	public static void setVideoCompletedDelegate (UnityAdsVideoCompleted action) {
		_videoCompletedDelegate = action;
	}
	
	public delegate void UnityAdsVideoStarted();
	private static UnityAdsVideoStarted _videoStartedDelegate;
	public static void setVideoStartedDelegate (UnityAdsVideoStarted action) {
		_videoStartedDelegate = action;
	}
	
	
	public void Awake () {
		if (_gotAwake == false) {
			_gotAwake = true;
			this.init(this.gameId, this.testModeEnabled, this.debugModeEnabled);
		}
	}
	
	public void OnDestroy () {
		_campaignsAvailableDelegate = null;
		_campaignsFetchFailedDelegate = null;
		_adsOpenDelegate = null;
		_adsCloseDelegate = null;
		_videoCompletedDelegate = null;
		_videoStartedDelegate = null;
	}
	
	public void init (string gameId, bool testModeEnabled, bool debugModeEnabled) {
		if (!_initRun) {
			_initRun = true;
			_gameObjectName = gameObject.name;
			UnityAdsExternal.init(gameId, testModeEnabled, debugModeEnabled, _gameObjectName);
		}
	}
	
	
	/* Static Methods */
	
	public static UnityAds getCurrentInstance () {
		GameObject unityAds = GameObject.FindWithTag("UnityAds");
		UnityAds unityAdsMobile = unityAds.GetComponent<UnityAds>();
		return unityAdsMobile;
	}
	
	
	public static bool getTestButtonVisibility () {
		return getCurrentInstance().showTestButton;
	}
	
	public static bool isSupported () {
		return UnityAdsExternal.isSupported();
	}
	
	public static string getSDKVersion () {
		return UnityAdsExternal.getSDKVersion();
	}
	
	public static bool canShowAds () {
		if (_campaignsAvailable)
			return UnityAdsExternal.canShowAds();
		
		return false;
	}
	
	public static bool canShow () {
		if (_campaignsAvailable)
			return UnityAdsExternal.canShow();
		
		return false;
	}
	
	public static void setGamerSID (string sid) {
		_gamerSID = sid;
	}
	
	public static void stopAll () {
		UnityAdsExternal.stopAll();
	}
	
	public static bool hasMultipleRewardItems () {
		if (_campaignsAvailable)
			return UnityAdsExternal.hasMultipleRewardItems();
		
		return false;
	}
	
	public static List<string> getRewardItemKeys () {
		List<string> retList = new List<string>();
		
		if (_campaignsAvailable) {
			string keys = UnityAdsExternal.getRewardItemKeys();
			retList = new List<string>(keys.Split(';'));
		}
		
		return retList;
	}
	
	public static string getDefaultRewardItemKey () {
		if (_campaignsAvailable) {
			return UnityAdsExternal.getDefaultRewardItemKey();
		}
		
		return "";
	}
	
	public static string getCurrentRewardItemKey () {
		if (_campaignsAvailable) {
			return UnityAdsExternal.getCurrentRewardItemKey();
		}
		
		return "";
	}
	
	public static bool setRewardItemKey (string rewardItemKey) {
		if (_campaignsAvailable) {
			return UnityAdsExternal.setRewardItemKey(rewardItemKey);
		}
		
		return false;
	}
	
	public static void setDefaultRewardItemAsRewardItem () {
		if (_campaignsAvailable) {
			UnityAdsExternal.setDefaultRewardItemAsRewardItem();
		}
	}
	
	public static Dictionary<string, string> getRewardItemDetailsWithKey (string rewardItemKey) {
		Dictionary<string, string> retDict = new Dictionary<string, string>();
		if (_campaignsAvailable) {
			retDict = UnityAdsExternal.getRewardItemDetailsWithKey(rewardItemKey);
			return retDict;
		}
		
		return retDict;
	}
	
	public static bool show () {
		if (!_adsOpen && _campaignsAvailable) {
			UnityAds instance = getCurrentInstance();
			
			bool animated = false;
			bool noOfferscreen = false;
			string gamerSID = _gamerSID;
			
			if (instance != null) {
				animated = instance.openAnimated;
				noOfferscreen = instance.noOfferscreen;
			}
			
			if (UnityAdsExternal.show(animated, noOfferscreen, gamerSID)) {				
				if (_adsOpenDelegate != null)
					_adsOpenDelegate();
				
				_adsOpen = true;
				_savedTimeScale = Time.timeScale;
				AudioListener.pause = true;
				Time.timeScale = 0;
			}
		}
		
		return false;
	}
	
	public static void hide () {
		if (_adsOpen) {
			UnityAdsExternal.hide();
		}
	}
	
	
	/* Events */
	
	public void onHide () {
		_adsOpen = false;
		AudioListener.pause = false;
		Time.timeScale = _savedTimeScale;
		
		if (_adsCloseDelegate != null)
			_adsCloseDelegate();
		
		UnityAdsExternal.Log("onHide");
	}
	
	public void onShow () {
		UnityAdsExternal.Log("onShow");
	}
	
	public void onVideoStarted () {
		if (_videoStartedDelegate != null)
			_videoStartedDelegate();
		
		UnityAdsExternal.Log("onVideoStarted");
	}
	
	public void onVideoCompleted (string rewardItemKey) {
		if (_videoCompletedDelegate != null)
			_videoCompletedDelegate(rewardItemKey);
		
		UnityAdsExternal.Log("onVideoCompleted: " + rewardItemKey);
	}
	
	public void onFetchCompleted () {
		_campaignsAvailable = true;
		if (_campaignsAvailableDelegate != null)
			_campaignsAvailableDelegate();
			
		UnityAdsExternal.Log("onFetchCompleted");
	}

	public void onFetchFailed () {
		_campaignsAvailable = false;
		if (_campaignsFetchFailedDelegate != null)
			_campaignsFetchFailedDelegate();
		
		UnityAdsExternal.Log("onFetchFailed");
	}
}
