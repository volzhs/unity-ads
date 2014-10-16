package com.unity3d.ads.android.video;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsBufferingView;
import com.unity3d.ads.android.view.UnityAdsMuteVideoButton;
import com.unity3d.ads.android.view.UnityAdsMuteVideoButton.UnityAdsMuteVideoButtonState;
import com.unity3d.ads.android.webapp.UnityAdsInstrumentation;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;
import com.unity3d.ads.android.zone.UnityAdsZone;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsVideoPlayView extends RelativeLayout {
	private static final int FILL_PARENT = -1;

	private RelativeLayout _countDownText = null;
	private TextView _timeLeftInSecondsText = null;
	
	private RelativeLayout _skipText = null;
	private TextView _skipTextView = null;
	private long _skipTimeInSeconds = 0;
	private RelativeLayout _stagingLayout = null;
	private TextView _stagingText = null;
	
	private RelativeLayout _bufferingText = null;
	
	private long _bufferingStartedMillis = 0;
	private long _bufferingCompledtedMillis = 0;
	private long _videoStartedPlayingMillis = 0;
	
	private IUnityAdsVideoPlayerListener _listener;
	private Timer _videoPausedTimer = null;
	private VideoView _videoView = null;
	private String _videoFileName = null;
	private UnityAdsBufferingView _bufferingView = null;
	private UnityAdsVideoPausedView _pausedView = null;
	private UnityAdsMuteVideoButton _muteButton = null;
	private boolean _videoPlayheadPrepared = false;
	private Map<UnityAdsVideoPosition, Boolean> _sentPositionEvents = new HashMap<UnityAdsVideoPosition, Boolean>();
	private boolean _videoPlaybackStartedSent = false;
	private boolean _videoPlaybackErrors = false;
	private boolean _videoCompleted = false;
	private MediaPlayer _mediaPlayer = null;
	private boolean _muted = false;
	private float _volumeBeforeMute = 0.5f;
	
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
		UnityAdsDeviceLog.debug("Playing video from: " + _videoFileName);
		
		_videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				UnityAdsDeviceLog.error("For some reason the device failed to play the video (error: " + what + ", " + extra + "), a crash was prevented.");
				videoErrorOperations();
				return true;
			}
		});
		
		try {
			_videoView.setVideoPath(_videoFileName);
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("For some reason the device failed to play the video, a crash was prevented.");
			videoErrorOperations();
			return;
		}
		
		if (!_videoPlaybackErrors) {
			_timeLeftInSecondsText.setText("" + Math.round(Math.ceil(_videoView.getDuration() / 1000)));
			_bufferingStartedMillis = System.currentTimeMillis();
			startVideo();
		}
	}

	public void pauseVideo () {
		purgeVideoPausedTimer();
		
		if (_videoView != null && _videoView.isPlaying()) {
			UnityAdsUtils.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_videoView.pause();
					setKeepScreenOn(false);
					createAndAddPausedView();
				}
			});
		}		
	}

	public void hideVideo() {
		purgeVideoPausedTimer();
	}

	public void clearVideoPlayer  () {
		UnityAdsDeviceLog.entered();
		setKeepScreenOn(false);
		setOnClickListener(null);
		setOnFocusChangeListener(null);
		
		hideSkipText();
		hideTimeRemainingLabel();
		hideBufferingView();
		hideVideoPausedView();
		purgeVideoPausedTimer();
				
		_videoView.stopPlayback();
		_videoView.setOnCompletionListener(null);
		_videoView.setOnPreparedListener(null);
		_videoView.setOnErrorListener(null);
		
		removeAllViews();
		
		_skipText = null;
		_skipTextView = null;
		
		_bufferingText = null;
		_bufferingView = null;
				
		_countDownText = null;
		_timeLeftInSecondsText = null;

		_stagingText = null;
		_stagingLayout = null;
	}
	
	public long getBufferingDuration () {
		if (_bufferingCompledtedMillis == 0) {
			_bufferingCompledtedMillis = System.currentTimeMillis();
		}
		
		return _bufferingCompledtedMillis - _bufferingStartedMillis;
	}
	
	public int getSecondsUntilBackButtonAllowed () {
		int timeUntilBackButton = 0;
		
		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.disableBackButtonForSeconds() > 0 && _videoStartedPlayingMillis > 0) {
			timeUntilBackButton = Math.round((currentZone.disableBackButtonForSeconds() * 1000) - (System.currentTimeMillis() - _videoStartedPlayingMillis));
			if (timeUntilBackButton < 0)
				timeUntilBackButton = 0;
		}
		else if (currentZone.allowVideoSkipInSeconds() > 0 && _videoStartedPlayingMillis <= 0){
			return 1;
		}
		
		return timeUntilBackButton;
	}
	
	
	/* INTERNAL METHODS */
	private void storeVolume () {
		AudioManager am = ((AudioManager)((Context)UnityAdsProperties.getCurrentActivity()).getSystemService(Context.AUDIO_SERVICE));
		int curVol = 0;
		int maxVol = 0;
		
		if (am != null) {
			curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			float parts = 1f / (float)maxVol;
			_volumeBeforeMute = parts * (float)curVol;
			UnityAdsDeviceLog.debug("Storing volume: " + curVol + ", " + maxVol + ", " + parts + ", " + _volumeBeforeMute);
		}
	}
	
	private void videoErrorOperations () {
		_videoPlaybackErrors = true;
		purgeVideoPausedTimer();
		if (_listener != null)
			_listener.onVideoPlaybackError();
		
		UnityAdsInstrumentation.gaInstrumentationVideoError(UnityAdsProperties.SELECTED_CAMPAIGN, null);		
	}
	
	
	private void startVideo () {
		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_videoView.start();
				setKeepScreenOn(true);
			}
		});
		
		if (_videoPausedTimer == null) {
			_videoPausedTimer = new Timer();
			_videoPausedTimer.scheduleAtFixedRate(new VideoStateChecker(), 500, 500);
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
		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.muteVideoSounds()) {
			_muted = true;
		}
		
		UnityAdsDeviceLog.debug("Creating custom view");
				
		setBackgroundColor(0xFF000000);
		
		_videoCompleted = false;
		_videoView = new VideoView(getContext()) {
			@Override
			public void onWindowVisibilityChanged(int visibility) {
				if(visibility == View.VISIBLE) {
					super.onWindowVisibilityChanged(visibility);
				} else if(!_videoCompleted) {
						_listener.onVideoHidden();
				}
			}
		};
		_videoView.setId(3001);
		RelativeLayout.LayoutParams videoLayoutParams = new RelativeLayout.LayoutParams(FILL_PARENT, FILL_PARENT);
		videoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		_videoView.setLayoutParams(videoLayoutParams);		
		addView(_videoView, videoLayoutParams);
		_videoView.setClickable(true);
		_videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				_videoCompleted = true;
				_listener.onCompletion(mp);
			}
		});
		_videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {			
			@Override
			public void onPrepared(MediaPlayer mp) {
				UnityAdsDeviceLog.entered();
				_mediaPlayer = mp;
				
				if (_muted) {
					storeVolume();
					_mediaPlayer.setVolume(0f, 0f);
				}
				
				_videoPlayheadPrepared = true;
			}
		});
		
		_bufferingText = new RelativeLayout(getContext());
		_bufferingText.setId(3100);
		RelativeLayout.LayoutParams bufferingTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		bufferingTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		bufferingTextParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		bufferingTextParams.topMargin = 3;
		bufferingTextParams.rightMargin = 3;
		_bufferingText.setLayoutParams(bufferingTextParams);
		
		TextView bufferingTextView = new TextView(getContext());
		bufferingTextView.setTextColor(Color.WHITE);
		bufferingTextView.setText("Buffering...");
		bufferingTextView.setId(3103);
		
		_bufferingText.addView(bufferingTextView);
		addView(_bufferingText);
		
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

		if(UnityAdsProperties.UNITY_DEVELOPER_INTERNAL_TEST) { 
			_stagingLayout = new RelativeLayout(getContext());
			RelativeLayout.LayoutParams stagingParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			stagingParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
			stagingParams.addRule(RelativeLayout.CENTER_VERTICAL);
			_stagingLayout.setLayoutParams(stagingParams);

			_stagingText = new TextView(getContext());
			_stagingText.setTextColor(Color.RED);
			_stagingText.setBackgroundColor(Color.BLACK);
			_stagingText.setText("INTERNAL UNITY TEST BUILD\nDO NOT USE IN PRODUCTION");

			_stagingLayout.addView(_stagingText);
			addView(_stagingLayout);
		}

		if (hasSkipDuration()) {
			_skipTimeInSeconds = getSkipDuration();
			createAndAddSkipText();
		}
			
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
		
		
		createAndAddMuteButton();
	}
	
	private void createAndAddMuteButton () {
		RelativeLayout.LayoutParams muteButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		muteButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		muteButtonParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				
		_muteButton = new UnityAdsMuteVideoButton(getContext());
		_muteButton.setLayoutParams(muteButtonParams);
		
		if (_muted) {
			_muteButton.setState(UnityAdsMuteVideoButtonState.Muted);
		}
		
		_muteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (_videoPlayheadPrepared && _videoPlaybackStartedSent) {
					if (_muted) {
						_muted = false;
						_muteButton.setState(UnityAdsMuteVideoButtonState.UnMuted);
						_mediaPlayer.setVolume(_volumeBeforeMute, _volumeBeforeMute);
					}
					else {
						_muted = true;
						_muteButton.setState(UnityAdsMuteVideoButtonState.Muted);
						storeVolume();
						_mediaPlayer.setVolume(0f, 0f);
					}
				}
			}
		});
		
		addView(_muteButton);
	}
	
	private void createAndAddPausedView () {
		if (_pausedView == null)
			_pausedView = new UnityAdsVideoPausedView(getContext());
				
		if (_pausedView != null && _pausedView.getParent() == null) {
			RelativeLayout.LayoutParams pausedViewParams = new RelativeLayout.LayoutParams(FILL_PARENT, FILL_PARENT);
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
	
	private boolean hasSkipDuration () {
		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		return currentZone.allowVideoSkipInSeconds() > 0;
	}
	
	private long getSkipDuration () {
		if (hasSkipDuration()) {
			UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
			return currentZone.allowVideoSkipInSeconds();
		}	
		
		return 0;
	}
	
	private void createAndAddSkipText () {	
		_skipText = new RelativeLayout(getContext());
		_skipText.setId(3010);
		RelativeLayout.LayoutParams skipTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		skipTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		skipTextParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		skipTextParams.topMargin = 5;
		skipTextParams.leftMargin = 5;
		_skipText.setLayoutParams(skipTextParams);
		
		_skipText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				_listener.onVideoSkip();
			}
		});
		
		_skipTextView = new TextView(getContext());
		_skipTextView.setTextColor(Color.WHITE);
		_skipTextView.setText("You can skip this video in " + _skipTimeInSeconds + " seconds");
		_skipTextView.setId(10010);
		
		_skipText.addView(_skipTextView);
		
		addView(_skipText);	
	}
	
	private void enableSkippingFromSkipText () {
		if (_skipText != null) {
			_skipText.setVisibility(VISIBLE);
			_skipText.setClickable(true);
			_skipText.setBackgroundColor(0x01FFFFFF);
			_skipText.setFocusable(true);
			_skipTextView.setText("Skip video");
			_skipText.requestFocus();
			
			// Get touch events for skip button from a larger area
			Rect skipHitArea = new Rect();
			_skipText.getHitRect(skipHitArea);
			int textHeight = skipHitArea.bottom - skipHitArea.top;
			skipHitArea.bottom += textHeight * 2;
			skipHitArea.right += textHeight * 2;
			TouchDelegate td = new TouchDelegate(skipHitArea, _skipText);
			((View)_skipText.getParent()).setTouchDelegate(td);
		}
	}
	
	private void disableSkippingFromSkipText() {
		if(_skipText != null) {
			_skipText.setClickable(false);
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
	
	private void hideSkipText () {
		if (_skipText != null && _skipText.getParent() != null) {
			disableSkippingFromSkipText();
			_skipText.setVisibility(INVISIBLE);
		}
	}
	
	private void setBufferingTextVisibility(final int visibility, final boolean hasSkip, final boolean canSkip) {
		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(_bufferingText != null) {
					_bufferingText.setVisibility(visibility);
				}
				if(visibility == VISIBLE) {
					if(_skipText == null) {
						createAndAddSkipText();
					}
					enableSkippingFromSkipText();
				} else {
					if(hasSkip) {
						if(canSkip) {
							enableSkippingFromSkipText();
						} else {
							disableSkippingFromSkipText();
						}
					} else {
						hideSkipText();
					}
				}
			}
		});
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	long bufferingDuration = 0;
    	Map<String, Object> values = null;
    	
    	switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				UnityAdsDeviceLog.entered();
				
				UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
				long allowBackButtonSkip = currentZone.disableBackButtonForSeconds();
				if (allowBackButtonSkip == 0 || (allowBackButtonSkip > 0 && getSecondsUntilBackButtonAllowed() == 0)) {
					clearVideoPlayer();
					
					bufferingDuration = getBufferingDuration();
					values = new HashMap<String, Object>();
					values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_BUFFERINGDURATION_KEY, bufferingDuration);
					values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VALUE_KEY, UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_VIDEOABORT_BACK);
					UnityAdsInstrumentation.gaInstrumentationVideoAbort(UnityAdsProperties.SELECTED_CAMPAIGN, values);				
				}
				
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
		private Float _skipTimeLeft = 0.01f; 
		private int _duration = 1;
		private boolean _playHeadHasMoved = false;	
		private boolean _videoHasStalled = false;
		
		@Override
		public void run () {
			if (_videoView == null || _timeLeftInSecondsText == null) {
				purgeVideoPausedTimer();
				return;
			}

			PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);			
			if (!pm.isScreenOn()) {
				pauseVideo();
			}

			_oldPos = _curPos;

			try {
				_curPos = Float.valueOf(_videoView.getCurrentPosition());
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Could not get videoView currentPosition");
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
				UnityAdsDeviceLog.error("Could not get videoView duration");
				durationSuccess = false;
			}
			
			if (durationSuccess)
				_duration = duration;
			
			position = _curPos / _duration;
			
			if (_curPos > _oldPos) {
				_playHeadHasMoved = true;
				_videoHasStalled = false;
				setBufferingTextVisibility(INVISIBLE, hasSkipDuration(), _skipTimeLeft <= 0f);
			} else { 
				_videoHasStalled = true;
				setBufferingTextVisibility(VISIBLE, true, true);
			}
			
			UnityAdsUtils.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (_timeLeftInSecondsText != null) {
						_timeLeftInSecondsText.setText("" + Math.round(Math.ceil((_duration - _curPos) / 1000)));
					}
				}
			});
			
			if (hasSkipDuration() && _skipTimeInSeconds > 0 && _skipTimeLeft > 0f && (_duration / 1000) > _skipTimeInSeconds) {
				_skipTimeLeft = (_skipTimeInSeconds * 1000) - _curPos;
				
				if (_skipTimeLeft < 0)
					_skipTimeLeft = 0f;
				
				if (_skipTimeLeft == 0) {
					UnityAdsUtils.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							enableSkippingFromSkipText();
						}
					});
				}
				else {
					UnityAdsUtils.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (_skipTextView != null && !_videoHasStalled) {
								_skipText.setVisibility(VISIBLE);
								_skipTextView.setText("You can skip this video in " + Math.round(Math.ceil(((_skipTimeInSeconds * 1000) - _curPos) / 1000)) + " seconds");
							}
						}
					});
				}
			}
			else if (_playHeadHasMoved && (_duration / 1000) <= _skipTimeInSeconds) {
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						hideSkipText();
					}
				});
			}
			
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
				UnityAdsDeviceLog.error("Could not get videoView buffering percentage");
			}
			
			if (!_playHeadHasMoved && _bufferingStartedMillis > 0 &&
				(System.currentTimeMillis() - _bufferingStartedMillis) > (UnityAdsProperties.MAX_BUFFERING_WAIT_SECONDS * 1000)) {
				this.cancel();
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						UnityAdsDeviceLog.error("Buffering taking too long.. cancelling video play");
						videoErrorOperations();
					}
				});
			}
						
			if (_videoView != null && bufferPercentage < 15 && _videoView.getParent() == null) {
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						createAndAddBufferingView();
					}
				});				
			}
			
			if (_videoPlayheadPrepared && _playHeadHasMoved) {
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						hideBufferingView();
						if (!_videoPlaybackStartedSent) {
							if (_listener != null) {
								_videoPlaybackStartedSent = true;
								_listener.onVideoPlaybackStarted();
								_bufferingCompledtedMillis = System.currentTimeMillis();
								_videoStartedPlayingMillis = System.currentTimeMillis();
								long bufferingDuration = _bufferingCompledtedMillis - _bufferingStartedMillis;
								Map<String, Object> values = new HashMap<String, Object>();
								values.put(UnityAdsConstants.UNITY_ADS_GOOGLE_ANALYTICS_EVENT_BUFFERINGDURATION_KEY, bufferingDuration);
								UnityAdsInstrumentation.gaInstrumentationVideoPlay(UnityAdsProperties.SELECTED_CAMPAIGN, values);
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
