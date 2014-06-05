package com.unity3d.ads.android;

public class UnityAdsDeviceLogLevel {

	private UnityAdsDeviceLog.UnityAdsLogLevel _level = null;
	private String _receivingMethodName = null;
	private String _logTag = null;
	
	public UnityAdsDeviceLogLevel(UnityAdsDeviceLog.UnityAdsLogLevel level, String logTag, String receivingMethodName) {
		_level = level;
		_logTag = logTag;
		_receivingMethodName = receivingMethodName;
	}
	
	public UnityAdsDeviceLog.UnityAdsLogLevel getLevel () {
		return _level;
	}
	
	public String getLogTag () {
		return _logTag;
	}
	
	public String getReceivingMethodName () {
		return _receivingMethodName;
	}
}
