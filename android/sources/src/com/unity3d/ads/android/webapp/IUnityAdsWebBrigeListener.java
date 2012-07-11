package com.unity3d.ads.android.webapp;

public interface IUnityAdsWebBrigeListener {
	public void onPlayVideo (String data);
	public void onPauseVideo (String data);
	public void onVideoCompleted (String data);
	public void onCloseView (String data);
}
