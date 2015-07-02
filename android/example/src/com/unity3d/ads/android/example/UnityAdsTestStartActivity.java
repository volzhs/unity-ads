
package com.unity3d.ads.android.example;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;

public class UnityAdsTestStartActivity extends Activity implements IUnityAdsListener {
	private UnityAdsTestStartActivity _self = null;
	private Button _settingsButton = null;
	private Button _startButton = null;
	private RelativeLayout _optionsView = null;
	private TextView _statusText = null;
	private final String _exampleAppLogTag = "UnityAdsExample";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onCreate()");
        super.onCreate(savedInstanceState);
        
        _self = this;
        
        setContentView(R.layout.main);
		Log.d(_exampleAppLogTag, "Init Unity Ads");
		
		UnityAds.setDebugMode(true);
		//UnityAds.setTestMode(true);

		_optionsView = ((RelativeLayout)findViewById(R.id.unityads_example_optionsview));
		_statusText = ((TextView)findViewById(R.id.unityads_status));

		_settingsButton = ((Button)findViewById(R.id.unityads_settings));
		_settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (_optionsView != null) {
					if (_optionsView.getVisibility() == View.INVISIBLE) {
						_optionsView.setVisibility(View.VISIBLE);
					} else {
						_optionsView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
		
		_startButton = ((Button)findViewById(R.id.unityads_example_startbutton));
		_startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				_statusText.setVisibility(View.VISIBLE);
				UnityAds.setTestDeveloperId(((EditText) findViewById(R.id.unityads_example_developer_id_data)).getText().toString());
				UnityAds.setTestOptionsId(((EditText) findViewById(R.id.options_id_data)).getText().toString());
				UnityAds.init(_self, "14851", _self);
				UnityAds.setListener(_self);
			}
		});

		if (UnityAds.canShow()) {
			onFetchCompleted();
		}
    }
    
    @Override
    public void onResume () {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onResume()");
    	super.onResume();
    	
   		UnityAds.changeActivity(this);
   		UnityAds.setListener(this);
    }

    @Override
	protected void onDestroy() {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onDestroy()");
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
		Log.d(_exampleAppLogTag, "Video Started!");
    }
    
    @Override
	public void onVideoCompleted (String rewardItemKey, boolean skipped) {
    	if(skipped) {
    		Log.d(_exampleAppLogTag, "Video was skipped!");
    	}
		else {
			Log.d(_exampleAppLogTag, "Video Completed!");
		}
    }
	
	// Unity Ads campaign events
    @Override
	public void onFetchCompleted () {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onFetchCompleted()");

		_statusText.setVisibility(View.VISIBLE);
		_statusText.setText(getResources().getString(R.string.unityads_example_loaded));

    	TextView instructions = ((TextView)findViewById(R.id.unityads_example_instructions));
		instructions.setText(R.string.unityads_example_helptextloaded);
    	
    	_settingsButton.setEnabled(false);
    	_settingsButton.setVisibility(View.INVISIBLE);
    	_startButton.setEnabled(false);
    	_startButton.setVisibility(View.INVISIBLE);
    	_optionsView.setVisibility(View.INVISIBLE);
    	
    	Button openButton = ((Button)findViewById(R.id.unityads_example_openbutton));
		openButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Open with options test
				Map<String, Object> optionsMap = new HashMap<>();
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, true);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, "gom");
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION, false);
				
				UnityAds.show(optionsMap);
			}
		});
		openButton.setVisibility(View.VISIBLE);
	}
    
    @Override
    public void onFetchFailed () {
    }
}