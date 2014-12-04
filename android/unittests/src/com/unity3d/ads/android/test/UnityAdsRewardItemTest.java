package com.unity3d.ads.android.test;

import java.util.HashMap;

import org.json.JSONObject;

import com.unity3d.ads.android.example.UnityAdsTestStartActivity;
import com.unity3d.ads.android.item.UnityAdsRewardItem;

import android.test.ActivityInstrumentationTestCase2;

public class UnityAdsRewardItemTest extends ActivityInstrumentationTestCase2<UnityAdsTestStartActivity> {

	private JSONObject validItem, invalidItem;
	private UnityAdsRewardItem rewardItem;
	
	public UnityAdsRewardItemTest() {
		super(UnityAdsTestStartActivity.class);
	}
	
	@Override
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		super.setUp();
		
		validItem = new JSONObject(new HashMap<String, Object>(){{
			put("key", "testItemKey1");
			put("name", "testItemName1");
			put("picture", "http://invalid.url.com");
		}});
		
		invalidItem = new JSONObject(new HashMap<String, Object>(){{
			put("key", "testItemKey2");
			put("picture", "http://invalid.url.com");
		}});
		
		rewardItem = null;
	}
	
	public void testValidItem() {
		rewardItem = new UnityAdsRewardItem(validItem);
		assertTrue(rewardItem.getKey().equals("testItemKey1"));
		assertTrue(rewardItem.getName().equals("testItemName1"));
		assertTrue(rewardItem.getPictureUrl().equals("http://invalid.url.com"));
	}
	
	public void testInvalidItem() {
		rewardItem = new UnityAdsRewardItem(invalidItem);
		assertTrue(!rewardItem.hasValidData());
	}
	
}
