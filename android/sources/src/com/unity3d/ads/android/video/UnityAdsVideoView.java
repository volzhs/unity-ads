package com.unity3d.ads.android.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.VideoView;

public class UnityAdsVideoView extends VideoView {

	private IUnityAdsVideoPlayerListener _listener;
	private boolean _videoCompleted = false;

	public UnityAdsVideoView(Context context) {
		super(context);
	}

	public UnityAdsVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UnityAdsVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onWindowVisibilityChanged(int visibility) {
		if(visibility == View.VISIBLE) {
			super.onWindowVisibilityChanged(visibility);
		} else if(!_videoCompleted) {
			_listener.onVideoHidden();
		}
	}

	public void setListener (IUnityAdsVideoPlayerListener listener) {
		_listener = listener;
	}

	public void setVideoCompleted (boolean completed) {
		_videoCompleted = completed;
	}
}