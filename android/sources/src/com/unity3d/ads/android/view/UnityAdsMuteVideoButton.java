package com.unity3d.ads.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.unity3d.ads.android.R;
import com.unity3d.ads.android.UnityAdsDeviceLog;

public class UnityAdsMuteVideoButton extends RelativeLayout {

	private UnityAdsMuteVideoButtonState _state = UnityAdsMuteVideoButtonState.UnMuted;

	public static enum UnityAdsMuteVideoButtonState { UnMuted, Muted }
	private RelativeLayout _layout = null;

	public UnityAdsMuteVideoButton(Context context) {
		super(context);
		setupView();
	}

	public UnityAdsMuteVideoButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupView();
	}

	public UnityAdsMuteVideoButton(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setupView();
	}
	
	public void setState (UnityAdsMuteVideoButtonState state) {
		if (state != null && !state.equals(_state)) {
			_state = state;

			View muted = _layout.findViewById(R.id.unityAdsMuteButtonSpeakerX);
			View unmuted = _layout.findViewById(R.id.unityAdsMuteButtonSpeakerWaves);

			if (muted != null && unmuted != null) {
				switch (_state) {
					case Muted:
						muted.setVisibility(View.VISIBLE);
						unmuted.setVisibility(View.GONE);
						break;
					case UnMuted:
						muted.setVisibility(View.GONE);
						unmuted.setVisibility(View.VISIBLE);
						break;
					default:
						UnityAdsDeviceLog.debug("Invalid state: " + _state);
						break;
				}
			}

		}
	}

	private void setupView () {
		LayoutInflater inflater = LayoutInflater.from(getContext());

		if (inflater != null) {
			_layout = (RelativeLayout)inflater.inflate(R.layout.unityads_button_audio_toggle, null);
			addView(_layout, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}
}