package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

public interface IUnityAdsWebBridgeListener {
	void onPlayVideo (JSONObject data);
	void onPauseVideo (JSONObject data);
	void onCloseAdsView (JSONObject data);
	void onWebAppLoadComplete (JSONObject data);
	void onWebAppInitComplete (JSONObject data);
	void onOrientationRequest (JSONObject data);
	void onOpenPlayStore (JSONObject data);
	void onLaunchIntent(JSONObject data);
}
