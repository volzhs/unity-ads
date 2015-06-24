package com.unity3d.ads.android.video;

import android.media.MediaPlayer;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

public interface IUnityAdsVideoPlayerListener extends MediaPlayer.OnCompletionListener {
	void onEventPositionReached (UnityAdsVideoPosition position);
	void onVideoPlaybackStarted ();
	void onVideoPlaybackError ();
	void onVideoSkip ();
}
