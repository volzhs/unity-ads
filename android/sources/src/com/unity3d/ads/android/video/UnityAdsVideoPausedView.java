package com.unity3d.ads.android.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.unity3d.ads.android.R;

public class UnityAdsVideoPausedView extends RelativeLayout {
	public UnityAdsVideoPausedView(Context context) {
		super(context);
		createView();
	}

	public UnityAdsVideoPausedView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createView();
	}

	public UnityAdsVideoPausedView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		createView();
	}

	private void createView () {
		LayoutInflater inflater = LayoutInflater.from(getContext());

		if (inflater != null) {
			RelativeLayout layout = (RelativeLayout)inflater.inflate(R.layout.unityads_view_video_paused, null);
			addView(layout, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
	}
}