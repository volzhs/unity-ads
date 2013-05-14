package com.unity3d.ads.android.view;

import com.unity3d.ads.android.data.UnityAdsGraphicsBundle;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class UnityAdsMuteVideoButton extends ImageButton {

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
	
	private void setupView () {
		setAdjustViewBounds(true);
		//setMinimumHeight(32);
		//set(32);
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
		setImageBitmap(UnityAdsGraphicsBundle.getBitmapFromString(UnityAdsGraphicsBundle.ICON_AUDIO_UNMUTED_32x23));
		setBackgroundResource(0);
		//setMaxHeight(50);
		//setMaxWidth(50);
		setPadding(0, 0, 0, 0);
	}
}
