package com.unity3d.ads.android.unity3d;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.zone.UnityAdsZoneManager;

import java.util.HashMap;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsUnityEngineWrapper implements IUnityAdsListener {
  private static Boolean _initialized = false;

  public UnityAdsUnityEngineWrapper () {
  }

  // Public methods

  public void init (final Activity activity, final String gameId, final boolean testMode, final int logLevel, final String unityVersion) {
    if (!_initialized) {
      _initialized = true;

      final UnityAdsUnityEngineWrapper listener = this;

      try {
        UnityAdsUtils.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            UnityAdsDeviceLog.setLogLevel(logLevel);
            UnityAds.setTestMode(testMode);
            if(unityVersion.length() > 0) {
              UnityAdsProperties.UNITY_VERSION = unityVersion;
            }
            UnityAds.init(activity, gameId, listener);
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

      if(canShowAds(zoneId)) {
        if (rewardItemKey.length() > 0) {
          UnityAds.setZone(zoneId, rewardItemKey);
        } else {
          if (zoneId.length() > 0) {
            UnityAds.setZone(zoneId);
          }
        }
      } else {
        return false;
      }

      return UnityAds.show(options);
    }

    return false;
  }

  public boolean canShowAds(String zone) {
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

  public void setLogLevel(int logLevel) {
    UnityAdsDeviceLog.setLogLevel(logLevel);
  }

  public void setCampaignDataURL(String url) { UnityAds.setCampaignDataURL(url); }

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

}
