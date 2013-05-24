
package com.mycompany.test;

import java.util.HashMap;
import java.util.Map;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;
import com.unity3d.ads.android.properties.UnityAdsConstants;

import com.mycompany.test.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UnityAdsTestStartActivity extends Activity implements IUnityAdsListener {
	private UnityAds ai = null;
	private UnityAdsTestStartActivity _self = null;
	private Button _piButton = null;
	private Button _startButton = null;
	private Button _openButton = null;
	private RelativeLayout _optionsView = null;
	private TextView _instructions = null;
	private ImageView _statusImage = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onCreate()");
        super.onCreate(savedInstanceState);
        
        _self = this;
        
        setContentView(R.layout.main);
        //((ImageView)findViewById(R.id.playbtn)).setAlpha(80);
		Log.d(UnityAdsConstants.LOG_NAME, "Init Unity Ads");
		
		UnityAds.setDebugMode(true);
		UnityAds.setTestMode(true);
		
		_optionsView = ((RelativeLayout)findViewById(R.id.optionsView));
		
		_piButton = ((Button)findViewById(R.id.sandrabullock));
		_piButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (_optionsView != null) {
					if (_optionsView.getVisibility() == View.INVISIBLE) {
						_optionsView.setVisibility(View.VISIBLE);
					}
					else {
						_optionsView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
		
		_startButton = ((Button)findViewById(R.id.startAdsButton));
		_startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		    	_statusImage = ((ImageView)findViewById(R.id.unityads_status));
		    	_statusImage.setVisibility(View.VISIBLE);
		    	UnityAds.setTestDeveloperId(((EditText)findViewById(R.id.developer_id_data)).getText().toString());
		    	UnityAds.setTestOptionsId(((EditText)findViewById(R.id.options_id_data)).getText().toString());
				ai = new UnityAds(_self, "16", _self);
	    		ai.changeActivity(_self);
	    		ai.setListener(_self);
			}
		});
    }
    
    @Override
    public void onResume () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onResume()");
    	super.onResume();
    	
    	if (ai != null) {
    		ai.changeActivity(this);
    		ai.setListener(this);
    	}
    }
    
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.kill:
		    	ai.stopAll();
		    	System.runFinalizersOnExit(true);		
				finish();
		    	Log.d(UnityAdsConstants.LOG_NAME, "Quitting");

		    	break;
		}
		
		return true;
	}
	
    @Override
	protected void onDestroy() {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onDestroy()");
    	super.onDestroy();		
	}
	
    @Override
	public void onHide () {
    	
    }
    
    @Override
	public void onShow () {
    	
    }
	
	// Unity Ads video events
    @Override
	public void onVideoStarted () {
    	
    }
    
    @Override
	public void onVideoCompleted (String rewardItemKey) {
    	_statusImage.setImageResource(R.drawable.unityads_reward);
    }
	
	// Unity Ads campaign events
    @Override
	public void onFetchCompleted () {
    	Log.d(UnityAdsConstants.LOG_NAME, "UnityAdsTestStartActivity->onFetchCompleted()");
    	
    	_statusImage.setImageResource(R.drawable.unityads_loaded);
    	
    	_instructions = ((TextView)findViewById(R.id.instructionsText));
    	_instructions.setText(R.string.helpTextLoaded);
    	
    	_piButton.setEnabled(false);
    	_piButton.setVisibility(View.INVISIBLE);
    	_startButton.setEnabled(false);
    	_startButton.setVisibility(View.INVISIBLE);
    	_optionsView.setVisibility(View.INVISIBLE);
    	
    	_openButton = ((Button)findViewById(R.id.openAdsButton));
    	_openButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Open with options test
				Map<String, Object> optionsMap = new HashMap<String, Object>();
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, "gom");
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS, true);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION, false);
				
				UnityAds.instance.show(optionsMap);
				
				// Open without options (defaults)
				//UnityAds.instance.show();
			}
		});
    	_openButton.setVisibility(View.VISIBLE);
	}
    
    @Override
    public void onFetchFailed () {
    }
}