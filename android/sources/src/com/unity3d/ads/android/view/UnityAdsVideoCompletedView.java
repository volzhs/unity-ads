package com.unity3d.ads.android.view;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.unity3d.ads.android.R;

public class UnityAdsVideoCompletedView extends FrameLayout {

	public UnityAdsVideoCompletedView(Context context) {
		super(context);
		createView();		
	}

	public UnityAdsVideoCompletedView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createView();
	}

	public UnityAdsVideoCompletedView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		createView();
	}

	private void createView () {
		Log.d(UnityAdsProperties.LOG_NAME, "Creating custom view");
		setBackgroundColor(0xBA000000);
		inflate(getContext(), R.layout.applifier_videoshown, this);
        
		((ImageView)findViewById(R.id.closeb)).setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
		    	onKeyDown(KeyEvent.KEYCODE_BACK, null);
			}
		});
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
		    	UnityAds.instance.closeAdsView(this, true);
		    	return true;
		}
		
    	return false;
    }  
}
