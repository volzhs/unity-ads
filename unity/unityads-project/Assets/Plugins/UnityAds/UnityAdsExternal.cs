using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public static class UnityAdsExternal {

	private static string _logTag = "UnityAds";
	
	public static void Log (string message) {
		UnityAds unityAdsMobileInstance = UnityAds.SharedInstance;
		
		if(unityAdsMobileInstance) {
			if(unityAdsMobileInstance.debugModeEnabled && Debug.isDebugBuild)
				Debug.Log(_logTag + "/" + message);
		}
	}
	
#if UNITY_IPHONE
	[DllImport ("__Internal")]
	public static extern void init (string gameId, bool testModeEnabled, bool debugModeEnabled, string gameObjectName);
	
	[DllImport ("__Internal")]
	public static extern bool show (string zoneId, string rewardItemKey, string options);
	
	[DllImport ("__Internal")]
	public static extern void hide ();
	
	[DllImport ("__Internal")]
	public static extern bool isSupported ();
	
	[DllImport ("__Internal")]
	public static extern string getSDKVersion ();

	[DllImport ("__Internal")]
	public static extern bool canShowAds ();

	[DllImport ("__Internal")]
	public static extern bool canShow ();
	
	[DllImport ("__Internal")]
	public static extern bool hasMultipleRewardItems ();
	
	[DllImport ("__Internal")]
	public static extern string getRewardItemKeys ();

	[DllImport ("__Internal")]
	public static extern string getDefaultRewardItemKey ();
	
	[DllImport ("__Internal")]
	public static extern string getCurrentRewardItemKey ();

	[DllImport ("__Internal")]
	public static extern bool setRewardItemKey (string rewardItemKey);
	
	[DllImport ("__Internal")]
	public static extern void setDefaultRewardItemAsRewardItem ();

	[DllImport ("__Internal")]
	public static extern string getRewardItemDetailsWithKey (string rewardItemKey);

	[DllImport ("__Internal")]
	public static extern string getRewardItemDetailsKeys ();

#elif UNITY_ANDROID
	private static AndroidJavaObject unityAds;
	private static AndroidJavaObject unityAdsUnity;
	private static AndroidJavaObject currentActivity;
	
	public static void init (string gameId, bool testModeEnabled, bool debugModeEnabled, string gameObjectName) {		
		Log("UnityAndroid: init(), gameId=" + gameId + ", testModeEnabled=" + testModeEnabled + ", gameObjectName=" + gameObjectName + ", debugModeEnabled=" + debugModeEnabled);
		currentActivity = (new AndroidJavaClass("com.unity3d.player.UnityPlayer")).GetStatic<AndroidJavaObject>("currentActivity");
		unityAdsUnity = new AndroidJavaObject("com.unity3d.ads.android.unity3d.UnityAdsUnityWrapper");
		unityAdsUnity.Call("init", gameId, currentActivity, testModeEnabled, debugModeEnabled, gameObjectName);
	}
	
	public static bool show (string zoneId, string rewardItemKey, string options) {
		Log ("UnityAndroid: show()");
		return unityAdsUnity.Call<bool>("show", zoneId, rewardItemKey, options);
	}
	
	public static void hide () {
		Log ("UnityAndroid: hide()");
		unityAdsUnity.Call("hide");
	}
	
	public static bool isSupported () {
		Log ("UnityAndroid: isSupported()");
		return unityAdsUnity.Call<bool>("isSupported");
	}
	
	public static string getSDKVersion () {
		Log ("UnityAndroid: getSDKVersion()");
		return unityAdsUnity.Call<string>("getSDKVersion");
	}
	
	public static bool canShowAds () {
		Log ("UnityAndroid: canShowAds()");
		return unityAdsUnity.Call<bool>("canShowAds");
	}
	
	public static bool canShow () {
		Log ("UnityAndroid: canShow()");
		return unityAdsUnity.Call<bool>("canShow");
	}
	
	public static bool hasMultipleRewardItems () {
		Log ("UnityAndroid: hasMultipleRewardItems()");
		return unityAdsUnity.Call<bool>("hasMultipleRewardItems");
	}
	
	public static string getRewardItemKeys () {
		Log ("UnityAndroid: getRewardItemKeys()");
		return unityAdsUnity.Call<string>("getRewardItemKeys");
	}

	public static string getDefaultRewardItemKey () {
		Log ("UnityAndroid: getDefaultRewardItemKey()");
		return unityAdsUnity.Call<string>("getDefaultRewardItemKey");
	}
	
	public static string getCurrentRewardItemKey () {
		Log ("UnityAndroid: getCurrentRewardItemKey()");
		return unityAdsUnity.Call<string>("getCurrentRewardItemKey");
	}

	public static bool setRewardItemKey (string rewardItemKey) {
		Log ("UnityAndroid: setRewardItemKey() rewardItemKey=" + rewardItemKey);
		return unityAdsUnity.Call<bool>("setRewardItemKey", rewardItemKey);
	}
	
	public static void setDefaultRewardItemAsRewardItem () {
		Log ("UnityAndroid: setDefaultRewardItemAsRewardItem()");
		unityAdsUnity.Call("setDefaultRewardItemAsRewardItem");
	}
	
	public static string getRewardItemDetailsWithKey (string rewardItemKey) {
		Log ("UnityAndroid: getRewardItemDetailsWithKey() rewardItemKey=" + rewardItemKey);
		return unityAdsUnity.Call<string>("getRewardItemDetailsWithKey", rewardItemKey);
	}
	
	public static string getRewardItemDetailsKeys () {
		Log ("UnityAndroid: getRewardItemDetailsKeys()");
		return unityAdsUnity.Call<string>("getRewardItemDetailsKeys");
	}
	
#elif UNITY_EDITOR
	public static void init (string gameId, bool testModeEnabled, bool debugModeEnabled, string gameObjectName) {
		Log ("UnityEditor: init(), gameId=" + gameId + ", testModeEnabled=" + testModeEnabled + ", gameObjectName=" + gameObjectName + ", debugModeEnabled=" + debugModeEnabled);
	}
	
	public static bool show (string zoneId, string rewardItemKey, string options) {
		Log ("UnityEditor: show()");
		return false;
	}
	
	public static void hide () {
		Log ("UnityEditor: hide()");
	}
	
	public static bool isSupported () {
		Log ("UnityEditor: isSupported()");
		return false;
	}
	
	public static string getSDKVersion () {
		Log ("UnityEditor: getSDKVersion()");
		return "EDITOR";
	}
	
	public static bool canShowAds () {
		Log ("UnityEditor: canShowAds()");
		return false;
	}
	
	public static bool canShow () {
		Log ("UnityEditor: canShow()");
		return false;
	}
	
	public static bool hasMultipleRewardItems () {
		Log ("UnityEditor: hasMultipleRewardItems()");
		return false;
	}
	
	public static string getRewardItemKeys () {
		Log ("UnityEditor: getRewardItemKeys()");
		return "";
	}
	
	public static string getDefaultRewardItemKey () {
		Log ("UnityEditor: getDefaultRewardItemKey()");
		return "";
	}
	
	public static string getCurrentRewardItemKey () {
		Log ("UnityEditor: getCurrentRewardItemKey()");
		return "";
	}
	
	public static bool setRewardItemKey (string rewardItemKey) {
		Log ("UnityEditor: setRewardItemKey() rewardItemKey=" + rewardItemKey);
		return false;
	}
	
	public static void setDefaultRewardItemAsRewardItem () {
		Log ("UnityEditor: setDefaultRewardItemAsRewardItem()");
	}
	
	public static string getRewardItemDetailsWithKey (string rewardItemKey) {
		Log ("UnityEditor: getRewardItemDetailsWithKey() rewardItemKey=" + rewardItemKey);
		return "";
	}
	
	public static string getRewardItemDetailsKeys () {
		return "name;picture";
	}

#else
	public static void init (string gameId, bool testModeEnabled, bool debugModeEnabled, string gameObjectName) {}
	public static bool show (string zoneId, string rewardItemKey, string options) { return false; }
	public static void hide () {}	
	public static bool isSupported () { return false; }
	public static string getSDKVersion () { return null; }	
	public static bool canShowAds () { return false; }
	public static bool canShow () { return false; }
	public static bool hasMultipleRewardItems () { return false; }
	public static string getRewardItemKeys () { return null; }	
	public static string getDefaultRewardItemKey () { return null; }	
	public static string getCurrentRewardItemKey () { return null; }	
	public static bool setRewardItemKey (string rewardItemKey) { return false; }
	public static void setDefaultRewardItemAsRewardItem () {}
	public static string getRewardItemDetailsWithKey (string rewardItemKey) { return null; }
	public static string getRewardItemDetailsKeys () { return null; }

#endif

}
