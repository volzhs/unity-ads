package com.unity3d.ads.android.cache;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.data.UnityAdsDevice;
import com.unity3d.ads.android.properties.UnityAdsProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;

class UnityAdsCacheThreadHandler extends Handler {
	private String _currentDownload = null;
	private boolean _stopped = false;

	@Override
	public void handleMessage(Message msg) {
		Bundle data = msg.getData();
		String source = data.getString("source");
		String target = data.getString("target");

		downloadFile(source, target);
	}

	public void setStoppedStatus(boolean stopped) {
		_stopped = stopped;
	}

	public String getCurrentDownload() {
		return _currentDownload;
	}

	private void downloadFile(String source, String target) {
		if(_stopped || source == null || target == null) return;

		// TODO: Fix this try / catch block, it is WAY too long
		try {
			UnityAdsDeviceLog.debug("Unity Ads cache: start downloading " + source + " to " + target);
			_currentDownload = target;

			long startTime = SystemClock.elapsedRealtime();

			URL url = new URL(source);

			if (!UnityAdsDevice.isActiveNetworkConnected()) {
				UnityAdsDeviceLog.debug("Unity Ads cache: download cancelled, no internet connection available");
				return;
			}

			File targetFile = new File(target);
			FileOutputStream fileOutput = new FileOutputStream(targetFile);
			BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);

			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.connect();


			BufferedInputStream bufferedInput = new BufferedInputStream(conn.getInputStream());

			byte data[] = new byte[4096];
			long total = 0;
			int count;

			while(!_stopped && (count = bufferedInput.read(data)) != -1) {
				total += count;
				bufferedOutput.write(data, 0, count);
			}

			bufferedOutput.flush();
			bufferedOutput.close();
			bufferedInput.close();

			_currentDownload = null;

			if(!_stopped) {
				long duration = SystemClock.elapsedRealtime() - startTime;

				UnityAdsDeviceLog.debug("Unity Ads cache: File " + target + " of " + total + " bytes downloaded in " + duration + "ms");

				if(duration > 0 && total > 0) {
					UnityAdsProperties.CACHING_SPEED = total / duration;
				}
			} else {
				UnityAdsDeviceLog.debug("Unity Ads cache: downloading of " + source + " stopped");
				boolean success = targetFile.delete();
				if (!success) UnityAdsDeviceLog.debug("Couldn't delete file: " + targetFile.getName());
			}
		} catch (Exception e) {
			_currentDownload = null;
			UnityAdsDeviceLog.debug("Unity Ads cache: Exception when downloading " + source + " to " + target + ": " + e.getMessage());
		}
	}
}