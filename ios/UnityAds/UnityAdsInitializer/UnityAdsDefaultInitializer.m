//
//  UnityAdsDefaultInitializer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsDefaultInitializer.h"

#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAds.h"

#import "../UnityAdsViewState/UnityAdsViewStateDefaultOffers.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultVideoPlayer.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultEndScreen.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultSpinner.h"

@implementation UnityAdsDefaultInitializer

- (void)initAds:(NSDictionary *)options {
	UALOG_DEBUG(@"");
  [super initAds:options];
  
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateDefaultOffers alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateDefaultVideoPlayer alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateDefaultEndScreen alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateDefaultSpinner alloc] init]];
  
  [UnityAdsWebAppController sharedInstance];
  [[UnityAdsWebAppController sharedInstance] setDelegate:self];
  
  [self performSelector:@selector(_initCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
  [self performSelector:@selector(_initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}

- (BOOL)initWasSuccessfull {
  if ([[UnityAdsWebAppController sharedInstance] webViewInitialized]) {
    return YES;
  }
  return NO;
}

- (void)reInitialize {
  dispatch_async(self.queue, ^{
    [[UnityAdsWebAppController sharedInstance] setWebViewInitialized:NO];
		[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
		[self performSelector:@selector(_refreshCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
    [self performSelector:@selector(_initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	});
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
}

- (void)campaignManagerCampaignDataReceived {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"Campaign data received.");
  
  if ([[UnityAdsCampaignManager sharedInstance] campaignData] != nil) {
    [[UnityAdsWebAppController sharedInstance] setWebViewInitialized:NO];
  }
  
  if (![[UnityAdsWebAppController sharedInstance] webViewInitialized]) {
    [[UnityAdsWebAppController sharedInstance] initWebApp];
  }
}

- (void)campaignManagerCampaignDataFailed {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"Campaign data failed.");
  
  dispatch_async(dispatch_get_main_queue(), ^{
    if (self.delegate != nil) {
      [self.delegate initFailed];
  }});
}


#pragma mark - WebAppController

- (void)webAppReady {
  UALOG_DEBUG(@"webAppReady");
  dispatch_async(dispatch_get_main_queue(), ^{
    [self checkForVersionAndShowAlertDialog];
    
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIInitComplete, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key}];
    
    if (self.delegate != nil) {
      [self.delegate initComplete];
    }
  });
}

@end
