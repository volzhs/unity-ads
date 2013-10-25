package com.mycompany.test.test;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.test.ActivityInstrumentationTestCase2;
import com.mycompany.test.UnityAdsTestStartActivity;
import com.unity3d.ads.android.zone.UnityAdsZone;

public class UnityAdsZoneTest extends ActivityInstrumentationTestCase2<UnityAdsTestStartActivity> {

	private UnityAdsZone validZone;
	
	public UnityAdsZoneTest() {
		super(UnityAdsTestStartActivity.class);
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		JSONObject validZoneObject = new JSONObject(new HashMap<String, Object>(){{
			put("id", "testZoneId1");
			put("name", "testZoneName1");
			put("noOfferScreen", false);
			put("openAnimated", true);
			put("muteVideoSounds", false);
			put("useDeviceOrientationForVideo", true);
			put("allowClientOverrides", new JSONArray(Arrays.asList("noOfferScreen", "openAnimated")));
		}});
		validZone = new UnityAdsZone(validZoneObject);
	}
	
	public void testZoneValidOverrides() {
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("openAnimated", false);
		}});
		assertTrue(!validZone.openAnimated());
	}
	
	public void testZoneInvalidOverrides() {
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("muteVideoSounds", true);
		}});
		assertTrue(!validZone.muteVideoSounds());
	}
	
}
