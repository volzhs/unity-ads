package com.unity3d.ads.android.data;

import java.lang.reflect.Method;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class UnityAdsDevice {
	
	public static String getSoftwareVersion () {
		return "" + Build.VERSION.SDK_INT;
	}
	
	public static String getHardwareVersion () {
		return Build.MANUFACTURER + " " + Build.MODEL;
	}
	
	public static int getDeviceType () {
		return UnityAdsProperties.CURRENT_ACTIVITY.getResources().getConfiguration().screenLayout;
	}
	
	public static String getOdin1Id () {
		String odin1ID = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		try {
			odin1ID = UnityAdsUtils.SHA1(Secure.getString(UnityAdsProperties.CURRENT_ACTIVITY.getContentResolver(), Secure.ANDROID_ID));
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not resolve ODIN1 Id: " + e.getMessage(), UnityAdsDevice.class);
		}
		
		return odin1ID;
	}

	public static String getAndroidId () {
		String androidID = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		try {
			androidID = UnityAdsUtils.Md5(Secure.getString(UnityAdsProperties.CURRENT_ACTIVITY.getContentResolver(), Secure.ANDROID_ID));
			androidID = androidID.toLowerCase();
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Problems fetching androidId: " + e.getMessage(), UnityAdsDevice.class);
		}
		
		return androidID;
	}
	
	public static String getTelephonyId () {
		String telephonyID = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		try {
			TelephonyManager tmanager = (TelephonyManager)UnityAdsProperties.CURRENT_ACTIVITY.getSystemService(Context.TELEPHONY_SERVICE);
			telephonyID = UnityAdsUtils.Md5(tmanager.getDeviceId());
			telephonyID = telephonyID.toLowerCase();
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Problems fetching telephonyId: " + e.getMessage(), UnityAdsDevice.class);
		}
		
		return telephonyID;
	}
	
	public static String getAndroidSerial () {
		String androidSerial = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		try {
	        Class<?> c = Class.forName("android.os.SystemProperties");
	        Method get = c.getMethod("get", String.class);
	        androidSerial = (String) get.invoke(c, "ro.serialno");
	        androidSerial = UnityAdsUtils.Md5(androidSerial);
	        androidSerial = androidSerial.toLowerCase();
	    } 
		catch (Exception e) {
	    }
		
		return androidSerial;
	}
	
	public static String getMacAddress () {
		String deviceId = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		if (UnityAdsProperties.CURRENT_ACTIVITY == null) return deviceId;
		
		Context context = UnityAdsProperties.CURRENT_ACTIVITY;

		try {
			WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			
			Boolean originalStatus = wm.isWifiEnabled();
			if (!originalStatus)
				wm.setWifiEnabled(true);
			
			deviceId = UnityAdsUtils.Md5(wm.getConnectionInfo().getMacAddress());
			wm.setWifiEnabled(originalStatus);
		} 
		catch (Exception e) {
			//maybe no permissons or wifi off
		}
		
		if (deviceId == null)
			deviceId = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		
		return deviceId.toLowerCase();
	}
	
	public static String getOpenUdid () {
		String deviceId = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		UnityAdsOpenUDID.syncContext(UnityAdsProperties.CURRENT_ACTIVITY);
		deviceId = UnityAdsUtils.Md5(UnityAdsOpenUDID.getOpenUDIDInContext());
		return deviceId.toLowerCase();
	}
	
	public static String getConnectionType () {
		if (isUsingWifi()) {
			return "wifi";
		}
		
		return "cellular";
	}
	
	public static boolean isUsingWifi () {
		ConnectivityManager mConnectivity = null;
		mConnectivity = (ConnectivityManager)UnityAdsProperties.CURRENT_ACTIVITY.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (mConnectivity == null)
			return false;

		TelephonyManager mTelephony = (TelephonyManager)UnityAdsProperties.CURRENT_ACTIVITY.getSystemService(Context.TELEPHONY_SERVICE);
		// Skip if no connection, or background data disabled
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null || !mConnectivity.getBackgroundDataSetting() || mTelephony == null) {
		    return false;
		}

		int netType = info.getType();
		if (netType == ConnectivityManager.TYPE_WIFI) {
		    return info.isConnected();
		}
		else {
			return false;
		}
	}
	
	public static int getScreenDensity () {
		return UnityAdsProperties.CURRENT_ACTIVITY.getResources().getDisplayMetrics().densityDpi;
	}
	
	public static int getScreenSize () {
		return getDeviceType();
	}
}
