package com.unity3d.ads.android.data;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

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
	
	public static String getOldMacAddress () {
		String deviceId ="unknown";
		
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
			deviceId ="unknown";
		
		return deviceId.toLowerCase();
	}
	
    public static String getMacAddress() {
		NetworkInterface intf = null;
		intf = getInterfaceFor("eth0");		
		if (intf == null) {
			intf = getInterfaceFor("wlan0");
		}
		
		UnityAdsUtils.Log("Old mac: " + getOldMacAddress(), UnityAdsDevice.class);
		return buildMacAddressFromInterface(intf);
    }
	
    public static String buildMacAddressFromInterface (NetworkInterface intf) {
		byte[] mac = null;
		
		if (intf == null) {
			return UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		}
		
		try
		{
			Method layertype = NetworkInterface.class.getMethod("getHardwareAddress");
			mac = (byte[])layertype.invoke(intf);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Could not getHardwareAddress", UnityAdsDevice.class);
		}
		
		if (mac == null) {
			return UnityAdsConstants.UNITY_ADS_DEVICEID_UNKNOWN;
		}
		
		StringBuilder buf = new StringBuilder();
        for (int idx=0; idx<mac.length; idx++)
            buf.append(String.format("%02X:", mac[idx]));       
        if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
        
        String retMacAddress = UnityAdsUtils.Md5(buf.toString());
        return retMacAddress.toLowerCase();    	
    }
    
    public static NetworkInterface getInterfaceFor (String interfaceName) {
    	List<NetworkInterface> interfaces = null;
    	
        try {
        	interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			UnityAdsUtils.Log("Got interface list", UnityAdsDevice.class);
        }
        catch (Exception e) {
        	return null;
        }
        
    	for (NetworkInterface intf : interfaces) {
    		if (interfaceName != null) {
    			UnityAdsUtils.Log("Interface '" + intf.getName() + "' mac=" + buildMacAddressFromInterface(intf), UnityAdsDevice.class);
    			if (intf.getName().equalsIgnoreCase(interfaceName)) {
    				UnityAdsUtils.Log("Returning interface: " + intf.getName(), UnityAdsDevice.class);
    				return intf;
    			}
    				
            }
    	}
        
    	return null;
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
