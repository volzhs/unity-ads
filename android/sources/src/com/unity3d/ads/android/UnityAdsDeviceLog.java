package com.unity3d.ads.android;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.util.Log;

public class UnityAdsDeviceLog {

	private static boolean LOGGING = true;
	private static boolean LOG_ERROR = true;
	private static boolean LOG_WARNING = true;
	private static boolean LOG_DEBUG = true;
	private static boolean LOG_INFO = true;

	public static int LOGLEVEL_NONE = 0;
	public static int LOGLEVEL_ERROR = 1;
	public static int LOGLEVEL_WARNING = 2;
	public static int LOGLEVEL_INFO = 4;
	public static int LOGLEVEL_DEBUG = 8;

	public enum UnityAdsLogLevel {
		INFO, DEBUG, WARNING, ERROR
	};

	private static HashMap<UnityAdsLogLevel, UnityAdsDeviceLogLevel> _deviceLogLevel = null;

	static {
		if (_deviceLogLevel == null) {
			_deviceLogLevel = new HashMap<UnityAdsDeviceLog.UnityAdsLogLevel, UnityAdsDeviceLogLevel>();
			_deviceLogLevel.put(UnityAdsLogLevel.INFO, new UnityAdsDeviceLogLevel(UnityAdsLogLevel.INFO, "UnityAds", "i"));
			_deviceLogLevel.put(UnityAdsLogLevel.DEBUG, new UnityAdsDeviceLogLevel(UnityAdsLogLevel.DEBUG, "UnityAds", "d"));
			_deviceLogLevel.put(UnityAdsLogLevel.WARNING, new UnityAdsDeviceLogLevel(UnityAdsLogLevel.WARNING, "UnityAds", "w"));
			_deviceLogLevel.put(UnityAdsLogLevel.ERROR, new UnityAdsDeviceLogLevel(UnityAdsLogLevel.ERROR, "UnityAds", "e"));
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
		write(UnityAdsLogLevel.DEBUG, checkMessage(message));
	}

	public static void debug(String format, Object... args) {
		debug(String.format(format, args));
	}

	public static void warning(String message) {
		write(UnityAdsLogLevel.WARNING, checkMessage(message));
	}

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
		boolean canLog = LOGGING;

		if (canLog) {
			switch (level) {
				case INFO:
					canLog = LOG_INFO;
					break;
				case DEBUG:
					canLog = LOG_DEBUG;
					break;
				case WARNING:
					canLog = LOG_WARNING;
					break;
				case ERROR:
					canLog = LOG_ERROR;
					break;
				default:
					break;
			}
		}

		if (canLog) {
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
		StackTraceElement e = null;
		UnityAdsDeviceLogLevel logLevel = getLogLevel(level);
		UnityAdsDeviceLogEntry logEntry = null;

		if (logLevel != null) {
			int callerIndex = 0;
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
			else {
			}
		}
		else {
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
			}

			if (receivingMethod != null) {
				try {
					receivingMethod.invoke(null, logEntry.getLogLevel().getLogTag(), logEntry.getParsedMessage());
				}
				catch (Exception e) {
				}
			}
		}
	}
}
