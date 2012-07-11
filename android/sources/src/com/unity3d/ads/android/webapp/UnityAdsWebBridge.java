package com.unity3d.ads.android.webapp;


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
	
	public void handleWebEvent (String event, String data) {
		if (_listener == null) return;
		
		UnityAdsWebEvent eventType = getEventType(event);
		
		switch (eventType) {
			case PlayVideo:
				_listener.onPlayVideo(data);
				break;
			case PauseVideo:
				_listener.onPauseVideo(data);
				break;
			case CloseView:
				_listener.onCloseView(data);
				break;
			case VideoCompleted:
				_listener.onVideoCompleted(data);
				break;
		}
	}
}
