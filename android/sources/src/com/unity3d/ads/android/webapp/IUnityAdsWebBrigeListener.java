package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

public interface IUnityAdsWebBrigeListener {
	public void onPlayVideo (JSONObject data);
	public void onPauseVideo (JSONObject data);
	public void onCloseView (JSONObject data);
}
