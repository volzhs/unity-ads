package com.unity3d.ads.android.unity3d;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.app.Activity;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.zone.UnityAdsZoneManager;

public class UnityAdsUnityWrapper implements IUnityAdsListener {
	private Activity _startupActivity = null;
	private String _gameObject = null;
	private String _gameId = null;
	private Method _sendMessageMethod = null;
	private boolean _testMode = false;
	private static Boolean _constructed = false;
	private static Boolean _initialized = false;

	public UnityAdsUnityWrapper () {
		if (!_constructed) {
			_constructed = true;
	        try {
	                Class<?> unityClass = Class.forName("com.unity3d.player.UnityPlayer");
	                Class<?> paramTypes[] = new Class[3];
	                paramTypes[0] = String.class;
	                paramTypes[1] = String.class;
	                paramTypes[2] = String.class;
	                _sendMessageMethod = unityClass.getDeclaredMethod("UnitySendMessage", paramTypes);
	        }
	        catch (Exception e) {
	        	UnityAdsDeviceLog.error("Error getting class or method of com.unity3d.player.UnityPlayer, method UnitySendMessage(string, string, string). " + e.getLocalizedMessage());
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

	public void init (final String gameId, final Activity activity, boolean testMode, final int logLevel, String gameObject, final String unityVersion) {
		if (!_initialized) {
			_initialized = true;
			_gameId = gameId;
			_gameObject = gameObject;
			_testMode = testMode;

			if (_startupActivity == null)
				_startupActivity = activity;

			final UnityAdsUnityWrapper listener = this;

			try {
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						UnityAdsDeviceLog.setLogLevel(logLevel);
						UnityAds.setTestMode(_testMode);
            if(unityVersion.length() > 0) {
              UnityAdsProperties.UNITY_VERSION = unityVersion;
            }
						UnityAds.init(_startupActivity, _gameId, listener);
					}
				});
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Error occured while initializing Unity Ads");
			}
		}
	}

	public boolean show (final String zoneId, final String rewardItemKey, final String optionsString) {
		if(canShowZone(zoneId)) {
			HashMap<String, Object> options = null;

			if(optionsString.length() > 0) {
				options = new HashMap<String, Object>();
				for(String rawOptionPair : optionsString.split(",")) {
					String[] optionPair = rawOptionPair.split(":");
					options.put(optionPair[0], optionPair[1]);
				}
			}

			if(rewardItemKey.length() > 0) {
				if(zoneId != null && zoneId.length() > 0) {
					UnityAds.setZone(zoneId, rewardItemKey);
				}
			} else {
				if(zoneId != null && zoneId.length() > 0) {
					UnityAds.setZone(zoneId);
				}
			}

			return UnityAds.show(options);
		}

		return false;
	}

	public void hide () {
		UnityAds.hide();
	}

	public boolean canShow () {
		return UnityAds.canShow();
	}

	public boolean canShowZone(String zone) {
		if(zone != null && zone.length() > 0) {
			UnityAdsZoneManager zoneManager = UnityAdsWebData.getZoneManager();
			if(zoneManager != null) {
				if(zoneManager.getZone(zone) != null) {
					return UnityAds.canShow();
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		return UnityAds.canShow();
	}

	public boolean hasMultipleRewardItems () {
		return UnityAds.hasMultipleRewardItems();
	}

	public String getRewardItemKeys () {
		if (UnityAds.getRewardItemKeys() == null) return null;
		if (UnityAds.getRewardItemKeys().size() > 0) {
			String keys = "";
			for (String key : UnityAds.getRewardItemKeys()) {
				if (UnityAds.getRewardItemKeys().indexOf(key) > 0) {
					keys += ";";
				}
				keys += key;
			}

			return keys;
		}

		return null;
	}

	public String getDefaultRewardItemKey () {
		return UnityAds.getDefaultRewardItemKey();
	}

	public String getCurrentRewardItemKey () {
		return UnityAds.getCurrentRewardItemKey();
	}

	public boolean setRewardItemKey (String rewardItemKey) {
		return UnityAds.setRewardItemKey(rewardItemKey);
	}

	public void setDefaultRewardItemAsRewardItem () {
		UnityAds.setDefaultRewardItemAsRewardItem();
	}

	public String getRewardItemDetailsWithKey (String rewardItemKey) {
		String retString = "";

		if (UnityAds.getRewardItemDetailsWithKey(rewardItemKey) != null) {
			UnityAdsDeviceLog.debug("Fetching reward data");

			@SuppressWarnings({ "unchecked", "rawtypes" })
			HashMap<String, String> rewardMap = (HashMap)UnityAds.getRewardItemDetailsWithKey(rewardItemKey);

			if (rewardMap != null) {
				retString = rewardMap.get(UnityAds.UNITY_ADS_REWARDITEM_NAME_KEY);
				retString += ";" + rewardMap.get(UnityAds.UNITY_ADS_REWARDITEM_PICTURE_KEY);
				return retString;
			}
			else {
				UnityAdsDeviceLog.debug("Problems getting reward item details");
			}
		}
		else {
			UnityAdsDeviceLog.debug("Could not find reward item details");
		}
		return "";
	}

    public String getRewardItemDetailsKeys () {
    	return String.format("%s;%s", UnityAds.UNITY_ADS_REWARDITEM_NAME_KEY, UnityAds.UNITY_ADS_REWARDITEM_PICTURE_KEY);
    }

	public void setLogLevel(int logLevel) {
		UnityAdsDeviceLog.setLogLevel(logLevel);
	}

	public void enableUnityDeveloperInternalTestMode() {
		UnityAds.enableUnityDeveloperInternalTestMode();
	}

	public void setCampaignDataURL(String campaignDataURL) {
		UnityAds.setCampaignDataURL(campaignDataURL);
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
	public void onVideoCompleted(String rewardItemKey, boolean skipped) {
		sendMessageToUnity3D("onVideoCompleted", rewardItemKey + ";" + (skipped ? "true" : "false"));
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
        	UnityAdsDeviceLog.error("Cannot send message to Unity3D. Method is null");
        	return;
        }
        try {
        	UnityAdsDeviceLog.debug("Sending message (" + methodName + ", " + parameter + ") to Unity3D");
        	_sendMessageMethod.invoke(null, _gameObject, methodName, parameter);
        }
        catch (Exception e) {
        	UnityAdsDeviceLog.error("Can't invoke UnitySendMessage method. Error = "  + e.getLocalizedMessage());
        }
    }

}
