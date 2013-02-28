using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAdsOpenButton : MonoBehaviour {
	private bool _canOpen = false;
	
	// Use this for initialization
	void Start () {
	}
	
	// Update is called once per frame
	void Update () {
	}
	
	void OnGUI () {
		if (UnityAds.getTestButtonVisibility()) {
			string buttonText = "Waiting...";
			
			if (!_canOpen && UnityAds.canShowAds() && UnityAds.canShow())
				_canOpen = true;
			
			if (_canOpen)
				buttonText = "Open Unity Ads";
			
			if (GUI.Button (new Rect (10,10,170,50), buttonText)) {
				if (_canOpen) {
					UnityAdsExternal.Log("Open Unity Ads -button clicked");
					UnityAds.show();
				}	
			}
			
			if (GUI.Button (new Rect (10,70,170,50), "Show rewardItemKeys")) {
				if (_canOpen) {
					List<string> keys = UnityAds.getRewardItemKeys();
					foreach (string key in keys) {
						UnityAdsExternal.Log("Reward key: " + key);
					}
				}	
			}
			
			if (GUI.Button (new Rect (10,130,170,50), "Reward item details")) {
				if (_canOpen) {
					UnityAdsExternal.Log("Trying to fetch details with key: " + UnityAds.getCurrentRewardItemKey());
					UnityAds.getRewardItemDetailsWithKey(UnityAds.getCurrentRewardItemKey());
				}	
			}
		}
	}
}
