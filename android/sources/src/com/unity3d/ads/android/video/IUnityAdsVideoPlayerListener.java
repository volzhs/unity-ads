package com.unity3d.ads.android.video;

import android.media.MediaPlayer.OnCompletionListener;

import com.unity3d.ads.android.view.IUnityAdsViewListener;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;


public interface IUnityAdsVideoPlayerListener extends IUnityAdsViewListener,
		OnCompletionListener {
	
	public void onEventPositionReached (UnityAdsVideoPosition position);
	public void onVideoPlaybackStarted ();
	public void onVideoPlaybackError ();
	public void onVideoSkip ();
	public void onVideoHidden ();
}
