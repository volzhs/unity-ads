package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

public interface IUnityAdsWebBridgeListener {
	public void onPlayVideo (JSONObject data);
	public void onPauseVideo (JSONObject data);
	public void onCloseAdsView (JSONObject data);
	public void onWebAppLoadComplete (JSONObject data);
	public void onWebAppInitComplete (JSONObject data);
	public void onOrientationRequest (JSONObject data);
	public void onOpenPlayStore (JSONObject data);
}
