package com.mycompany.test;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.IUnityAdsListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.unity3d.ads.android.video.IUnityAdsVideoListener;

public class UnityAdsGameActivity extends Activity implements IUnityAdsListener, IUnityAdsVideoListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        
        ((ImageView)findViewById(R.id.plissken)).setAlpha(60);
        ((ImageView)findViewById(R.id.unlock)).setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				UnityAds.instance.show();
			}
		});
        
        UnityAds.instance.setListener(this);
    }
    
    @Override
    public void onResume () {
    	super.onResume();
		UnityAds.instance.changeActivity(this);
		UnityAds.instance.setListener(this);
		UnityAds.instance.setVideoListener(this);
    }
    
    public void onHide () {
    	Log.d(UnityAdsProperties.LOG_NAME, "Unity Ads close");
    }
    
    public void onShow () {   	
    	Log.d(UnityAdsProperties.LOG_NAME, "Unity Ads open");
    }
    
	public void onVideoStarted () {
		Log.d(UnityAdsProperties.LOG_NAME, "Video started!");
	}
	
	public void onVideoCompleted () {
    	((ImageView)findViewById(R.id.plissken)).setAlpha(255);
    	((ImageView)findViewById(R.id.unlock)).setVisibility(View.INVISIBLE);
    	Log.d(UnityAdsProperties.LOG_NAME, "Video completed!");
	}
}
