//
//  UnityAdsDefaultInitializer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsDefaultInitializer.h"

#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"
#import "../UnityAds.h"

@implementation UnityAdsDefaultInitializer

- (void)init:(NSDictionary *)options {
  [super init:options];
  
  [self performSelector:@selector(_initCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
  [self performSelector:@selector(_initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}

#pragma mark - Private initalization

- (void)_initCampaignManager {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
  [[UnityAdsCampaignManager sharedInstance] setDelegate:self];
	[self _refreshCampaignManager];
}

- (void)_refreshCampaignManager {
	UAAssert(![NSThread isMainThread]);
	[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
	[[UnityAdsCampaignManager sharedInstance] updateCampaigns];
}

- (void)_initAnalyticsUploader {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
	[[UnityAdsAnalyticsUploader sharedInstance] retryFailedUploads];
}


#pragma mark - UnityAdsCampaignManagerDelegate

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem gamerID:(NSString *)gamerID {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	//[self _notifyDelegateOfCampaignAvailability];
}

- (void)campaignManagerCampaignDataReceived {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"Campaign data received.");
  /*
  if ([[UnityAdsCampaignManager sharedInstance] campaignData] != nil) {
    [[UnityAdsWebAppController sharedInstance] setWebViewInitialized:NO];
  }
  
  if (![[UnityAdsWebAppController sharedInstance] webViewInitialized]) {
    [[UnityAdsWebAppController sharedInstance] initWebApp];
  }*/
}

- (void)campaignManagerCampaignDataFailed {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"Campaign data failed.");
  /*
  if ([self.delegate respondsToSelector:@selector(unityAdsFetchFailed:)])
		[self.delegate unityAdsFetchFailed:self];*/
  
  if (self.delegate != nil) {
    [self.delegate initFailed];
  }
}

@end
