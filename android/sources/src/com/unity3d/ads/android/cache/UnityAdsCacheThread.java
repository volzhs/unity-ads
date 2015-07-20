package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.UnityAdsDeviceLog;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

class UnityAdsCacheThread extends Thread {
	private static final int MSG_DOWNLOAD = 1;
	private static UnityAdsCacheThreadHandler _handler = null;
	private static boolean _ready = false;
	private static final Object _readyLock = new Object();

	private static void init() {
		UnityAdsCacheThread thread = new UnityAdsCacheThread();
		thread.setName("UnityAdsCacheThread");
		thread.start();

		while(!_ready) {
			try {
				synchronized(_readyLock) {
					_readyLock.wait();
				}
			} catch (InterruptedException e) {
				UnityAdsDeviceLog.debug("Couldn't synchronize thread");
			}
		}
	}

	@Override
	public void run() {
		Looper.prepare();
		_handler = new UnityAdsCacheThreadHandler();
		_ready = true;
		synchronized(_readyLock) {
			_readyLock.notify();
		}
		Looper.loop();
	}

	public static synchronized void download(String source, String target) {
		if(!_ready) init();

		Bundle params = new Bundle();
		params.putString("source", source);
		params.putString("target", target);

		Message msg = new Message();
		msg.what = MSG_DOWNLOAD;
		msg.setData(params);

		_handler.setStoppedStatus(false);
		_handler.sendMessage(msg);
	}

	public static String getCurrentDownload() {
		if(!_ready) return null;

		return _handler.getCurrentDownload();
	}

	public static void stopAllDownloads() {
		if(!_ready) return;

		_handler.removeMessages(MSG_DOWNLOAD);
		_handler.setStoppedStatus(true);
	}
}