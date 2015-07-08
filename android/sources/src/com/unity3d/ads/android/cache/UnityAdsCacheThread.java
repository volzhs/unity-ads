package com.unity3d.ads.android.cache;

import com.unity3d.ads.android.UnityAdsDeviceLog;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

public class UnityAdsCacheThread extends Thread {
	private static int MSG_DOWNLOAD = 1;

	private static UnityAdsCacheThread _thread = null;
	private static UnityAdsCacheThreadHandler _handler = null;
	private static boolean ready = false;

	private static void init() {
		_thread = new UnityAdsCacheThread();
		_thread.setName("UnityAdsCacheThread");
		_thread.start();

		// TODO: Fix this with proper locking
		while(!ready) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		Looper.prepare();
		_handler = new UnityAdsCacheThreadHandler();
		ready = true;
		Looper.loop();
	}

	public static void download(String source, String target) {
		if(_thread == null) init();

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
		if(_handler == null) return null;

		return _handler.getCurrentDownload();
	}

	public static void stopAllDownloads() {
		if(_thread == null) {
			init();
		} else {
			_handler.removeMessages(MSG_DOWNLOAD);
			_handler.setStoppedStatus(true);
		}
	}
}