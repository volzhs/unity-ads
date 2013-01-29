package com.mycompany.test;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.unity3d.ads.android.properties.UnityAdsConstants;

public class UnityAdsGameActivity extends Activity implements IUnityAdsListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsGameActivity->onCreate()");
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
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsGameActivity->onResume()");
    	super.onResume();
    	
    	UnityAds.instance.changeActivity(this);
		UnityAds.instance.setListener(this);
		
		if (!UnityAds.instance.canShowAds()) {
			((ImageView)findViewById(R.id.unlock)).setVisibility(View.INVISIBLE);
		}
    }
    
    public void onHide () {
    	Log.d(UnityAdsConstants.LOG_NAME, "HOST: Unity Ads close");
    }
    
    public void onShow () {   	
    	Log.d(UnityAdsConstants.LOG_NAME, "HOST: Unity Ads open");
    }
    
	public void onVideoStarted () {
		Log.d(UnityAdsConstants.LOG_NAME, "HOST: Video started!");
	}
	
	public void onVideoCompleted () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsGameActivity->onVideoCompleted()");
    	((ImageView)findViewById(R.id.plissken)).setAlpha(255);
    	((ImageView)findViewById(R.id.unlock)).setVisibility(View.INVISIBLE);
    	Log.d(UnityAdsConstants.LOG_NAME, "HOST: Video completed!");
	}
	
    @Override
	public void onFetchCompleted () {
	}
}
