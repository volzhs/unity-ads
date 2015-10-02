package com.unity3d.ads.android.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unity3d.ads.android.R;
import com.unity3d.ads.android.UnityAdsDeviceLog;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsMuteVideoButton;
import com.unity3d.ads.android.view.UnityAdsViewUtils;
import com.unity3d.ads.android.webapp.UnityAdsWebData;
import com.unity3d.ads.android.webapp.UnityAdsWebData.UnityAdsVideoPosition;
import com.unity3d.ads.android.zone.UnityAdsZone;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class UnityAdsVideoPlayView extends RelativeLayout {

	private TextView _timeLeftInSecondsText = null;

	private long _bufferingStartedMillis = 0;
	private long _videoStartedPlayingMillis = 0;
	private float _volumeBeforeMute = 0.5f;

	private final Map<UnityAdsVideoPosition, Boolean> _sentPositionEvents = new HashMap<>();
	private UnityAdsVideoPausedView _pausedView = null;
	private UnityAdsMuteVideoButton _muteButton = null;
	private LinearLayout _countDownText = null;

	private IUnityAdsVideoPlayerListener _listener;
	private Timer _videoPausedTimer = null;
	private MediaPlayer _mediaPlayer = null;

	private boolean _videoPlaybackErrors = false;
	private boolean _muted = false;
	private boolean _videoPlaybackStartedSent = false;
	private boolean _videoPlayheadPrepared = false;

	private RelativeLayout _layout = null;
	private UnityAdsVideoView _videoView = null;
	private TextView _skipTextView = null;
	private TextView _bufferingText = null;

	public UnityAdsVideoPlayView(Context context) {
		super(context);
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

	public void setListener (IUnityAdsVideoPlayerListener listener) {
		_listener = listener;
	}

	public void playVideo (String fileName, boolean cached) {
		if (fileName == null) return;
		
		_videoPlayheadPrepared = false;
		UnityAdsDeviceLog.debug("Playing video from: " + fileName);

		_videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				UnityAdsDeviceLog.error("For some reason the device failed to play the video (error: " + what + ", " + extra + "), a crash was prevented.");
				videoPlaybackFailed();
				return true;
			}
		});

		// Set the video file to readable, in some caching cases the MediaPlayer cannot play the file unless it's set to readable for all
		if(cached) {
			File f = new File(fileName);
			boolean result = f.setReadable(true, false);
			if (!result) UnityAdsDeviceLog.debug("COULD NOT SET FILE READABLE");
		}

		try {
			_videoView.setVideoPath(fileName);
		}
		catch (Exception e) {
			UnityAdsDeviceLog.error("For some reason the device failed to play the video, a crash was prevented.");
			videoPlaybackFailed();
			return;
		}

		if (!_videoPlaybackErrors) {
			updateTimeLeftText();
			_bufferingStartedMillis = System.currentTimeMillis();
			startVideo();
		}
	}

	public int getCurrentPosition () {
		if (_videoView != null) return _videoView.getCurrentPosition();
		else return 0;
	}

	public void seekTo (int time) {
		if (_videoView != null) _videoView.seekTo(time);
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

	public boolean isPlaying () {
		return _videoView != null && _videoView.isPlaying();
	}

	public void clearVideoPlayer  () {
		UnityAdsDeviceLog.entered();
		setKeepScreenOn(false);
		setOnClickListener(null);
		setOnFocusChangeListener(null);
		
		hideSkipText();
		hideTimeRemainingLabel();
		hideVideoPausedView();
		purgeVideoPausedTimer();
		_videoView.stopPlayback();
		_videoView.setOnCompletionListener(null);
		_videoView.setOnPreparedListener(null);
		_videoView.setOnErrorListener(null);
		
		removeAllViews();
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

	private void storeVolume () {
		AudioManager am = ((AudioManager)UnityAdsProperties.APPLICATION_CONTEXT.getSystemService(Context.AUDIO_SERVICE));
		int curVol;
		int maxVol;
		
		if (am != null) {
			curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			float parts = 1f / (float)maxVol;
			_volumeBeforeMute = parts * (float)curVol;
			UnityAdsDeviceLog.debug("Storing volume: " + curVol + ", " + maxVol + ", " + parts + ", " + _volumeBeforeMute);
		}
	}

	private void videoPlaybackFailed() {
		_videoPlaybackErrors = true;
		purgeVideoPausedTimer();
		if (_listener != null)
			_listener.onVideoPlaybackError();
	}

	private void startVideo() {
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
		LayoutInflater inflater = LayoutInflater.from(getContext());
		_layout = (RelativeLayout)inflater.inflate(R.layout.unityads_view_video_play, this);

		UnityAdsZone currentZone = UnityAdsWebData.getZoneManager().getCurrentZone();
		if (currentZone.muteVideoSounds()) {
			_muted = true;
		}

		_videoView = (UnityAdsVideoView)_layout.findViewById(R.id.unityAdsVideoView);
		_videoView.setClickable(true);
		_videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
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

		_bufferingText = (TextView)_layout.findViewById(R.id.unityAdsVideoBufferingText);
		_countDownText = (LinearLayout)_layout.findViewById(R.id.unityAdsVideoCountDown);
		_timeLeftInSecondsText = (TextView)_layout.findViewById(R.id.unityAdsVideoTimeLeftText);
		_timeLeftInSecondsText.setText(R.string.unityads_default_video_length_text);
		_skipTextView = (TextView)_layout.findViewById(R.id.unityAdsVideoSkipText);
		_muteButton = new UnityAdsMuteVideoButton(getContext());
		_muteButton.setLayout((RelativeLayout) _layout.findViewById(R.id.unityAdsAudioToggleView));

		if (_muted) {
			_muteButton.setState(UnityAdsMuteVideoButton.UnityAdsMuteVideoButtonState.Muted);
		}

		_layout.findViewById(R.id.unityAdsAudioToggleView).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (_videoPlayheadPrepared && _videoPlaybackStartedSent) {
					if (_muted) {
						_muted = false;
						_muteButton.setState(UnityAdsMuteVideoButton.UnityAdsMuteVideoButtonState.UnMuted);
						_mediaPlayer.setVolume(_volumeBeforeMute, _volumeBeforeMute);
					}
					else {
						_muted = true;
						_muteButton.setState(UnityAdsMuteVideoButton.UnityAdsMuteVideoButtonState.Muted);
						storeVolume();
						_mediaPlayer.setVolume(0f, 0f);
					}
				}
			}
		});

		if(UnityAdsProperties.UNITY_DEVELOPER_INTERNAL_TEST) {
			RelativeLayout stagingLayout = new RelativeLayout(getContext());
			RelativeLayout.LayoutParams stagingParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			stagingParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
			stagingParams.addRule(RelativeLayout.CENTER_VERTICAL);
			stagingLayout.setLayoutParams(stagingParams);

			TextView stagingText = new TextView(getContext());
			stagingText.setTextColor(Color.RED);
			stagingText.setBackgroundColor(Color.BLACK);
			stagingText.setText("INTERNAL UNITY TEST BUILD\nDO NOT USE IN PRODUCTION");

			stagingLayout.addView(stagingText);
			addView(stagingLayout);
		}

		if (hasSkipDuration()) {
			updateSkipText(getSkipDuration());
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
	}

	private void enableSkippingFromSkipText () {
		if (_skipTextView == null) {
			_skipTextView = (TextView) _layout.findViewById(R.id.unityAdsVideoSkipText);
		}

		if (_skipTextView != null) {
			_skipTextView.setText(R.string.unityads_skip_video_text);
		}

		if (_skipTextView != null) {
			_skipTextView.setClickable(true);
			_skipTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					_listener.onVideoSkip();
				}
			});
		}
	}

	private void updateSkipText (long skipTimeSeconds) {
		if (_skipTextView == null) {
			_skipTextView = (TextView) _layout.findViewById(R.id.unityAdsVideoSkipText);
		}

		_skipTextView.setText(getResources().getString(R.string.unityads_skip_video_prefix) + " " + skipTimeSeconds + " " + getResources().getString(R.string.unityads_skip_video_suffix));
	}

	private void updateTimeLeftText () {
		if (_timeLeftInSecondsText == null) {
			_timeLeftInSecondsText = (TextView)_layout.findViewById(R.id.unityAdsVideoTimeLeftText);
		}

		_timeLeftInSecondsText.setText("" + Math.round(Math.ceil((_videoView.getDuration() - _videoView.getCurrentPosition()) / 1000)));
	}

	private void createAndAddPausedView () {
		if (_pausedView == null)
			_pausedView = new UnityAdsVideoPausedView(getContext());
				
		if (_pausedView.getParent() == null) {
			RelativeLayout.LayoutParams pausedViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			pausedViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			addView(_pausedView, pausedViewParams);		
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

	private void disableSkippingFromSkipText() {
		if(_skipTextView != null) {
			_skipTextView.setClickable(false);
		}
	}

	private void hideTimeRemainingLabel () {
		UnityAdsViewUtils.removeViewFromParent(_countDownText);
	}

	private void hideVideoPausedView () {
		if (_pausedView != null && _pausedView.getParent() != null)
			removeView(_pausedView);
	}

	private void hideSkipText () {
		if (_skipTextView != null && _skipTextView.getParent() != null) {
			disableSkippingFromSkipText();
			_skipTextView.setVisibility(INVISIBLE);
		}
	}

	private void setBufferingTextVisibility(final int visibility, final boolean hasSkip, final boolean canSkip) {
		UnityAdsUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (_bufferingText != null) {
					_bufferingText.setVisibility(visibility);
				}
				if (visibility == VISIBLE) {
					if (_skipTextView == null) {
						updateSkipText(getSkipDuration());
					}
					enableSkippingFromSkipText();
				} else {
					if (hasSkip) {
						if (canSkip) {
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

			_oldPos = _curPos;

			try {
				_curPos = (float)_videoView.getCurrentPosition();
			}
			catch (Exception e) {
				UnityAdsDeviceLog.error("Could not get videoView currentPosition");
				if (_oldPos > 0)
					_curPos = _oldPos;
				else
					_curPos = 0.01f;
			}
			
			Float position;
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
					updateTimeLeftText();
				}
			});
			
			if (hasSkipDuration() && getSkipDuration() > 0 && _skipTimeLeft > 0f && (_duration / 1000) > getSkipDuration()) {
				_skipTimeLeft = (getSkipDuration() * 1000) - _curPos;
				
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
								_skipTextView.setVisibility(VISIBLE);
								updateSkipText(Math.round(Math.ceil(((getSkipDuration() * 1000) - _curPos) / 1000)));
							}
						}
					});
				}
			}
			else if (_playHeadHasMoved && (_duration / 1000) <= getSkipDuration()) {
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
			
			if (!_playHeadHasMoved && _bufferingStartedMillis > 0 &&
				(System.currentTimeMillis() - _bufferingStartedMillis) > (UnityAdsProperties.MAX_BUFFERING_WAIT_SECONDS * 1000)) {
				this.cancel();
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						UnityAdsDeviceLog.error("Buffering taking too long.. cancelling video play");
						videoPlaybackFailed();
					}
				});
			}

			if (_videoPlayheadPrepared && _playHeadHasMoved) {
				UnityAdsUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!_videoPlaybackStartedSent) {
							if (_listener != null) {
								_videoPlaybackStartedSent = true;
								_listener.onVideoPlaybackStarted();
								_videoStartedPlayingMillis = System.currentTimeMillis();
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