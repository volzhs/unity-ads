using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class UnityAdsOpenButton : MonoBehaviour {
	private bool _canOpen = false;
	
	void Awake () {
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
		}
	}
}
