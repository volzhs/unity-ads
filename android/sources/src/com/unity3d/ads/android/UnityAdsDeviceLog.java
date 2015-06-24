package com.unity3d.ads.android;

import java.lang.reflect.Method;
import java.util.HashMap;
import android.util.Log;

public class UnityAdsDeviceLog {

	private static boolean LOG_ERROR = true;
	private static boolean LOG_WARNING = true;
	private static boolean LOG_DEBUG = false;
	private static boolean LOG_INFO = true;

	public static final int LOGLEVEL_ERROR = 1;
	public static final int LOGLEVEL_WARNING = 2;
	public static final int LOGLEVEL_INFO = 4;
	public static final int LOGLEVEL_DEBUG = 8;

	public enum UnityAdsLogLevel {
		INFO, DEBUG, WARNING, ERROR
	}

	private static HashMap<UnityAdsLogLevel, UnityAdsDeviceLogLevel> _deviceLogLevel = null;

	static {
		if (_deviceLogLevel == null) {
			_deviceLogLevel = new HashMap<>();
			_deviceLogLevel.put(UnityAdsLogLevel.INFO, new UnityAdsDeviceLogLevel("UnityAds", "i"));
			_deviceLogLevel.put(UnityAdsLogLevel.DEBUG, new UnityAdsDeviceLogLevel("UnityAds", "d"));
			_deviceLogLevel.put(UnityAdsLogLevel.WARNING, new UnityAdsDeviceLogLevel("UnityAds", "w"));
			_deviceLogLevel.put(UnityAdsLogLevel.ERROR, new UnityAdsDeviceLogLevel("UnityAds", "e"));
		}
	}

	public UnityAdsDeviceLog() {
	}

	public static void setLogLevel(int newLevel) {
		if(newLevel >= LOGLEVEL_DEBUG) {
			LOG_ERROR = true;
			LOG_WARNING = true;
			LOG_INFO = true;
			LOG_DEBUG = true;
		} else if(newLevel >= LOGLEVEL_INFO) {
			LOG_ERROR = true;
			LOG_WARNING = true;
			LOG_INFO = true;
			LOG_DEBUG = false;
		} else if(newLevel >= LOGLEVEL_WARNING) {
			LOG_ERROR = true;
			LOG_WARNING = true;
			LOG_INFO = false;
			LOG_DEBUG = false;
		} else if(newLevel >= LOGLEVEL_ERROR) {
			LOG_ERROR = true;
			LOG_WARNING = false;
			LOG_INFO = false;
			LOG_DEBUG = false;
		} else {
			LOG_ERROR = false;
			LOG_WARNING = false;
			LOG_INFO = false;
			LOG_DEBUG = false;
		}
	}

	public static void entered() {
		debug("ENTERED METHOD");
	}

	public static void info(String message) {
		write(UnityAdsLogLevel.INFO, checkMessage(message));
	}

	public static void info(String format, Object... args) {
		info(String.format(format, args));
	}

	public static void debug(String message) {
		int maxDebugMsgLength = 3072;

		if(message.length() > maxDebugMsgLength) {
			debug(message.substring(0,maxDebugMsgLength));

			if(message.length() < 10 * maxDebugMsgLength) {
				debug(message.substring(maxDebugMsgLength));
			}

			return;
		}

		write(UnityAdsLogLevel.DEBUG, checkMessage(message));
	}

	public static void debug(String format, Object... args) {
		debug(String.format(format, args));
	}

	public static void warning(String message) {
		write(UnityAdsLogLevel.WARNING, checkMessage(message));
	}

	@SuppressWarnings({"unused"})
	public static void warning(String format, Object... args) {
		warning(String.format(format, args));
	}

	public static void error(String message) {
		write(UnityAdsLogLevel.ERROR, checkMessage(message));
	}

	public static void error(String format, Object... args) {
		error(String.format(format, args));
	}

	private static void write(UnityAdsLogLevel level, String message) {
		boolean LOG_THIS_MSG = true;

		switch (level) {
			case INFO:
				LOG_THIS_MSG = LOG_INFO;
				break;
			case DEBUG:
				LOG_THIS_MSG = LOG_DEBUG;
				break;
			case WARNING:
				LOG_THIS_MSG = LOG_WARNING;
				break;
			case ERROR:
				LOG_THIS_MSG = LOG_ERROR;
				break;
			default:
				break;
		}

		if (LOG_THIS_MSG) {
			UnityAdsDeviceLogEntry logEntry = createLogEntry(level, message);
			writeToLog(logEntry);
		}
	}

	private static String checkMessage (String message) {
		if (message == null || message.length() == 0) {
			message = "DO NOT USE EMPTY MESSAGES, use UnityAdsDeviceLog.entered() instead";
		}
		
		return message;
	}
	
	private static UnityAdsDeviceLogLevel getLogLevel(UnityAdsLogLevel logLevel) {
		return _deviceLogLevel.get(logLevel);
	}

	private static UnityAdsDeviceLogEntry createLogEntry(UnityAdsLogLevel level, String message) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement e;
		UnityAdsDeviceLogLevel logLevel = getLogLevel(level);
		UnityAdsDeviceLogEntry logEntry = null;

		if (logLevel != null) {
			int callerIndex;
			boolean markedIndex = false;

			for (callerIndex = 0; callerIndex < stack.length; callerIndex++) {
				e = stack[callerIndex];
				if (e.getClassName().equals(UnityAdsDeviceLog.class.getName())) {
					markedIndex = true;
				}
				if (!e.getClassName().equals(UnityAdsDeviceLog.class.getName()) && markedIndex) {
					break;
				}
			}

			e = null;

			if (callerIndex < stack.length) {
				e = stack[callerIndex];
			}

			if (e != null) {
				logEntry = new UnityAdsDeviceLogEntry(logLevel, message, e);
			}
		}

		return logEntry;
	}

	private static void writeToLog(UnityAdsDeviceLogEntry logEntry) {
		Method receivingMethod = null;

		if (logEntry != null && logEntry.getLogLevel() != null) {
			try {
				receivingMethod = Log.class.getMethod(logEntry.getLogLevel().getReceivingMethodName(), String.class, String.class);
			}
			catch (Exception e) {
				Log.e("UnityAds", "Writing to log failed!");
			}

			if (receivingMethod != null) {
				try {
					receivingMethod.invoke(null, logEntry.getLogLevel().getLogTag(), logEntry.getParsedMessage());
				}
				catch (Exception e) {
					Log.e("UnityAds", "Writing to log failed!");
				}
			}
		}
	}
}