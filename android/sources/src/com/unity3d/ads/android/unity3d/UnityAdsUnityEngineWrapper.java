package com.unity3d.ads.android.unity3d;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.DisplayMetrics;

import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.data.UnityAdsDevice;

import java.lang.reflect.Field;
import java.util.HashMap;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsUnityEngineWrapper implements IUnityAdsListener {
  private Activity _startupActivity = null;
  private String _gameId = null;
  private boolean _testMode = false;
  private static Boolean _initialized = false;

  public UnityAdsUnityEngineWrapper () {
    try {
      Class unityPlayer = Class.forName("com.unity3d.player.UnityPlayer");
      Field currentActivityField = unityPlayer.getField("currentActivity");
      Activity currentActivity = (Activity)currentActivityField.get(null);
      if(currentActivity != null) {
        _startupActivity = currentActivity;
      }
    } catch(Exception e) {}
  }

  // Public methods

  public boolean isSupported () {
    return UnityAds.isSupported();
  }

  public String getSDKVersion () {
    return UnityAds.getSDKVersion();
  }

  public void init (final String gameId, final Activity activity, boolean testMode, final int logLevel) {
    if (!_initialized) {
      _initialized = true;
      _gameId = gameId;
      _testMode = testMode;

      if (_startupActivity == null)
        _startupActivity = activity;

      final UnityAdsUnityEngineWrapper listener = this;

      try {
        UnityAdsUtils.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            UnityAdsDeviceLog.setLogLevel(logLevel);
            UnityAds.setTestMode(_testMode);
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
    if (UnityAds.canShowAds()) {
      HashMap<String, Object> options = null;

      if(optionsString.length() > 0) {
        options = new HashMap<String, Object>();
        for(String rawOptionPair : optionsString.split(",")) {
          String[] optionPair = rawOptionPair.split(":");
          options.put(optionPair[0], optionPair[1]);
        }
      }

      if(rewardItemKey.length() > 0) {
        UnityAds.setZone(zoneId, rewardItemKey);
      } else {
        UnityAds.setZone(zoneId);
      }

      return UnityAds.show(options);
    }

    return false;
  }

  public void hide () {
    UnityAds.hide();
  }

  public boolean canShowAds (String network) {
    return UnityAds.canShowAds();
  }

  public boolean canShow () {
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

  // IUnityAdsListener

  private static native void UnityAdsOnHide();
  @Override
  public void onHide() {
    UnityAdsOnHide();
  }

  private static native void UnityAdsOnShow();
  @Override
  public void onShow() {
    UnityAdsOnShow();
  }

  private static native void UnityAdsOnVideoStarted();
  @Override
  public void onVideoStarted() {
    UnityAdsOnVideoStarted();
  }

  private static native void UnityAdsOnVideoCompleted(String rewardItemKey, int skipped);
  @Override
  public void onVideoCompleted(String rewardItemKey, boolean skipped) {
    if(rewardItemKey == null || rewardItemKey.isEmpty()) {
      rewardItemKey = "null";
    }
    UnityAdsOnVideoCompleted(rewardItemKey, skipped ? 1 : 0);
  }

  private static native void UnityAdsOnFetchCompleted();
  @Override
  public void onFetchCompleted() {
    UnityAdsOnFetchCompleted();
  }

  private static native void UnityAdsOnFetchFailed();
  @Override
  public void onFetchFailed() {
    UnityAdsOnFetchFailed();
  }

  // Device Info Wrapper

  public String getPlatformName() {
    return "android";
  }

  public String getAdvertisingIdentifier() {
    return UnityAdsDevice.getAdvertisingTrackingId();
  }

  public boolean getNoTrack() {
    return UnityAdsDevice.isLimitAdTrackingEnabled();
  }

  public String getVendor() {
    return Build.MANUFACTURER;
  }

  public String getModel() {
    return Build.MODEL;
  }

  public String getOSVersion() {
    return Build.VERSION.RELEASE;
  }

  public String getDeviceId() {
    try {
      String androidId = Secure.getString(_startupActivity.getContentResolver(), Secure.ANDROID_ID);
      return androidId;
    } catch (Exception e) {
      return null;
    }
  }

  public String getBundleId() {
    Context context = _startupActivity.getApplicationContext();
    if (context != null) {
      return context.getPackageName();
    }
    return null;
  }

  public String getScreenSize() {
    if (_startupActivity== null) return String.valueOf(-1);
    DisplayMetrics metrics = new DisplayMetrics();
    _startupActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    double x = metrics.widthPixels / metrics.xdpi;
    double y = metrics.heightPixels / metrics.ydpi;
    return String.valueOf(Math.sqrt(x * x + y * y));
  }

  public String getScreenDpi() {
    if (_startupActivity == null) return String.valueOf(-1);
    DisplayMetrics metrics = new DisplayMetrics();
    _startupActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    return String.valueOf(metrics.densityDpi);
  }

}
