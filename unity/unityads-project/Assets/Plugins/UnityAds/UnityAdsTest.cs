using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAdsTest : MonoBehaviour {
	
	private bool _campaignsAvailable = false;

	void Awake() {
		UnityAds.setCampaignsAvailableDelegate(UnityAdsCampaignsAvailable);
		UnityAds.setCloseDelegate(UnityAdsClose);
		UnityAds.setOpenDelegate(UnityAdsOpen);
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

	public void UnityAdsOpen() {
		Debug.Log ("ADS: OPEN!");
	}
	
	public void UnityAdsClose() {
		Debug.Log ("ADS: CLOSE!");
	}

	public void UnityAdsVideoCompleted(string rewardItemKey, bool skipped) {
		Debug.Log ("ADS: VIDEO COMPLETE : " + rewardItemKey + " - " + skipped);
	}

	public void UnityAdsVideoStarted() {
		Debug.Log ("ADS: VIDEO STARTED!");
	}

	void OnGUI () {
		if (GUI.Button (new Rect (10,10,170,50), _campaignsAvailable ? "Open Unity Ads" : "Waiting...")) {
			if (_campaignsAvailable) {
				UnityAdsExternal.Log("Open Unity Ads -button clicked");
				UnityAds.show();
			}	
		}
	}
}
