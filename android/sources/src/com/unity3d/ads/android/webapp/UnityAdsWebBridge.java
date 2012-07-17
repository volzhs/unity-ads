package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

import android.util.Log;

import com.unity3d.ads.android.UnityAdsProperties;


public class UnityAdsWebBridge {
	private enum UnityAdsWebEvent { PlayVideo, PauseVideo, VideoCompleted, CloseView;
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
				case VideoCompleted:
					retVal = "videoCompleted";
					break;
				case CloseView:
					retVal = "close";
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
	
	public void handleWebEvent (String data) {
		if (_listener == null || data == null) return;
		
		JSONObject paramObj = null;
		String event = null;
		
		try {
			paramObj = new JSONObject(data);
			event = paramObj.getString("type");
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Error while parsing parameters: " + e.getMessage());
		}
		
		if (paramObj == null || event == null) return;
		
		UnityAdsWebEvent eventType = getEventType(event);
		
		switch (eventType) {
			case PlayVideo:
				_listener.onPlayVideo(paramObj);
				break;
			case PauseVideo:
				_listener.onPauseVideo(paramObj);
				break;
			case CloseView:
				_listener.onCloseView(paramObj);
				break;
			case VideoCompleted:
				_listener.onVideoCompleted(paramObj);
				break;
		}
	}
}
