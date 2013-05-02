using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAds : MonoBehaviour {

	public string gameId = "";
	public bool debugModeEnabled = false;
	public bool testModeEnabled = false;
	public bool openAnimated = false;
	public bool noOfferscreen = false;
	
	private static UnityAds sharedInstance;
	private static bool _campaignsAvailable = false;
	private static bool _adsOpen = false;
	private static float _savedTimeScale = 1f;
	private static float _savedAudioVolume = 1f;
	private static string _gamerSID = "";
	
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
	
	public static UnityAds SharedInstance {
		get {
			if(!sharedInstance) {
				sharedInstance = (UnityAds) FindObjectOfType(typeof(UnityAds));

				#if (UNITY_IPHONE || UNITY_ANDROID) && !UNITY_EDITOR
				UnityAdsExternal.init(sharedInstance.gameId, sharedInstance.testModeEnabled, sharedInstance.debugModeEnabled && Debug.isDebugBuild, sharedInstance.gameObject.name);
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
		_campaignsAvailableDelegate = null;
		_campaignsFetchFailedDelegate = null;
		_adsOpenDelegate = null;
		_adsCloseDelegate = null;
		_videoCompletedDelegate = null;
		_videoStartedDelegate = null;
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
		if (!_adsOpen && _campaignsAvailable) {
			UnityAds instance = SharedInstance;
			
			if(instance) {
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
					_savedAudioVolume = AudioListener.volume;
					AudioListener.pause = true;
					AudioListener.volume = 0;
					Time.timeScale = 0;
				}
			}
		}
		
		return false;
	}
	
	public static void hide () {
		if (_adsOpen) {
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

	/* Events */
	
	public void onHide () {
		_adsOpen = false;
		AudioListener.pause = false;
		AudioListener.volume = _savedAudioVolume;
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
