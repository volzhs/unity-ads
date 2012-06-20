package com.unity3d.ads.android.view;

import java.util.Timer;
import java.util.TimerTask;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.unity3d.ads.android.R;

// TODO: Keep screen on
// TODO: Pause playback on screen lock.
// TODO: Generally, force the user to actually watch the video.
public class UnityAdsVideoPlayView extends FrameLayout {

	private MediaPlayer.OnCompletionListener _listener;
	private Timer _videoPausedTimer = null;
	
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
		startVideo();
	}
	
	/* INTERNAL METHODS */
	
	private void startVideo () {
		((VideoView)findViewById(R.id.videoplayer)).start();
		
		if (_videoPausedTimer == null) {
			_videoPausedTimer = new Timer();
			_videoPausedTimer.scheduleAtFixedRate(new VideoPausedTask(), 500, 500);
		}
	}
	
	private void pauseVideo () {
		purgeVideoPausedTimer();
		((VideoView)findViewById(R.id.videoplayer)).pause();
	}
	
	private void purgeVideoPausedTimer () {
		if (_videoPausedTimer != null) {
			_videoPausedTimer.purge();
			_videoPausedTimer = null;
		}
	}

	private void createView () {
		Log.d(UnityAdsProperties.LOG_NAME, "Creating custom view");
		setBackgroundColor(0xBA000000);
		inflate(getContext(), R.layout.applifier_showvideo, this);
		((VideoView)findViewById(R.id.videoplayer)).setClickable(true);
		((VideoView)findViewById(R.id.videoplayer)).setOnCompletionListener(_listener);
		setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (!((VideoView)findViewById(R.id.videoplayer)).isPlaying()) {
					startVideo();
				}
			}
		});
		setOnFocusChangeListener(new View.OnFocusChangeListener() {			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					pauseVideo();
			}
		});
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
    
    /* INTERNAL CLASSES */
    
	private class VideoPausedTask extends TimerTask {
		@Override
		public void run () {
			PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
			
			if (!pm.isScreenOn())
				pauseVideo();
		}
	}
}
