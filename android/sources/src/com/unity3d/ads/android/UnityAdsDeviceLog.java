package com.unity3d.ads.android;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

import android.util.Log;

public class UnityAdsDeviceLog {

	private static boolean LOG_ERROR = true;
	private static boolean LOG_WARNING = true;
	private static boolean LOG_DEBUG = false;
	private static boolean LOG_INFO = true;

	private static final int LOGLEVEL_ERROR = 1;
	private static final int LOGLEVEL_WARNING = 2;
	public static final int LOGLEVEL_INFO = 4;
	public static final int LOGLEVEL_DEBUG = 8;

	public enum UnityAdsLogLevel {
		INFO, DEBUG, WARNING, ERROR
	}

	private static final HashMap<UnityAdsLogLevel, UnityAdsDeviceLogLevel> _deviceLogLevel = new HashMap<>();

	static {
		if (_deviceLogLevel.size() == 0) {
			_deviceLogLevel.put(UnityAdsLogLevel.INFO, new UnityAdsDeviceLogLevel("i"));
			_deviceLogLevel.put(UnityAdsLogLevel.DEBUG, new UnityAdsDeviceLogLevel("d"));
			_deviceLogLevel.put(UnityAdsLogLevel.WARNING, new UnityAdsDeviceLogLevel("w"));
			_deviceLogLevel.put(UnityAdsLogLevel.ERROR, new UnityAdsDeviceLogLevel("e"));
		}
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

	@SuppressWarnings({"unused"})
	public static void info(String format, Object... args) {
		info(String.format(Locale.US, format, args));
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

	@SuppressWarnings("unused")
	public static void debug(String format, Object... args) {
		debug(String.format(Locale.US, format, args));
	}

	public static void warning(String message) {
		write(UnityAdsLogLevel.WARNING, checkMessage(message));
	}

	@SuppressWarnings({"unused"})
	public static void warning(String format, Object... args) {
		warning(String.format(Locale.US, format, args));
	}

	public static void error(String message) {
		write(UnityAdsLogLevel.ERROR, checkMessage(message));
	}

	@SuppressWarnings("unused")
	public static void error(String format, Object... args) {
		error(String.format(Locale.US, format, args));
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

	/* Special logging for canShow -method, so that it doesn't keep repeating same messages */

	private static void buildShowStatusMessages() {
		if (_showStatusMessages == null || _showStatusMessages.size() == 0) {
			_showStatusMessages = new HashMap<>();
			_showStatusMessages.put(UnityAdsShowMsg.READY, "Unity Ads is ready to show ads");
			_showStatusMessages.put(UnityAdsShowMsg.NOT_INITIALIZED, "not initialized");
			_showStatusMessages.put(UnityAdsShowMsg.WEBAPP_NOT_INITIALIZED, "webapp not initialized");
			_showStatusMessages.put(UnityAdsShowMsg.SHOWING_ADS, "already showing ads");
			_showStatusMessages.put(UnityAdsShowMsg.NO_INTERNET, "no internet connection available");
			_showStatusMessages.put(UnityAdsShowMsg.NO_ADS, "no ads are available");
			_showStatusMessages.put(UnityAdsShowMsg.ZERO_ADS, "zero ads available");
			_showStatusMessages.put(UnityAdsShowMsg.VIDEO_NOT_CACHED, "video not cached");
		}
	}

	public enum UnityAdsShowMsg {
		READY, NOT_INITIALIZED, WEBAPP_NOT_INITIALIZED, SHOWING_ADS, NO_INTERNET, NO_ADS, ZERO_ADS, VIDEO_NOT_CACHED
	}

	private static HashMap<UnityAdsShowMsg, String> _showStatusMessages;
	private static UnityAdsShowMsg _previousMsg;

	public static void logShowStatus (UnityAdsShowMsg reason) {
		if (reason != _previousMsg) {
			buildShowStatusMessages();
			_previousMsg = reason;
			String msg = _showStatusMessages.get(reason);
			if (reason != UnityAdsShowMsg.READY) msg = "Unity Ads cannot show ads: " + msg;
			UnityAdsDeviceLog.info(msg);
		}
	}
}