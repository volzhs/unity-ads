package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.UnityAdsDeviceLog;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

public class UnityAdsCacheThread extends Thread {
	private static int MSG_DOWNLOAD = 1;

	private static UnityAdsCacheThread _thread = null;
	private static UnityAdsCacheThreadHandler _handler = null;
	private static boolean _ready = false;
	private static Object _readyLock = new Object();

	private static void init() {
		_readyLock = new Object();
		_thread = new UnityAdsCacheThread();
		_thread.setName("UnityAdsCacheThread");
		_thread.start();

		while(!_ready) {
			try {
				synchronized(_readyLock) {
					_readyLock.wait();
				}
			} catch (InterruptedException e) { }
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