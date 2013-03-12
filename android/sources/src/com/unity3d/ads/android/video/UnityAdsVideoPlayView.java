package com.unity3d.ads.android.video;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsBufferingView;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class UnityAdsVideoPlayView extends RelativeLayout {

	private IUnityAdsVideoPlayerListener _listener;
	private Timer _videoPausedTimer = null;
	private VideoView _videoView = null;
	private String _videoFileName = null;
	private UnityAdsBufferingView _bufferingView = null;
	private UnityAdsVideoPausedView _pausedView = null;
	private boolean _videoPlayheadPrepared = false;
	private Map<UnityAdsVideoPosition, Boolean> _sentPositionEvents = new HashMap<UnityAdsVideoPosition, Boolean>();
	private RelativeLayout _countDownText = null;
	private TextView _timeLeftInSecondsText = null;
	private boolean _videoPlaybackStartedSent = false;
	private boolean _videoPlaybackErrors = false;
	
	public UnityAdsVideoPlayView(Context context, IUnityAdsVideoPlayerListener listener) {
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
		if (fileName == null) return;
		
		_videoPlayheadPrepared = false;
		_videoFileName = fileName;
		UnityAdsUtils.Log("Playing video from: " + _videoFileName, this);
		
		_videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				UnityAdsUtils.Log("For some reason the device failed to play the video (error: " + what + ", " + extra + "), a crash was prevented.", this);
				_videoPlaybackErrors = true;
				purgeVideoPausedTimer();
				if (_listener != null)
					_listener.onVideoPlaybackError();
				return true;
			}
		});
		
		try {
			_videoView.setVideoPath(_videoFileName);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("For some reason the device failed to play the video, a crash was prevented.", this);
			_videoPlaybackErrors = true;
			purgeVideoPausedTimer();
			if (_listener != null)
				_listener.onVideoPlaybackError();
			return;
		}
		
		if (!_videoPlaybackErrors) {
			_timeLeftInSecondsText.setText("" + Math.round(Math.ceil(_videoView.getDuration() / 1000)));
			startVideo();
		}
	}

	public void pauseVideo () {
		purgeVideoPausedTimer();
		
		if (UnityAdsProperties.CURRENT_ACTIVITY != null && _videoView != null && _videoView.isPlaying()) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					_videoView.pause();
					setKeepScreenOn(false);
					createAndAddPausedView();
				}
			});
		}		
	}
	
	public void clearVideoPlayer  () {
		UnityAdsUtils.Log("clearVideoPlayer", this);
		setKeepScreenOn(false);
		setOnClickListener(null);
		setOnFocusChangeListener(null);
		
		hideTimeRemainingLabel();
		hideBufferingView();
		hideVideoPausedView();
		purgeVideoPausedTimer();
		
		_videoView.stopPlayback();
		_videoView.setOnCompletionListener(null);
		_videoView.setOnPreparedListener(null);
		_videoView.setOnErrorListener(null);
	}
	
	
	/* INTERNAL METHODS */
	
	private void startVideo () {
		if (UnityAdsProperties.CURRENT_ACTIVITY != null) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					_videoView.start();
					setKeepScreenOn(true);
				}
			});
		}
		
		if (_videoPausedTimer == null) {
			_videoPausedTimer = new Timer();
			_videoPausedTimer.scheduleAtFixedRate(new VideoStateChecker(), 10, 60);
		}
	}
	
	private void purgeVideoPausedTimer () {
		if (_videoPausedTimer != null) {
			_videoPausedTimer.cancel();
			_videoPausedTimer.purge();
			_videoPausedTimer = null;
		}
	}

	private void createView () {
		UnityAdsUtils.Log("Creating custom view", this);
		setBackgroundColor(0xFF000000);
		_videoView = new VideoView(getContext());
		_videoView.setId(3001);
		RelativeLayout.LayoutParams videoLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		videoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		_videoView.setLayoutParams(videoLayoutParams);		
		addView(_videoView, videoLayoutParams);
		_videoView.setClickable(true);
		_videoView.setOnCompletionListener(_listener);
		_videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {			
			@Override
			public void onPrepared(MediaPlayer mp) {
				UnityAdsUtils.Log("onPrepared", this);
				_videoPlayheadPrepared = true;
			}
		});
		
		_countDownText = new RelativeLayout(getContext());
		_countDownText.setId(3002);
		RelativeLayout.LayoutParams countDownParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		countDownParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		countDownParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		countDownParams.bottomMargin = 3;
		countDownParams.rightMargin = 3;
		_countDownText.setLayoutParams(countDownParams);
		
		TextView tv = new TextView(getContext());
		tv.setTextColor(Color.WHITE);
		tv.setText("This video ends in ");
		tv.setId(10001);
		
		_timeLeftInSecondsText = new TextView(getContext());
		_timeLeftInSecondsText.setTextColor(Color.WHITE);
		_timeLeftInSecondsText.setText("00");
		_timeLeftInSecondsText.setId(10002);
		RelativeLayout.LayoutParams tv2params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		tv2params.addRule(RelativeLayout.RIGHT_OF, 10001);
		tv2params.leftMargin = 1;
		_timeLeftInSecondsText.setLayoutParams(tv2params);
		
		TextView tv3 = new TextView(getContext());
		tv3.setTextColor(Color.WHITE);
		tv3.setText("seconds.");
		RelativeLayout.LayoutParams tv3params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		tv3params.addRule(RelativeLayout.RIGHT_OF, 10002);
		tv3params.leftMargin = 4;
		tv3.setLayoutParams(tv3params);
		
		_countDownText.addView(tv);
		_countDownText.addView(_timeLeftInSecondsText);
		_countDownText.addView(tv3);
		
		addView(_countDownText);
		
		setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (!_videoView.isPlaying()) {
					hideVideoPausedView();
					startVideo();
				}
			}
		});
		setOnFocusChangeListener(new View.OnFocusChangeListener() {			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					pauseVideo();
				}
			}
		});
	}
	
	private void createAndAddPausedView () {
		if (_pausedView == null)
			_pausedView = new UnityAdsVideoPausedView(getContext());
				
		if (_pausedView != null && _pausedView.getParent() == null) {
			RelativeLayout.LayoutParams pausedViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			pausedViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			addView(_pausedView, pausedViewParams);		
		}
	}
	
	private void createAndAddBufferingView () {
		if (_bufferingView == null) {
    		_bufferingView = new UnityAdsBufferingView(getContext());
    	}
    	
    	if (_bufferingView != null && _bufferingView.getParent() == null) {
    		RelativeLayout.LayoutParams bufferingLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    		bufferingLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    		addView(_bufferingView, bufferingLayoutParams);
    	}  		
	}
	
	private void hideTimeRemainingLabel () {
		if (_countDownText != null && _countDownText.getParent() != null) {
			_countDownText.removeAllViews();
			removeView(_countDownText);			
		}
	}
	
	private void hideBufferingView () {
		if (_bufferingView != null && _bufferingView.getParent() != null)
			removeView(_bufferingView);
	}
	
	private void hideVideoPausedView () {
		if (_pausedView != null && _pausedView.getParent() != null)
			removeView(_pausedView);
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				UnityAdsUtils.Log("onKeyDown", this);
				clearVideoPlayer();
				
				if (_listener != null)
					_listener.onBackButtonClicked(this);
				
		    	return true;
		}
    	
    	return false;
    } 
    
    @Override
    protected void onAttachedToWindow() {
    	super.onAttachedToWindow();    	
  		hideVideoPausedView();
    }
    
    /* INTERNAL CLASSES */
    
	private class VideoStateChecker extends TimerTask {
		private Float _curPos = 0f;
		private Float _oldPos = 0f;
		private int _duration = 1;
		private boolean _playHeadHasMoved = false;
		
		
		@Override
		public void run () {
			if (_videoView == null || _timeLeftInSecondsText == null)
				this.cancel();
			
			PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);			
			if (!pm.isScreenOn()) {
				pauseVideo();
			}
			
			_oldPos = _curPos;
			
			try {
				_curPos = new Float(_videoView.getCurrentPosition());
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get videoView currentPosition", this);
				if (_oldPos > 0)
					_curPos = _oldPos;
				else
					_curPos = 0.01f;
			}
			
			Float position = 0f;
			int duration = 1;
			Boolean durationSuccess = true;
			
			try {
				duration = _videoView.getDuration();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get videoView duration", this);
				durationSuccess = false;
			}
			
			if (durationSuccess)
				_duration = duration;
			
			position = _curPos / _duration;
			
			if (_curPos > _oldPos) 
				_playHeadHasMoved = true;
			
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					_timeLeftInSecondsText.setText("" + Math.round(Math.ceil((_duration - _curPos) / 1000)));
				}
			});
			
			if (position > 0.25 && !_sentPositionEvents.containsKey(UnityAdsVideoPosition.FirstQuartile)) {
				_listener.onEventPositionReached(UnityAdsVideoPosition.FirstQuartile);
				_sentPositionEvents.put(UnityAdsVideoPosition.FirstQuartile, true);
			}
			if (position > 0.5 && !_sentPositionEvents.containsKey(UnityAdsVideoPosition.MidPoint)) {
				_listener.onEventPositionReached(UnityAdsVideoPosition.MidPoint);
				_sentPositionEvents.put(UnityAdsVideoPosition.MidPoint, true);
			}
			if (position > 0.75 && !_sentPositionEvents.containsKey(UnityAdsVideoPosition.ThirdQuartile)) {
				_listener.onEventPositionReached(UnityAdsVideoPosition.ThirdQuartile);
				_sentPositionEvents.put(UnityAdsVideoPosition.ThirdQuartile, true);
			}
			
			int bufferPercentage = 0;
			try {
				bufferPercentage = _videoView.getBufferPercentage();
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get videoView buffering percentage", this);
			}
			
			if (UnityAdsProperties.CURRENT_ACTIVITY != null && _videoView != null && bufferPercentage < 15 && _videoView.getParent() == null) {				
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {					
					@Override
					public void run() {
						createAndAddBufferingView();
					}
				});				
			}
			
			if (UnityAdsProperties.CURRENT_ACTIVITY != null && _videoPlayheadPrepared && _playHeadHasMoved) {
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						hideBufferingView();
						if (!_videoPlaybackStartedSent) {
							if (_listener != null) {
								_videoPlaybackStartedSent = true;
								UnityAdsUtils.Log("onVideoPlaybackStarted sent to listener", this);
								_listener.onVideoPlaybackStarted();
							}
							
							if (!_sentPositionEvents.containsKey(UnityAdsVideoPosition.Start)) {
								_sentPositionEvents.put(UnityAdsVideoPosition.Start, true);
								_listener.onEventPositionReached(UnityAdsVideoPosition.Start);
							}
						}
					}
				});
			}
		}
	}
}
