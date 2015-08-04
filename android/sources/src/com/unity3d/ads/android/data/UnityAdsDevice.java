package com.unity3d.ads.android.data;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsDevice {
	public static String getSoftwareVersion () {
		return "" + Build.VERSION.SDK_INT;
	}

	public static String getHardwareVersion () {
		return Build.MANUFACTURER + " " + Build.MODEL;
	}

	public static int getDeviceType () {
		return UnityAdsProperties.APPLICATION_CONTEXT.getResources().getConfiguration().screenLayout;
	}

	@SuppressLint("DefaultLocale")
	public static String getAndroidId (boolean md5hashed) {
		String androidID;

		try {
			androidID = Secure.getString(UnityAdsProperties.APPLICATION_CONTEXT.getContentResolver(), Secure.ANDROID_ID);

			if(md5hashed) {
				androidID = UnityAdsUtils.Md5(androidID);
				androidID = androidID.toLowerCase();
			}
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("Problems fetching androidId: " + e.getMessage());
			androidID = UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		}

		return androidID;
	}

    public static String getAdvertisingTrackingId() {
    	return UnityAdsAdvertisingId.getAdvertisingTrackingId();
    }

    public static boolean isLimitAdTrackingEnabled() {
    	return UnityAdsAdvertisingId.getLimitedAdTracking();
    }

	public static String getConnectionType () {
		if (isUsingWifi()) {
			return "wifi";
		}
		
		return "cellular";
	}

	@SuppressWarnings("deprecation")
	public static boolean isUsingWifi () {
		ConnectivityManager mConnectivity;

		mConnectivity = (ConnectivityManager) UnityAdsProperties.APPLICATION_CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (mConnectivity == null) return false;

		TelephonyManager mTelephony;
		mTelephony = (TelephonyManager) UnityAdsProperties.APPLICATION_CONTEXT.getSystemService(Context.TELEPHONY_SERVICE);

		// Skip if no connection, or background data disabled
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null || !mConnectivity.getBackgroundDataSetting() || !mConnectivity.getActiveNetworkInfo().isConnected() || mTelephony == null) {
			return false;
		}

		int netType = info.getType();
		return netType == ConnectivityManager.TYPE_WIFI && info.isConnected();
	}

	public static int getNetworkType() {
		TelephonyManager tm = (TelephonyManager)UnityAdsProperties.APPLICATION_CONTEXT.getSystemService(Context.TELEPHONY_SERVICE);

		return tm.getNetworkType();
	}

	public static int getScreenDensity() {
		return UnityAdsProperties.APPLICATION_CONTEXT.getResources().getDisplayMetrics().densityDpi;
	}

	public static int getScreenSize() {
		return getDeviceType();
	}

	private static JSONArray getPackageJsonArray(Map<String,String> whitelist) {
		if(whitelist == null || whitelist.size() == 0) return null;

		PackageManager pm = UnityAdsProperties.APPLICATION_CONTEXT.getPackageManager();
		JSONArray pkgList = null;

		for(PackageInfo pkg : pm.getInstalledPackages(0)) {
			try {
				if(pkg.packageName != null && pkg.packageName.length() > 0) {
					String md5pkg = UnityAdsUtils.Md5(pkg.packageName);

					if(whitelist.containsKey(md5pkg)) {
						whitelist.get(md5pkg);
						JSONObject jsonEntry = new JSONObject();

						jsonEntry.put("id", whitelist.get(md5pkg));
						if(pkg.firstInstallTime > 0) {
							jsonEntry.put("timestamp", pkg.firstInstallTime);
						}

						if(pkgList == null) pkgList = new JSONArray();
						pkgList.put(jsonEntry);
					}
				}
			} catch(Exception e) {
				UnityAdsDeviceLog.debug("Exception when processing package " + pkg.packageName + " " + e);
			}
		}

		return pkgList;
	}

	public static String getPackageDataJson(Map<String,String> whitelist) {
		JSONArray packages = getPackageJsonArray(whitelist);
		if(packages == null) return null;

		JSONObject wrapper = new JSONObject();
		try {
			wrapper.put("games", packages);
			return wrapper.toString();
		} catch(Exception e) {
			UnityAdsDeviceLog.debug("Exception in getPackageDataJson" + e);
			return null;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isActiveNetworkConnected () {
		ConnectivityManager cm = (ConnectivityManager)UnityAdsProperties.APPLICATION_CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE);

		if(cm != null) {
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			return activeNetwork != null && activeNetwork.isConnected();
		}

		return false;
	}
}