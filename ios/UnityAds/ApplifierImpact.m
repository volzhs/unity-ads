//
//  ApplifierImpact.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "ApplifierImpact.h"
#import <UnityAds.h>

NSString * const kApplifierImpactRewardItemPictureKey = @"picture";
NSString * const kApplifierImpactRewardItemNameKey = @"name";

NSString * const kApplifierImpactOptionNoOfferscreenKey = @"noOfferScreen";
NSString * const kApplifierImpactOptionOpenAnimatedKey = @"openAnimated";
NSString * const kApplifierImpactOptionGamerSIDKey = @"sid";
NSString * const kApplifierImpactOptionMuteVideoSounds = @"muteVideoSounds";
NSString * const kApplifierImpactOptionVideoUsesDeviceOrientation = @"useDeviceOrientationForVideo";

@interface ApplifierImpact () <UnityAdsDelegate>
  @property (nonatomic, assign) Boolean debug;
@end

@implementation ApplifierImpact

#pragma mark - Static accessors

+ (BOOL)isSupported {
  return [UnityAds isSupported];
}

- (void)setTestDeveloperId:(NSString *)developerId {
  [[UnityAds sharedInstance] setTestDeveloperId:developerId];
}

- (void)setTestOptionsId:(NSString *)optionsId {
  [[UnityAds sharedInstance] setTestOptionsId:optionsId];
}

+ (NSString *)getSDKVersion {
  return [UnityAds getSDKVersion];
}

- (void)setDebugMode:(BOOL)debugMode {
  [[UnityAds sharedInstance] setDebugMode:debugMode];
}

- (BOOL)isDebugMode {
  return [[UnityAds sharedInstance] isDebugMode];
}

static ApplifierImpact *sharedImpact = nil;

+ (ApplifierImpact *)sharedInstance {
	@synchronized(self) {
		if (sharedImpact == nil) {
      sharedImpact = [[ApplifierImpact alloc] init];
      sharedImpact.debug = NO;
      [[UnityAds sharedInstance] setDelegate:sharedImpact];
		}
	}
	
	return sharedImpact;
}

#pragma mark - Public

- (void)setTestMode:(BOOL)testModeEnabled {
  [[UnityAds sharedInstance] setTestMode:testModeEnabled];
}

- (BOOL)startWithGameId:(NSString *)gameId {
  return [[UnityAds sharedInstance] startWithGameId:gameId];
}

- (BOOL)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController {
  return [[UnityAds sharedInstance] startWithGameId:gameId andViewController:viewController];
}

- (BOOL)canShowCampaigns {
  return [[UnityAds sharedInstance] canShowAds];
}

- (BOOL)canShowImpact {
  return [[UnityAds sharedInstance] canShowAds];
}

- (BOOL)setZone:(NSString *)zoneId {
  return [[UnityAds sharedInstance] setZone:zoneId];
}

- (BOOL)setZone:(NSString *)zoneId withRewardItem:(NSString *)rewardItemKey {
  return [[UnityAds sharedInstance] setZone:zoneId withRewardItem:rewardItemKey];
}

- (BOOL)showImpact:(NSDictionary *)options {
  return [[UnityAds sharedInstance] show:options];
}

- (BOOL)showImpact {
  return [[UnityAds sharedInstance] show];
}

- (BOOL)hasMultipleRewardItems {
  return [[UnityAds sharedInstance] hasMultipleRewardItems];
}

- (NSArray *)getRewardItemKeys {
  return [[UnityAds sharedInstance] getRewardItemKeys];
}

- (NSString *)getDefaultRewardItemKey {
  return [[UnityAds sharedInstance] getDefaultRewardItemKey];
}

- (NSString *)getCurrentRewardItemKey {
  return [[UnityAds sharedInstance] getCurrentRewardItemKey];

}

- (BOOL)setRewardItemKey:(NSString *)rewardItemKey {
  return [[UnityAds sharedInstance] setRewardItemKey:rewardItemKey];
}

- (void)setDefaultRewardItemAsRewardItem {
  [[UnityAds sharedInstance] setDefaultRewardItemAsRewardItem];
}

- (NSDictionary *)getRewardItemDetailsWithKey:(NSString *)rewardItemKey {
  return [[UnityAds sharedInstance] getRewardItemDetailsWithKey:rewardItemKey];
}

- (BOOL)hideImpact {
  return [[UnityAds sharedInstance] hide];
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyImpact {
  [[UnityAds sharedInstance] setViewController:viewController];
}

- (void)stopAll{
}

#pragma mark - UnityAdsDelegate

- (void)unityAdsWillShow {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactWillOpen:)]) {
    [self.delegate applifierImpactWillOpen:self];
  }
}

- (void)unityAdsDidShow {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactDidOpen:)]) {
    [self.delegate applifierImpactDidOpen:self];
  }
}

- (void)unityAdsWillHide {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactWillClose:)]) {
    [self.delegate applifierImpactWillClose:self];
  }
}

- (void)unityAdsDidHide {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactDidClose:)]) {
    [self.delegate applifierImpactDidClose:self];
  }
}

- (void)unityAdsWillLeaveApplication {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactWillLeaveApplication:)]) {
    [self.delegate applifierImpactWillLeaveApplication:self];
  }
}

- (void)unityAdsVideoStarted {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactVideoStarted:)]) {
    [self.delegate applifierImpactVideoStarted:self];
  }
}

- (void)unityAdsFetchCompleted {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactCampaignsAreAvailable:)]) {
    [self.delegate applifierImpactCampaignsAreAvailable:self];
  }
}

- (void)unityAdsFetchFailed {
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(applifierImpactCampaignsFetchFailed:)]) {
    [self.delegate applifierImpactCampaignsFetchFailed:self];
  }
}

- (void)unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped {
  if (self.delegate != nil) {
    [self.delegate applifierImpact:self completedVideoWithRewardItemKey:rewardItemKey videoWasSkipped:skipped];
  }
}

@end