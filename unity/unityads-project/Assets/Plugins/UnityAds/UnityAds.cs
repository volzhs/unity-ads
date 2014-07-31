using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAds : MonoBehaviour {

	public string gameIdForAndroid = "";
	public string gameIdForIOS = "";
	public bool debugModeEnabled = false;
	public bool testModeEnabled = false;
	
	private static UnityAds sharedInstance;
	private static bool _campaignsAvailable = false;
	private static bool _adsShow = false;
	private static float _savedTimeScale = 1f;
	private static float _savedAudioVolume = 1f;
	
	private static string _rewardItemNameKey = "";
	private static string _rewardItemPictureKey = "";
	
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

	public delegate void UnityAdsShow();
	private static UnityAdsShow _adsShowDelegate;
	public static void setShowDelegate (UnityAdsShow action) {
		_adsShowDelegate = action;
	}
	
	public delegate void UnityAdsHide();
	private static UnityAdsHide _adsHideDelegate;
	public static void setHideDelegate (UnityAdsHide action) {
		_adsHideDelegate = action;
	}

	public delegate void UnityAdsVideoCompleted(string rewardItemKey, bool skipped);
	private static UnityAdsVideoCompleted _videoCompletedDelegate;
	public static void setVideoCompletedDelegate (UnityAdsVideoCompleted action) {
		_videoCompletedDelegate = action;
	}
	
	public delegate void UnityAdsVideoStarted();
	private static UnityAdsVideoStarted _videoStartedDelegate;
	public static void setVideoStartedDelegate (UnityAdsVideoStarted action) {
		_videoStartedDelegate = action;
	}
	
	public static UnityAds SharedInstance {
		get {
			if(!sharedInstance) {
				sharedInstance = (UnityAds) FindObjectOfType(typeof(UnityAds));

				#if UNITY_IPHONE && !UNITY_EDITOR
				UnityAdsExternal.init(sharedInstance.gameIdForIOS, sharedInstance.testModeEnabled, sharedInstance.debugModeEnabled && Debug.isDebugBuild, sharedInstance.gameObject.name.Replace("(Clone)", ""));
				#elif UNITY_ANDROID && !UNITY_EDITOR
				UnityAdsExternal.init(sharedInstance.gameIdForAndroid, sharedInstance.testModeEnabled, sharedInstance.debugModeEnabled && Debug.isDebugBuild, sharedInstance.gameObject.name.Replace("(Clone)", ""));
				#endif
			}

			return sharedInstance;
		}
	}
	
	public void Awake () {
		if(gameObject == SharedInstance.gameObject) {
			DontDestroyOnLoad(gameObject);
		}
		else {
			Destroy (gameObject);
		}
	}
	
	public void OnDestroy () {
		if(gameObject == SharedInstance.gameObject) {
			_campaignsAvailableDelegate = null;
			_campaignsFetchFailedDelegate = null;
			_adsShowDelegate = null;
			_adsHideDelegate = null;
			_videoCompletedDelegate = null;
			_videoStartedDelegate = null;
		}
	}

	/* Static Methods */
	
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
	
	public static string getRewardItemNameKey () {
		if (_rewardItemNameKey == null || _rewardItemNameKey.Length == 0) {
			fillRewardItemKeyData();
		}
		
		return _rewardItemNameKey;
	}
	
	public static string getRewardItemPictureKey () {
		if (_rewardItemPictureKey == null || _rewardItemPictureKey.Length == 0) {
			fillRewardItemKeyData();
		}
		
		return _rewardItemPictureKey;
	}
	
	public static Dictionary<string, string> getRewardItemDetailsWithKey (string rewardItemKey) {
		Dictionary<string, string> retDict = new Dictionary<string, string>();
		string rewardItemDataString = "";
		
		if (_campaignsAvailable) {
			rewardItemDataString = UnityAdsExternal.getRewardItemDetailsWithKey(rewardItemKey);
			
			if (rewardItemDataString != null) {
				List<string> splittedData = new List<string>(rewardItemDataString.Split(';'));
				UnityAdsExternal.Log("UnityAndroid: getRewardItemDetailsWithKey() rewardItemDataString=" + rewardItemDataString);
				
				if (splittedData.Count == 2) {
					retDict.Add(getRewardItemNameKey(), splittedData.ToArray().GetValue(0).ToString());
					retDict.Add(getRewardItemPictureKey(), splittedData.ToArray().GetValue(1).ToString());
				}
			}
		}
		
		return retDict;
	}

	public static bool show () {
		return show (null, "", null);
	}

	public static bool show (string zoneId) {
		return show (zoneId, "", null);	
	}
	
	public static bool show (string zoneId, string rewardItemKey) {
		return show (zoneId, rewardItemKey, null);	
	}
	
	public static bool show (string zoneId, string rewardItemKey, Dictionary<string, string> options) {
		if (!_adsShow && _campaignsAvailable) {			
			if(SharedInstance) {							
				string optionsString = parseOptionsDictionary(options);
				
				if (UnityAdsExternal.show(zoneId, rewardItemKey, optionsString)) {				
					if (_adsShowDelegate != null)
						_adsShowDelegate();
					
					_adsShow = true;
					_savedTimeScale = Time.timeScale;
					_savedAudioVolume = AudioListener.volume;
					AudioListener.pause = true;
					AudioListener.volume = 0;
					Time.timeScale = 0;
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static void hide () {
		if (_adsShow) {
			UnityAdsExternal.hide();
		}
	}

	private static void fillRewardItemKeyData () {
		string keyData = UnityAdsExternal.getRewardItemDetailsKeys();
		
		if (keyData != null && keyData.Length > 2) {
			List<string> splittedKeyData = new List<string>(keyData.Split(';'));
			_rewardItemNameKey = splittedKeyData.ToArray().GetValue(0).ToString();
			_rewardItemPictureKey = splittedKeyData.ToArray().GetValue(1).ToString();
		}
	}
	
	private static string parseOptionsDictionary(Dictionary<string, string> options) {
		string optionsString = "";
		if(options != null) {
			bool added = false;
			if(options.ContainsKey("noOfferScreen")) {
				optionsString += (added ? "," : "") + "noOfferScreen:" + options["noOfferScreen"];
				added = true;
			}
			if(options.ContainsKey("openAnimated")) {
				optionsString += (added ? "," : "") + "openAnimated:" + options["openAnimated"];
				added = true;
			}
			if(options.ContainsKey("sid")) {
				optionsString += (added ? "," : "") + "sid:" + options["sid"];
				added = true;
			}
			if(options.ContainsKey("muteVideoSounds")) {
				optionsString += (added ? "," : "") + "muteVideoSounds:" + options["muteVideoSounds"];
				added = true;
			}
			if(options.ContainsKey("useDeviceOrientationForVideo")) {
				optionsString += (added ? "," : "") + "useDeviceOrientationForVideo:" + options["useDeviceOrientationForVideo"];
				added = true;
			}
		}
		return optionsString;
	}

	/* Events */
	
	public void onHide () {
		_adsShow = false;
		AudioListener.pause = false;
		AudioListener.volume = _savedAudioVolume;
		Time.timeScale = _savedTimeScale;
		
		if (_adsHideDelegate != null)
			_adsHideDelegate();
		
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
	
	public void onVideoCompleted (string parameters) {
		if (parameters != null) {
			List<string> splittedParameters = new List<string>(parameters.Split(';'));
			string rewardItemKey = splittedParameters.ToArray().GetValue(0).ToString();
			bool skipped = splittedParameters.ToArray().GetValue(1).ToString() == "true" ? true : false;
			
			if (_videoCompletedDelegate != null)
				_videoCompletedDelegate(rewardItemKey, skipped);
		
			UnityAdsExternal.Log("onVideoCompleted: " + rewardItemKey + " - " + skipped);
		}
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
