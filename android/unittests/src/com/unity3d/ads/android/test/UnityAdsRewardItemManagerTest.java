package com.unity3d.ads.android.test;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.unity3d.ads.android.example.UnityAdsTestStartActivity;
import com.unity3d.ads.android.item.UnityAdsRewardItem;
import com.unity3d.ads.android.item.UnityAdsRewardItemManager;

import android.test.ActivityInstrumentationTestCase2;

public class UnityAdsRewardItemManagerTest extends ActivityInstrumentationTestCase2<UnityAdsTestStartActivity> {

	private JSONObject rewardItem1, rewardItem2;
	private UnityAdsRewardItemManager itemManager;
	
	public UnityAdsRewardItemManagerTest() {
		super(UnityAdsTestStartActivity.class);
	}
	
	@Override
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		super.setUp();
		
		rewardItem1 = new JSONObject(new HashMap<String, Object>(){{
			put("key", "testItemKey1");
			put("name", "testItemName1");
			put("picture", "http://invalid.url.com");
		}});
		
		rewardItem2 = new JSONObject(new HashMap<String, Object>(){{
			put("key", "testItemKey2");
			put("name", "testItemName2");
			put("picture", "http://invalid.url.com");
		}});
		
		itemManager = null;
	}
	
	public void testEmptyItems() {
		itemManager = new UnityAdsRewardItemManager(new JSONArray(), "defaultItem");
		assertTrue(itemManager.itemCount() == 0);
	}
	
	public void testSingleItem() {
		itemManager = new UnityAdsRewardItemManager(new JSONArray(Arrays.asList(rewardItem1)), "testItemKey1");
		assertTrue(itemManager.itemCount() == 1);
		
		UnityAdsRewardItem defaultItem = itemManager.getDefaultItem();
		UnityAdsRewardItem currentItem = itemManager.getCurrentItem();
		
		assertNotNull(defaultItem);
		assertNotNull(currentItem);
		
		assertTrue(defaultItem.getKey().equals("testItemKey1"));
		assertTrue(currentItem.getKey().equals("testItemKey1"));
	}
	
	public void testMultipleItems() {
		itemManager = new UnityAdsRewardItemManager(new JSONArray(Arrays.asList(
			rewardItem1,
			rewardItem2
		)), "testItemKey2");
		
		assertTrue(itemManager.itemCount() == 2);
		
		UnityAdsRewardItem defaultItem = itemManager.getDefaultItem();
		UnityAdsRewardItem currentItem = itemManager.getCurrentItem();
		
		assertNotNull(defaultItem);
		assertNotNull(currentItem);
		
		assertTrue(defaultItem.getKey().equals("testItemKey2"));
		assertTrue(currentItem.getKey().equals("testItemKey2"));
	}
	
	public void testMultipleItemSwitching() {
		itemManager = new UnityAdsRewardItemManager(new JSONArray(Arrays.asList(
			rewardItem1,
			rewardItem2
		)), "testItemKey2");
		
		assertTrue(itemManager.itemCount() == 2);
		
		itemManager.setCurrentItem("testItemKey1");
		
		UnityAdsRewardItem defaultItem = itemManager.getDefaultItem();
		UnityAdsRewardItem currentItem = itemManager.getCurrentItem();
		
		assertNotNull(defaultItem);
		assertNotNull(currentItem);
		
		assertTrue(defaultItem.getKey().equals("testItemKey2"));
		assertTrue(currentItem.getKey().equals("testItemKey1"));
	}
	
	public void testMissingDefaultItem() {
		itemManager = new UnityAdsRewardItemManager(new JSONArray(Arrays.asList(
			rewardItem1,
			rewardItem2
		)), "testItemKey3");
		
		assertTrue(itemManager.itemCount() == 2);
		assertNull(itemManager.getDefaultItem());
		assertNull(itemManager.getCurrentItem());
	}
	
}
