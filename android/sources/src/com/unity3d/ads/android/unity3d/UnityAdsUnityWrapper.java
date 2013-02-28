package com.unity3d.ads.android.unity3d;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.app.Activity;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.IUnityAdsListener;

public class UnityAdsUnityWrapper implements IUnityAdsListener {
	private Activity _startupActivity = null;
	private String _gameObject = null;
	private String _gameId = null;
	private Method _sendMessageMethod = null;
	private boolean _testMode = false;
	private boolean _debugMode = false;
	private static UnityAds _unityAds = null;
	private static Boolean _constructed = false;
	private static Boolean _initialized = false;
	
	public UnityAdsUnityWrapper () {
		if (!_constructed) {
			_constructed = true;
	    	UnityAdsUtils.Log("UnityAdsUnityWrapper Constructor", this);
	        try {
	                Class<?> unityClass = Class.forName("com.unity3d.player.UnityPlayer");
	                Class<?> paramTypes[] = new Class[3];
	                paramTypes[0] = String.class;
	                paramTypes[1] = String.class;
	                paramTypes[2] = String.class;
	                _sendMessageMethod = unityClass.getDeclaredMethod("UnitySendMessage", paramTypes);
	        } 
	        catch (Exception e) {
	        	UnityAdsUtils.Log("Error getting class or method of com.unity3d.player.UnityPlayer, method UnitySendMessage(string, string, string). " + e.getLocalizedMessage(), this);
	        }
		}
	}
	
	
	// Public methods

	public boolean isSupported () {
		return UnityAds.isSupported();
	}
	
	public String getSDKVersion () {
		return UnityAds.getSDKVersion();
	}
	
	public void init (final String gameId, final Activity activity, boolean testMode, boolean debugMode, String gameObject) {
		if (!_initialized) {
			_initialized = true;
			_gameId = gameId;
			_gameObject = gameObject;
			_testMode = testMode;
			_debugMode = debugMode;
			
			if (_startupActivity == null)
				_startupActivity = activity;
			
			final UnityAdsUnityWrapper listener = this;
			
			try {
				_startupActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						UnityAds.setTestMode(_testMode);
						UnityAds.setDebugMode(_debugMode);
						_unityAds = new UnityAds(_startupActivity, _gameId, listener);
					}
				});
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Error occured while initializing Unity Ads", this);
			}
		}
	}
	
	public void show (boolean openAnimated, boolean noOfferscreen, final String gamerSID) {
		if (_unityAds != null && _unityAds.canShowAds() && _unityAds.canShow()) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY, openAnimated);
			params.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, noOfferscreen);
			
			if (gamerSID != null)
				params.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, gamerSID);
			
			UnityAdsUtils.Log("Opening with: openAnimated=" + openAnimated + ", noOfferscreen=" + noOfferscreen + ", gamerSID=" + gamerSID, this);
			_unityAds.show(params);
		}
	}
	
	public void hide () {
		if (_unityAds == null) return;
		_unityAds.hide();
	}
	
	public boolean canShowAds () {
		if (_unityAds == null) return false;
		return _unityAds.canShowAds();
	}
	
	public boolean canShow () {
		if (_unityAds == null) return false;
		return _unityAds.canShow();
	}
	
	public void stopAll () {
		if (_unityAds == null) return;
		_unityAds.stopAll();
	}
	
	public boolean hasMultipleRewardItems () {
		if (_unityAds == null) return false;
		return _unityAds.hasMultipleRewardItems();
	}
	
	public String getRewardItemKeys () {
		if (_unityAds == null || _unityAds.getRewardItemKeys() == null) return null;
		if (_unityAds.getRewardItemKeys().size() > 0) {
			String keys = "";
			for (String key : _unityAds.getRewardItemKeys()) {
				if (_unityAds.getRewardItemKeys().indexOf(key) > 0) {
					keys += ";";
				}
				keys += key;
			}
			
			return keys;
		}
		
		return null;
	}
	
	public String getDefaultRewardItemKey () {
		if (_unityAds == null) return "";
		return _unityAds.getDefaultRewardItemKey();
	}
	
	public String getCurrentRewardItemKey () {
		if (_unityAds == null) return "";
		return _unityAds.getCurrentRewardItemKey();
	}
	
	public boolean setRewardItemKey (String rewardItemKey) {
		if (_unityAds == null || rewardItemKey == null) return false;
		return _unityAds.setRewardItemKey(rewardItemKey);
	}
	
	public void setDefaultRewardItemAsRewardItem () {
		if (_unityAds == null) return;
		_unityAds.setDefaultRewardItemAsRewardItem();
	}
	
	public String getRewardItemDetailsWithKey (String rewardItemKey) {
		String retString = "";
		
		if (_unityAds == null) return "";
		if (_unityAds.getRewardItemDetailsWithKey(rewardItemKey) != null) {
			UnityAdsUtils.Log("Fetching reward data", this);
			HashMap<String, String> rewardMap = (HashMap)_unityAds.getRewardItemDetailsWithKey(rewardItemKey);
			
			if (rewardMap != null) {
				retString = rewardMap.get(UnityAds.UNITY_ADS_REWARDITEM_NAME_KEY);
				retString += ";" + rewardMap.get(UnityAds.UNITY_ADS_REWARDITEM_PICTURE_KEY);
				return retString;
			}
			else {
				UnityAdsUtils.Log("Problems getting reward item details", this);
			}
		}
		else {
			UnityAdsUtils.Log("Could not find reward item details", this);
		}
		return "";
	}
	
	
	// IUnityAdsListener
	
	@Override
	public void onHide() {
		sendMessageToUnity3D("onHide", null);
	}

	@Override
	public void onShow() {
		sendMessageToUnity3D("onShow", null);
	}

	@Override
	public void onVideoStarted() {
		sendMessageToUnity3D("onVideoStarted", null);
	}

	@Override
	public void onVideoCompleted(String rewardItemKey) {
		sendMessageToUnity3D("onVideoCompleted", rewardItemKey);
	}

	@Override
	public void onFetchCompleted() {
		sendMessageToUnity3D("onFetchCompleted", null);
	}

	@Override
	public void onFetchFailed() {
		sendMessageToUnity3D("onFetchFailed", null);
	}
	
	
    public void sendMessageToUnity3D(String methodName, String parameter) {
        // Unity Development build crashes if parameter is NULL
        if (parameter == null)
                parameter = "";

        if (_sendMessageMethod == null) {
        	UnityAdsUtils.Log("Cannot send message to Unity3D. Method is null", this);
        	return;
        }
        try {
        	UnityAdsUtils.Log("Sending message (" + methodName + ", " + parameter + ") to Unity3D", this);
        	_sendMessageMethod.invoke(null, _gameObject, methodName, parameter);
        } 
        catch (Exception e) {
        	UnityAdsUtils.Log("Can't invoke UnitySendMessage method. Error = "  + e.getLocalizedMessage(), this);
        }
    }
}
