package com.unity3d.ads.android.view;

import android.view.View;
import android.view.ViewGroup;

import com.unity3d.ads.android.UnityAdsDeviceLog;

public class UnityAdsViewUtils {
	public static void removeViewFromParent(View view) {
		if (view != null) {
			try {
				((ViewGroup)view.getParent()).removeView(view);
			}
			catch (Exception e) {
				UnityAdsDeviceLog.info("Error while removing view from it's parent: " + e.getMessage());
			}
		}
	}
}
