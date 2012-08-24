package com.unity3d.ads.android.video;

import com.unity3d.ads.android.view.IUnityAdsViewListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.media.MediaPlayer.OnCompletionListener;


public interface IUnityAdsVideoPlayerListener extends IUnityAdsViewListener,
		OnCompletionListener {
	
	public void onEventPositionReached (UnityAdsVideoPosition position);
}
