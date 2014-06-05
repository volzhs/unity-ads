package com.unity3d.ads.android;

public class UnityAdsDeviceLogEntry {

	private UnityAdsDeviceLogLevel _logLevel = null;
	private String _originalMessage = null;
	private StackTraceElement _stackTraceElement = null;
	
	public UnityAdsDeviceLogEntry (UnityAdsDeviceLogLevel logLevel, String originalMessage, StackTraceElement stackTraceElement) {
		_logLevel = logLevel;
		_originalMessage = originalMessage;
		_stackTraceElement = stackTraceElement;
	}
	
	public UnityAdsDeviceLogLevel getLogLevel () {
		return _logLevel;
	}
	
	public String getOriginalMessage () {
		return _originalMessage;
	}
	
	public String getParsedMessage () {
		String message = _originalMessage;
		String className = "UnknownClass";
		String methodName = "unknownMethod";
		int lineNumber = -1;
		
		if (_stackTraceElement != null) {
			className = _stackTraceElement.getClassName();
			methodName = _stackTraceElement.getMethodName();
			lineNumber = _stackTraceElement.getLineNumber();
		}
		
		if (message != null && message.length() > 0) message = " :: " + message;
		if (message == null) message = "";
		
		String lineNumberPart =  " (line:" + lineNumber + ")";
		
		return className + "." + methodName + "()" + lineNumberPart + message;
	}
}
