//
//  UnityAdsDefaultInitializer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsDefaultInitializer.h"

#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsViewState/UnityAdsViewStateOfferScreen.h"
#import "../UnityAdsViewState/UnityAdsViewStateVideoPlayer.h"
#import "../UnityAdsViewState/UnityAdsViewStateEndScreen.h"

#import "../UnityAdsZone/UnityAdsZoneManager.h"
#import "../UnityAdsZone/UnityAdsIncentivizedZone.h"

@implementation UnityAdsDefaultInitializer

- (void)initAds:(NSDictionary *)options {
	UALOG_DEBUG(@"");
  [super initAds:options];
  
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateOfferScreen alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateVideoPlayer alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateEndScreen alloc] init]];
  
  [UnityAdsWebAppController sharedInstance];
  [(UnityAdsWebAppController *)[UnityAdsWebAppController sharedInstance] setDelegate:self];
  
  [self performSelector:@selector(initCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
  [self performSelector:@selector(initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
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
		[self performSelector:@selector(refreshCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
    [self performSelector:@selector(initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	});
}


#pragma mark - Private initalization

- (void)initCampaignManager {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
  [(UnityAdsCampaignManager *)[UnityAdsCampaignManager sharedInstance] setDelegate:self];
  [super initCampaignManager];
}


#pragma mark - UnityAdsCampaignManagerDelegate

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns gamerID:(NSString *)gamerID {
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
    
    id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
    if([currentZone isIncentivized]) {
      id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
      UAAssert(itemManager != nil);
      if (!itemManager) return;
      [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIInitComplete, kUnityAdsItemKeyKey:[itemManager getCurrentItem].key}];
    } else {
      [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIInitComplete}];
    }    
    
    if (self.delegate != nil) {
      [self.delegate initComplete];
    }
  });
}

@end
