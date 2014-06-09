using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAdsTest : MonoBehaviour {
	
	private bool _campaignsAvailable = false;

	void Awake() {
		UnityAds.setCampaignsAvailableDelegate(UnityAdsCampaignsAvailable);
		UnityAds.setHideDelegate(UnityAdsHide);
		UnityAds.setShowDelegate(UnityAdsShow);
		UnityAds.setCampaignsFetchFailedDelegate(UnityAdsCampaignsFetchFailed);
		UnityAds.setVideoCompletedDelegate(UnityAdsVideoCompleted);
		UnityAds.setVideoStartedDelegate(UnityAdsVideoStarted);
	}
	
	public void UnityAdsCampaignsAvailable() {
		Debug.Log ("ADS: CAMPAIGNS READY!");
		_campaignsAvailable = true;
	}

	public void UnityAdsCampaignsFetchFailed() {
		Debug.Log ("ADS: CAMPAIGNS FETCH FAILED!");
	}

	public void UnityAdsShow() {
		Debug.Log ("ADS: SHOW");
	}
	
	public void UnityAdsHide() {
		Debug.Log ("ADS: HIDE");
	}

	public void UnityAdsVideoCompleted(string rewardItemKey, bool skipped) {
		Debug.Log ("ADS: VIDEO COMPLETE : " + rewardItemKey + " - " + skipped);
	}

	public void UnityAdsVideoStarted() {
		Debug.Log ("ADS: VIDEO STARTED!");
	}

	void OnGUI () {
		if (GUI.Button (new Rect (10, 10, 150, 50), _campaignsAvailable ? "Open Zone 1" : "Waiting...")) {
			if (_campaignsAvailable) {
				UnityAdsExternal.Log("Open Zone 1 -button clicked");
				UnityAds.show("16-default");
			}	
		}
		
		if (GUI.Button (new Rect (10, 70, 150, 50), _campaignsAvailable ? "Open Zone 2" : "Waiting...")) {
			if(_campaignsAvailable) {
				UnityAdsExternal.Log ("Open Zone 2 -button clicked");
				UnityAds.show("16-default", "ship", new Dictionary<string, string>{
					{"openAnimated", "true"},
					{"noOfferScreen", "true"},
					{"sid", "testiSid"},
					{"muteVideoSounds", "true"},
					{"useDeviceOrientationForVideo", "true"}
				});
			}
		}
	}
}
