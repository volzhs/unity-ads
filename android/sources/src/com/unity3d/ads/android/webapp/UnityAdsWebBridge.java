package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

import android.util.Log;

import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsWebBridge {
	private enum UnityAdsWebEvent { PlayVideo, PauseVideo, CloseView, InitComplete;
		@Override
		public String toString () {
			String retVal = null;
			switch (this) {
				case PlayVideo:
					retVal = "playVideo";
					break;
				case PauseVideo:
					retVal = "pauseVideo";
					break;
				case CloseView:
					retVal = "close";
					break;
				case InitComplete:
					retVal = "initComplete";
					break;
			}
			return retVal;
		}
	}
	
	private IUnityAdsWebBrigeListener _listener = null;
	
	private UnityAdsWebEvent getEventType (String event) {
		for (UnityAdsWebEvent evt : UnityAdsWebEvent.values()) {
			if (evt.toString().equals(event))
				return evt;
		}
		
		return null;
	}
	
	public UnityAdsWebBridge (IUnityAdsWebBrigeListener listener) {
		_listener = listener;
	}
	
	public void handleWebEvent (String type, String data) {
		Log.d(UnityAdsConstants.LOG_NAME, "handleWebEvent: " + data);

		if (_listener == null || data == null) return;
		
		JSONObject jsonData = null;
		JSONObject parameters = null;
		String event = type;
		
		try {
			jsonData = new JSONObject(data);
			parameters = jsonData.getJSONObject("data");
		}
		catch (Exception e) {
			Log.d(UnityAdsConstants.LOG_NAME, "Error while parsing parameters: " + e.getMessage());
		}
		
		if (jsonData == null || event == null) return;
		
		UnityAdsWebEvent eventType = getEventType(event);
		
		if (eventType == null) return;
		
		switch (eventType) {
			case PlayVideo:
				_listener.onPlayVideo(parameters);
				break;
			case PauseVideo:
				_listener.onPauseVideo(parameters);
				break;
			case CloseView:
				_listener.onCloseView(parameters);
				break;
			case InitComplete:
				_listener.onWebAppInitComplete(parameters);
				break;
		}
	}
}
