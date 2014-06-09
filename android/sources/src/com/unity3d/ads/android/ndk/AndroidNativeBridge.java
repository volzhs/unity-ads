// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   AndroidNativeBridge.java

package com.unity3d.ads.android.ndk;

import java.util.HashMap;

import android.app.Activity;
import android.util.Log;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;

/**
 * The Unity Ads Android JDK <-> NDK bridge
 * 
 * @author tuomasrinta
 *
 */
public class AndroidNativeBridge implements IUnityAdsListener {
	
    private static final AndroidNativeBridge self = new AndroidNativeBridge();

    private Activity parentActivity;
    private boolean bridgeInitBroadcast;
    private static int EVENT_UNITY_ADS_HIDE = 1;
    private static int EVENT_UNITY_ADS_SHOW = 2;
    private static int EVENT_UNITY_ADS_VIDEO_START = 3;
    private static int EVENT_UNITY_ADS_VIDEO_COMPLETE = 4;
    private static int EVENT_UNITY_ADS_CAMPAIGNS_AVAILABLE = 5;
    private static int EVENT_UNITY_ADS_CAMPAIGNS_FAILED = 6;	
    private static int EVENT_UNITY_ADS_VIDEO_SKIPPED = 7;

    public static void __init(int id)
    {
        self.__initAds(id);
    }

    private AndroidNativeBridge() {
        parentActivity = null;
        bridgeInitBroadcast = false;
        if(self != null)
            throw new IllegalStateException("Cannot re-instantiate AndroidNativeBridge, something is wrong.");
    }

    public void setRootActivity(Activity activity) {
        parentActivity = activity;
        UnityAds.changeActivity(activity);
        if(!bridgeInitBroadcast) {
            bridgeReady();
            bridgeInitBroadcast = true;
        }
    }

    public void onHide() {
        dispatchEvent(EVENT_UNITY_ADS_HIDE, null);
    }

    public void onShow() {
        dispatchEvent(EVENT_UNITY_ADS_SHOW, null);
    }

    public void onVideoStarted() {
        dispatchEvent(EVENT_UNITY_ADS_VIDEO_START, null);
    }

    public void onVideoCompleted(String key, boolean skipped) {
    	if(skipped) {
    		dispatchEvent(EVENT_UNITY_ADS_VIDEO_SKIPPED, key);
    	} else {
    		dispatchEvent(EVENT_UNITY_ADS_VIDEO_COMPLETE, key);
    	}
    }

    public void onFetchCompleted() {
        setRewardItems((String[])UnityAds.getRewardItemKeys().toArray(new String[0]));
        dispatchEvent(EVENT_UNITY_ADS_CAMPAIGNS_AVAILABLE, null);
    }

    public void onFetchFailed() {
        dispatchEvent(EVENT_UNITY_ADS_CAMPAIGNS_FAILED, null);
    }

    public static void __show(boolean offerScreen, boolean animated) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("noOfferScreen", Boolean.valueOf(offerScreen));
        properties.put("openAnimated", Boolean.valueOf(animated));
        UnityAds.show(properties);
    }

    public static String __getDefaultReward()
    {
        return UnityAds.getDefaultRewardItemKey();
    }

    public static String __getRewardUrl(String key)
    {
        return (String)UnityAds.getRewardItemDetailsWithKey(key)
        		.get(UnityAds.UNITY_ADS_REWARDITEM_PICTURE_KEY);
    }

    public void __initAds(int appId)
    {
        if(parentActivity == null) {
            throw new IllegalStateException("You must call setRootActivity(Activity) in your Java code prior to initializing Unity Ads.");
        } else {
            UnityAds.setDebugMode(true);
            UnityAds.init(parentActivity, (new StringBuilder(String.valueOf(appId))).toString(), this);
            Log.d("UnityAds", "new UnityAds done");
            return;
        }
    }

    public native void bridgeReady();

    public native void dispatchEvent(int i, String s);

    public native void setRewardItems(String as[]);
}
