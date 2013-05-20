package com.unity3d.ads.android.view;

import com.unity3d.ads.android.data.UnityAdsGraphicsBundle;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class UnityAdsMuteVideoButton extends ImageButton {

	private UnityAdsMuteVideoButtonState _state = UnityAdsMuteVideoButtonState.UnMuted;
	private UnityAdsMuteVideoButtonSize _size = UnityAdsMuteVideoButtonSize.Medium;
	
	public static enum UnityAdsMuteVideoButtonState { UnMuted, Muted };
	public static enum UnityAdsMuteVideoButtonSize { Small, Medium, Large };
	
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
			setImageBitmap(selectBitmap());
		}
	}
	
	private Bitmap selectBitmap () {
		if (_size != null && _size.equals(UnityAdsMuteVideoButtonSize.Medium)) {
			switch (_state) {
				case UnMuted:
					return UnityAdsGraphicsBundle.getBitmapFromString(UnityAdsGraphicsBundle.ICON_AUDIO_UNMUTED_50x50);
				case Muted:
					return UnityAdsGraphicsBundle.getBitmapFromString(UnityAdsGraphicsBundle.ICON_AUDIO_MUTED_50x50);
			}
		}
		
		return null;
	}
	
	private void setupView () {
		setAdjustViewBounds(true);
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
		setImageBitmap(selectBitmap());
		setBackgroundResource(0);
		setPadding(0, 0, 0, 0);
	}
}
