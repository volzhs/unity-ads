package com.unity3d.ads.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;

import android.os.Environment;
import android.util.Log;

public class UnityAdsUtils {
	
	public static ArrayList<UnityAdsCampaign> createCampaignsFromJson (JSONObject json) {
		if (json != null && json.has("va")) {
			ArrayList<UnityAdsCampaign> campaignData = new ArrayList<UnityAdsCampaign>();
			JSONArray va = null;
			JSONObject currentCampaign = null;
			
			try {
				va = json.getJSONArray("va");
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON");
			}
			
			for (int i = 0; i < va.length(); i++) {
				try {
					currentCampaign = va.getJSONObject(i);
					campaignData.add(new UnityAdsCampaign(currentCampaign));
				}
				catch (Exception e) {
					Log.d(UnityAdsProperties.LOG_NAME, "Malformed JSON");
				}				
			}
			
			return campaignData;
		}
		
		return null;
	}
	
	public static String readFile (File fileToRead, boolean addLineBreaks) {
		String fileContent = "";
		BufferedReader br = null;
		
		if (fileToRead.exists() && fileToRead.canRead()) {
			try {
				br = new BufferedReader(new FileReader(fileToRead));
				String line = null;
				
				while ((line = br.readLine()) != null) {
					fileContent = fileContent.concat(line);
					if (addLineBreaks)
						fileContent = fileContent.concat("\n");
				}
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problem reading file: " + e.getMessage());
				return null;
			}
			
			try {
				br.close();
			}
			catch (Exception e) {
				Log.d(UnityAdsProperties.LOG_NAME, "Problem closing reader: " + e.getMessage());
			}
						
			return fileContent;
		}
		else {
			Log.d(UnityAdsProperties.LOG_NAME, "File did not exist or couldn't be read");
		}
		
		return null;
	}
	
	public static boolean writeFile (File fileToWrite, String content) {
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(fileToWrite);
			fos.write(content.getBytes());
			fos.flush();
			fos.close();
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Could not write file: " + e.getMessage());
			return false;
		}
		
		Log.d(UnityAdsProperties.LOG_NAME, "Wrote file: " + fileToWrite.getAbsolutePath());
		
		return true;
	}
	
	public static void removeFile (String fileName) {
		File removeFile = new File (fileName);
		File cachedVideoFile = new File (UnityAdsUtils.getCacheDirectory() + "/" + removeFile.getName());
		
		if (cachedVideoFile.exists()) {
			if (!cachedVideoFile.delete())
				Log.d(UnityAdsProperties.LOG_NAME, "Could not delete: " + cachedVideoFile.getAbsolutePath());
			else
				Log.d(UnityAdsProperties.LOG_NAME, "Deleted: " + cachedVideoFile.getAbsolutePath());
		}
		else {
			Log.d(UnityAdsProperties.LOG_NAME, "File: " + cachedVideoFile.getAbsolutePath() + " doesn't exist.");
		}
	}
		
	public static JSONObject createJsonFromCampaigns (ArrayList<UnityAdsCampaign> campaignList) {
		JSONObject retJson = new JSONObject();
		JSONArray campaigns = new JSONArray();
		JSONObject currentCampaign = null;
		
		try {
			for (UnityAdsCampaign campaign : campaignList) {
				currentCampaign = campaign.toJson();
				
				if (currentCampaign != null)
					campaigns.put(currentCampaign);
			}
			
			retJson.put("va", campaigns);
		}
		catch (Exception e) {
			Log.d(UnityAdsProperties.LOG_NAME, "Error while creating JSON from Campaigns");
			return null;
		}
		
		return retJson;
	}
		
	public static ArrayList<UnityAdsCampaign> substractFromCampaignList (ArrayList<UnityAdsCampaign> fromList, ArrayList<UnityAdsCampaign> substractionList) {
		if (fromList == null) return null;
		if (substractionList == null) return fromList;
		
		ArrayList<UnityAdsCampaign> pruneList = null;
		
		for (UnityAdsCampaign fromCampaign : fromList) {
			boolean match = false;
			
			for (UnityAdsCampaign substractionCampaign : substractionList) {
				if (fromCampaign.getCampaignId().equals(substractionCampaign.getCampaignId())) {
					match = true;
					break;
				}					
			}
			
			if (match)
				continue;
			
			if (pruneList == null)
				pruneList = new ArrayList<UnityAdsCampaign>();
			
			pruneList.add(fromCampaign);
		}
		
		return pruneList;
	}
	
	public static String getCacheDirectory () {
		return Environment.getExternalStorageDirectory().toString() + "/" + UnityAdsProperties.CACHE_DIR_NAME;
	}
	
	public static File createCacheDir () {
		File tdir = new File (getCacheDirectory());
		tdir.mkdirs();
		return tdir;
	}
	
	public static boolean isFileRequiredByCampaigns (String fileName, ArrayList<UnityAdsCampaign> campaigns) {
		if (fileName == null || campaigns == null) return false;
		
		File seekFile = new File(fileName);
		
		for (UnityAdsCampaign campaign : campaigns) {
			File matchFile = new File(campaign.getVideoUrl());
			if (seekFile.getName().equals(matchFile.getName()))
				return true;
		}
		
		return false;
	}
	
	public static ArrayList<UnityAdsCampaign> getViewableCampaignsFromCampaignList (ArrayList<UnityAdsCampaign> campaignList) {
		ArrayList<UnityAdsCampaign> retList = new ArrayList<UnityAdsCampaign>();
		
		if (campaignList != null) {
			for (UnityAdsCampaign campaign : campaignList) {
				if (campaign != null && campaign.getCampaignStatus() != UnityAdsCampaignStatus.VIEWED && campaign.getCampaignStatus() != UnityAdsCampaignStatus.PANIC)
					retList.add(campaign);
			}
			
			return retList;
		}
		
		return null;		
	}
	
	public static boolean isFileInCache (String fileName) {
		File targetFile = new File (fileName);
		File testFile = new File(getCacheDirectory() + "/" + targetFile.getName());
		return testFile.exists();
	}
}
