package com.unity3d.ads.android.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Canvas.VertexMode;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unity3d.ads.android.R;

public class UnityAdsVideoPausedView extends RelativeLayout {
	private ImageView _outerStroke = null;

	private static float screenDensity;
	
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
			RelativeLayout layout = (RelativeLayout)inflater.inflate(R.layout.unityads_video_paused_view, null);
			addView(layout, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
	}
}
