package com.unity3d.ads.android.view;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.unity3d.ads.android.R;

// TODO: Keep screen on
// TODO: Pause playback on screen lock.
// TODO: Generally, force the user to actually watch the video.
public class UnityAdsVideoPlayView extends FrameLayout {

	private MediaPlayer.OnCompletionListener _listener;
	
	public UnityAdsVideoPlayView(Context context, MediaPlayer.OnCompletionListener listener) {
		super(context);
		_listener = listener;
		createView();		
	}

	public UnityAdsVideoPlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createView();
	}

	public UnityAdsVideoPlayView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		createView();
	}
	
	public void playVideo (String fileName) {
		((VideoView)findViewById(R.id.videoplayer)).setVideoPath(UnityAdsUtils.getCacheDirectory() + "/" + fileName);
		((VideoView)findViewById(R.id.videoplayer)).start();
	}

	private void createView () {
		Log.d(UnityAdsProperties.LOG_NAME, "Creating custom view");
		setBackgroundColor(0xBA000000);
		inflate(getContext(), R.layout.applifier_showvideo, this);
		((VideoView)findViewById(R.id.videoplayer)).setOnCompletionListener(_listener);
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
		    	((VideoView)findViewById(R.id.videoplayer)).stopPlayback();
		    	UnityAds.instance.closeAdsView(this, true);
		    	return true;
		}
    	
    	return false;
    }  
}
