package com.unity3d.ads.android;

public class UnityAdsDeviceLogLevel {

	private String _receivingMethodName = null;
	private String _logTag = null;

	public UnityAdsDeviceLogLevel(String logTag, String receivingMethodName) {
		_logTag = logTag;
		_receivingMethodName = receivingMethodName;
	}

	public String getLogTag () {
		return _logTag;
	}

	public String getReceivingMethodName () {
		return _receivingMethodName;
	}
}
