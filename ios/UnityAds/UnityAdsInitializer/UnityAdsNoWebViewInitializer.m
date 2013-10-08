//
//  UnityAdsNoWebViewInitializer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/10/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsNoWebViewInitializer.h"

#import "../UnityAdsViewState/UnityAdsViewStateNoWebViewVideoPlayer.h"
#import "../UnityAdsViewState/UnityAdsViewStateNoWebViewEndScreen.h"

@interface UnityAdsNoWebViewInitializer ()
  @property (nonatomic, assign) BOOL campaignDataReceived;
@end

@implementation UnityAdsNoWebViewInitializer

- (void)initAds:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  self.campaignDataReceived = false;
  
  [super initAds:options];
  
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateNoWebViewVideoPlayer alloc] init]];
  [[UnityAdsMainViewController sharedInstance] applyViewStateHandler:[[UnityAdsViewStateNoWebViewEndScreen alloc] init]];
  
  [self performSelector:@selector(initCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
  [self performSelector:@selector(initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}


- (void)reInitialize {
  self.campaignDataReceived = false;
  dispatch_async(self.queue, ^{
		[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
		[self performSelector:@selector(refreshCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
    [self performSelector:@selector(initAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	});
}

- (BOOL)initWasSuccessfull {
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil &&
      [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 &&
      self.campaignDataReceived) {
    UALOG_DEBUG(@"");
    return YES;
  }
  return NO;
}

#pragma mark - Private initalization

- (void)initCampaignManager {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
  [[UnityAdsCampaignManager sharedInstance] setDelegate:self];
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
  
  self.campaignDataReceived = true;
  [self checkForVersionAndShowAlertDialog];
  
  dispatch_async(dispatch_get_main_queue(), ^{
    if (self.delegate != nil) {
      [self.delegate initComplete];
    }});
  
}

- (void)campaignManagerCampaignDataFailed {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"Campaign data failed.");
  
  dispatch_async(dispatch_get_main_queue(), ^{
    if (self.delegate != nil) {
      [self.delegate initFailed];
    }});
}

@end
