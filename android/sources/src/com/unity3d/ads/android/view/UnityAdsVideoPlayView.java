package com.unity3d.ads.android.view;

import java.util.Timer;
import java.util.TimerTask;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;

import android.app.Activity;
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

// TODO: Generally, force the user to actually watch the video.
public class UnityAdsVideoPlayView extends FrameLayout {

	private MediaPlayer.OnCompletionListener _listener;
	private Timer _videoPausedTimer = null;
	private Activity _currentActivity = null;
	
	public UnityAdsVideoPlayView(Context context, MediaPlayer.OnCompletionListener listener, Activity activity) {
		super(context);
		_currentActivity = activity;
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
	
	public void setActivity (Activity activity) {
		_currentActivity = activity;
	}
	
	
	/* INTERNAL METHODS */
	
	private void startVideo () {
		if (_currentActivity != null) {
			_currentActivity.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					((VideoView)findViewById(R.id.videoplayer)).start();
					setKeepScreenOn(true);
				}
			});
		}
		
		if (_videoPausedTimer == null) {
			_videoPausedTimer = new Timer();
			_videoPausedTimer.scheduleAtFixedRate(new VideoPausedTask(), 500, 500);
		}
	}
	
	private void pauseVideo () {
		purgeVideoPausedTimer();
		
		if (_currentActivity != null) {
			_currentActivity.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					((VideoView)findViewById(R.id.videoplayer)).pause();
					setKeepScreenOn(false);
				}
			});
		}
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
				setKeepScreenOn(false);
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
