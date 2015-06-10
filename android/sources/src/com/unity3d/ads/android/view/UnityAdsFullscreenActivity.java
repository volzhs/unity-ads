package com.unity3d.ads.android.view;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.webapp.IUnityAdsWebBridgeListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.zone.UnityAdsIncentivizedZone;
import com.unity3d.ads.android.zone.UnityAdsZone;

import org.json.JSONObject;

public class UnityAdsFullscreenActivity extends Activity implements IUnityAdsMainViewListener, IUnityAdsWebBridgeListener {

	private String _currentView = null;

	private void setupViews () {
		if (UnityAds.mainview != null) {
			UnityAdsDeviceLog.debug("View was not destroyed, trying to destroy it");
			UnityAds.mainview.webview.destroy();
			UnityAds.mainview = null;
		}

		if (UnityAds.mainview == null) {
			UnityAds.mainview = new UnityAdsMainView(this, this, this);
		}
	}

	private void open (final String view) {
		Boolean dataOk = true;
		final JSONObject data = new JSONObject();

		try  {
			UnityAdsZone zone = UnityAdsWebData.getZoneManager().getCurrentZone();

			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_OPEN);
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ZONE_KEY, zone.getZoneId());

			if(zone.isIncentivized()) {
				UnityAdsRewardItemManager itemManager = ((UnityAdsIncentivizedZone)zone).itemManager();
				data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_REWARD_ITEM_KEY, itemManager.getCurrentItem().getKey());
			}
		}
		catch (Exception e) {
			dataOk = false;
		}

		UnityAdsDeviceLog.debug("DataOk: " + dataOk);

		if (dataOk && view != null) {
			UnityAdsDeviceLog.debug("Opening with view:" + view + " and data:" + data.toString());

			if (UnityAds.mainview != null) {
				if(!UnityAds.mainview.webview.isWebAppLoadComplete()) {
					UnityAds.mainview.webview.waitForWebAppLoadComplete();
				}
				if (UnityAds.mainview != null) {
					UnityAds.mainview.webview.setWebViewCurrentView(view, data);
					UnityAds.mainview.setViewState(UnityAdsMainView.UnityAdsMainViewState.WebView);
					//UnityAds.mainview.openAds(view, data);

					UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
					if (currentZone.noOfferScreen()) {
						UnityAdsDeviceLog.debug("SHOULD PLAY VIDEO");
						// TODO: FIX
						//playVideo();
					}

					// TODO: FIX
					//if (_adsListener != null)
					//		_adsListener.onShow();
				}
				else {
					UnityAdsDeviceLog.error("mainview null after open, closing");
					// TODO: FIX
					//close();
				}

				//new Thread(new Runnable() {
				//	public void run() {

				//		UnityAdsUtils.runOnUiThread(new Runnable() {
				//			public void run() {
							//}
				//		});

				//	}
				//}).start();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		UnityAdsDeviceLog.entered();   	
		super.onCreate(savedInstanceState);
		UnityAds.changeActivity(this);

		String view = UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START;

		if (view != null) {
			_currentView = view;
			setupViews();
			open(_currentView);
			setContentView(UnityAds.mainview);
		}
	}

	@Override
	public void onStart() {
		UnityAdsDeviceLog.entered();
		super.onStart();
	}

	@Override
	public void onRestart() {
		UnityAdsDeviceLog.entered();
		super.onRestart();
	}

	@Override
	public void onResume() {
		UnityAdsDeviceLog.entered();   	
		super.onResume();
	//	UnityAds.changeActivity(this);
   	//	UnityAds.checkMainview();
	}

	@Override
	public void onPause() {
		UnityAdsDeviceLog.entered();
		super.onPause();
	}

	@Override
	public void onStop() {
		UnityAdsDeviceLog.entered();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		UnityAdsDeviceLog.entered();   	
		super.onDestroy();
		UnityAds.handleFullscreenDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		return false;
	}


	@Override
	public void onMainViewAction(UnityAdsMainView.UnityAdsMainViewAction action) {

	}

	@Override
	public void onPlayVideo(JSONObject data) {

	}

	@Override
	public void onPauseVideo(JSONObject data) {

	}

	@Override
	public void onCloseAdsView(JSONObject data) {

	}

	@Override
	public void onWebAppLoadComplete(JSONObject data) {

	}

	@Override
	public void onWebAppInitComplete(JSONObject data) {

	}

	@Override
	public void onOrientationRequest(JSONObject data) {

	}

	@Override
	public void onOpenPlayStore(JSONObject data) {

	}
}
