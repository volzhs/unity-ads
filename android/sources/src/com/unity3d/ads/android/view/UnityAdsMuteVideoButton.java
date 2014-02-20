package com.unity3d.ads.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.unity3d.ads.android.data.UnityAdsDevice;
import com.unity3d.ads.android.data.UnityAdsGraphicsBundle;

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
		String bitmapString = "";
		if (_size != null && _size.equals(UnityAdsMuteVideoButtonSize.Medium)) {
			switch (_state) {
				case UnMuted:
					bitmapString = UnityAdsGraphicsBundle.ICON_AUDIO_UNMUTED_50x50;
					if (UnityAdsDevice.getScreenDensity() == DisplayMetrics.DENSITY_LOW) {
						bitmapString = UnityAdsGraphicsBundle.ICON_AUDIO_UNMUTED_32x32;
					}
					return UnityAdsGraphicsBundle.getBitmapFromString(bitmapString);
				case Muted:
					bitmapString = UnityAdsGraphicsBundle.ICON_AUDIO_MUTED_50x50;
					if (UnityAdsDevice.getScreenDensity() == DisplayMetrics.DENSITY_LOW) {
						bitmapString = UnityAdsGraphicsBundle.ICON_AUDIO_MUTED_32x32;
					}

					return UnityAdsGraphicsBundle.getBitmapFromString(bitmapString);
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
