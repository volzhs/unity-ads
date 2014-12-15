package com.unity3d.ads.android.test;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.test.ActivityInstrumentationTestCase2;

import com.unity3d.ads.android.example.UnityAdsTestStartActivity;
import com.unity3d.ads.android.zone.UnityAdsZone;

@SuppressWarnings("serial")
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
	
	public void testZoneInitialOptionsMerge() {
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("openAnimated", false);
		}});
		assertTrue(!validZone.openAnimated());
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("noOfferScreen", true);
		}});
		assertTrue(!validZone.muteVideoSounds());
		assertTrue(validZone.noOfferScreen());
	}
	
	public void testZoneSetSid() {
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("sid", "testSid");
		}});
		assertTrue(validZone.getGamerSid().equals("testSid"));
	}
	
	public void testZoneRemoveSid() {
		validZone.mergeOptions(new HashMap<String, Object>(){{
			put("sid", "testSid");
		}});
		assertTrue(validZone.getGamerSid().equals("testSid"));
		validZone.mergeOptions(null);
		assertTrue(validZone.getGamerSid() == null);
	}
	
}
