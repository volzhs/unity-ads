package com.unity3d.ads.android.webapp;

import org.json.JSONObject;

public interface IUnityAdsWebBridgeListener {
	void onPlayVideo (JSONObject data);
	void onPauseVideo (@SuppressWarnings("UnusedParameters") JSONObject data);
	void onCloseAdsView (@SuppressWarnings("UnusedParameters") JSONObject data);
	void onWebAppLoadComplete (@SuppressWarnings("UnusedParameters") JSONObject data);
	void onWebAppInitComplete (@SuppressWarnings("UnusedParameters") JSONObject data);
	void onOrientationRequest (JSONObject data);
	void onOpenPlayStore (JSONObject data);
	void onLaunchIntent(JSONObject data);
}
