package com.unity3d.ads.android;

public class UnityAdsDeviceLogLevel {

	private String _receivingMethodName = null;
	private static final String LOG_TAG = "UnityAds";

	public UnityAdsDeviceLogLevel(String receivingMethodName) {
		_receivingMethodName = receivingMethodName;
	}

	public String getLogTag () {
		return LOG_TAG;
	}

	public String getReceivingMethodName () {
		return _receivingMethodName;
	}
}
