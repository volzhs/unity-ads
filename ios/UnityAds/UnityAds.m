//
//  UnityAds.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsCampaignManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"
#import "UnityAdsAnalyticsUploader.h"
#import "UnityAdsDevice.h"
#import "UnityAdsProperties.h"
#import "UnityAdsMainViewController.h"
#import "UnityAdsZoneManager.h"
#import "UnityAdsIncentivizedZone.h"
#import "UnityAdsDefaultInitializer.h"

NSString * const kUnityAdsRewardItemPictureKey = @"picture";
NSString * const kUnityAdsRewardItemNameKey = @"name";

NSString * const kUnityAdsOptionNoOfferscreenKey = @"noOfferScreen";
NSString * const kUnityAdsOptionOpenAnimatedKey = @"openAnimated";
NSString * const kUnityAdsOptionGamerSIDKey = @"sid";
NSString * const kUnityAdsOptionMuteVideoSounds = @"muteVideoSounds";
NSString * const kUnityAdsOptionVideoUsesDeviceOrientation = @"useDeviceOrientationForVideo";

@interface UnityAds () <UnityAdsInitializerDelegate, UnityAdsMainViewControllerDelegate>
  @property (nonatomic, strong) UnityAdsInitializer *initializer;
  @property (nonatomic, assign) Boolean debug;
@end

@implementation UnityAds


#pragma mark - Static accessors

+ (BOOL)isSupported {
  if (kCFCoreFoundationVersionNumber < kCFCoreFoundationVersionNumber_iOS_5_0) {
    return NO;
  }
  
  return YES;
}

- (void)setTestDeveloperId:(NSString *)developerId {
  [[UnityAdsProperties sharedInstance] setDeveloperId:developerId];
}

- (void)setTestOptionsId:(NSString *)optionsId {
  [[UnityAdsProperties sharedInstance] setOptionsId:optionsId];
}


+ (NSString *)getSDKVersion {
  return [[UnityAdsProperties sharedInstance] adsVersion];
}

- (void)setDebugMode:(BOOL)debugMode {
  self.debug = debugMode;
}

- (BOOL)isDebugMode {
  return self.debug;
}

static UnityAds *sharedUnityAdsInstance = nil;

+ (UnityAds *)sharedInstance {
	@synchronized(self) {
		if (sharedUnityAdsInstance == nil) {
      sharedUnityAdsInstance = [[UnityAds alloc] init];
      sharedUnityAdsInstance.debug = NO;
		}
	}
	
	return sharedUnityAdsInstance;
}


#pragma mark - Init delegates

- (void)initComplete {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
	[self notifyDelegateOfCampaignAvailability];
}

- (void)initFailed {
	UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"");
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsFetchFailed)])
    [self.delegate unityAdsFetchFailed];
}


#pragma mark - Public

- (void)setTestMode:(BOOL)testModeEnabled {
  if (![UnityAds isSupported]) return;
  [[UnityAdsProperties sharedInstance] setTestModeEnabled:testModeEnabled];
}

- (void)enableUnityDeveloperInternalTestMode {
  [[UnityAdsProperties sharedInstance] enableUnityDeveloperInternalTestMode];
}

- (BOOL)startWithGameId:(NSString *)gameId {
  if (![UnityAds isSupported]) return false;
  return [self startWithGameId:gameId andViewController:nil];
}

- (BOOL)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController {
  UALOG_DEBUG(@"");
  if (![UnityAds isSupported]) return false;
  if ([[UnityAdsProperties sharedInstance] adsGameId] != nil) return false;
	if (gameId == nil || [gameId length] == 0) return false;
  if (self.initializer != nil) return false;
  
  NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
  [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationWillEnterForegroundNotification object:nil];
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
	[[UnityAdsProperties sharedInstance] setAdsGameId:gameId];
  [[UnityAdsMainViewController sharedInstance] setDelegate:self];
  
  self.initializer = [[UnityAdsDefaultInitializer alloc] init];
  
  if (self.initializer != nil) {
    [self.initializer setDelegate:self];
    [self.initializer initAds:nil];
  }
  else {
    UALOG_DEBUG(@"Initializer is null, cannot start Unity Ads");
    return false;
  }
  
  return true;
}

- (BOOL)canShowAds {
  if ([self canShow] && [[[UnityAdsCampaignManager sharedInstance] getViewableCampaigns] count] > 0) {
    return YES;
  }
  
  return NO;
}

- (BOOL)canShow {
	UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) return NO;
	return [self adsCanBeShown];
}

- (BOOL)setZone:(NSString *)zoneId {
  if (![[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
    return [[UnityAdsZoneManager sharedInstance] setCurrentZone:zoneId];
  }
  return FALSE;
}

- (BOOL)setZone:(NSString *)zoneId withRewardItem:(NSString *)rewardItemKey {
  if([self setZone:zoneId]) {
    return [self setRewardItemKey:rewardItemKey];
  }
  return FALSE;
}

- (BOOL)show:(NSDictionary *)options {
  UAAssertV([NSThread mainThread], false);
  if (![UnityAds isSupported]) return false;
  if (![self canShow]) return false;
  
  UnityAdsViewStateType state = kUnityAdsViewStateTypeOfferScreen;
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil) {
    [currentZone mergeOptions:options];
    
    if ([currentZone noOfferScreen]) {
      if (![self canShowAds]) return false;
      state = kUnityAdsViewStateTypeVideoPlayer;
    }
    
    [[UnityAdsMainViewController sharedInstance] openAds:[currentZone openAnimated] inState:state withOptions:options];
    
    return true;
  } else {
    return false;
  }
}

- (BOOL)show {
  return [self show:nil];
}

- (BOOL)hasMultipleRewardItems {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil && [currentZone isIncentivized]) {
    id rewardManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
    if(rewardManager != nil && [rewardManager itemCount] > 1) {
      return TRUE;
    }
  }
  return FALSE;
}

- (NSArray *)getRewardItemKeys {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil && [currentZone isIncentivized]) {
    return [[((UnityAdsIncentivizedZone *)currentZone) itemManager] allItems];
  }
  return nil;
}

- (NSString *)getDefaultRewardItemKey {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil && [currentZone isIncentivized]) {
    return [[((UnityAdsIncentivizedZone *)currentZone) itemManager] getDefaultItem].key;
  }
  return nil;
}

- (NSString *)getCurrentRewardItemKey {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil && [currentZone isIncentivized]) {
    return [[((UnityAdsIncentivizedZone *)currentZone) itemManager] getCurrentItem].key;
  }
  return nil;

}

- (BOOL)setRewardItemKey:(NSString *)rewardItemKey {
  if (![[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
    id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
    if(currentZone != nil && [currentZone isIncentivized]) {
      return [[((UnityAdsIncentivizedZone *)currentZone) itemManager] setCurrentItem:rewardItemKey];
    }
  }
  return false;
}

- (void)setDefaultRewardItemAsRewardItem {
  if (![[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
    id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
    if(currentZone != nil && [currentZone isIncentivized]) {
      id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
      [itemManager setCurrentItem:[itemManager getDefaultItem].key];
    }
  }
}

- (NSDictionary *)getRewardItemDetailsWithKey:(NSString *)rewardItemKey {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if(currentZone != nil && [currentZone isIncentivized]) {
    id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
    id item = [itemManager getItem:rewardItemKey];
    if(item != nil) {
      return [item getDetails];
    }
  }
  return nil;
}

- (BOOL)hide {
  UAAssertV([NSThread mainThread], false);
  if (![UnityAds isSupported]) false;
  return [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:YES withOptions:nil];
}

- (BOOL)setViewController:(UIViewController *)viewController {
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported] || ![self canShow]) return false;
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  return true;
}

- (void)stopAll{
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
  if (self.initializer != nil) {
    [self.initializer deInitialize];
  }
}

- (void)dealloc {
	[[NSNotificationCenter defaultCenter] removeObserver:self];
  
  [[UnityAdsCampaignManager sharedInstance] setDelegate:nil];
  [[UnityAdsMainViewController sharedInstance] setDelegate:nil];
  
  if (self.initializer != nil) {
    [self.initializer setDelegate:nil];
  }
}


#pragma mark - Private uncategorized

- (void)notificationHandler:(id)notification {
  NSString *name = [notification name];
  UALOG_DEBUG(@"Got notification from notificationCenter: %@", name);
  
  if ([name isEqualToString:UIApplicationWillEnterForegroundNotification]) {
    UAAssert([NSThread isMainThread]);
    
    if ([[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
      UALOG_DEBUG(@"Ad view visible, not refreshing.");
    }
    else {
      [self refreshAds];
    }
  }
}

- (BOOL)adsCanBeShown {
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil && [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 && self.initializer != nil && [self.initializer initWasSuccessfull]) {
		return true;
  }
  return false;
}

#pragma mark - Private data refreshing

- (void)refreshAds {
	if ([[UnityAdsProperties sharedInstance] adsGameId] == nil) {
		UALOG_ERROR(@"Unity Ads has not been started properly. Launch with -startWithGameId: first.");
		return;
	}
	
  if (self.initializer != nil) {
    [self.initializer reInitialize];
  }
}


#pragma mark - UnityAdsViewManagerDelegate

- (void)mainControllerWillClose {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsWillHide)])
		[self.delegate unityAdsWillHide];
}

- (void)mainControllerDidClose {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsDidHide)])
		[self.delegate unityAdsDidHide];
}

- (void)mainControllerWillOpen {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsWillShow)])
		[self.delegate unityAdsWillShow];
}

- (void)mainControllerDidOpen {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsDidShow)])
		[self.delegate unityAdsDidShow];
}

- (void)mainControllerStartedPlayingVideo {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsVideoStarted)])
		[self.delegate unityAdsVideoStarted];
}

- (void)mainControllerVideoEnded {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
  if (![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
    
    if (self.delegate != nil) {
      NSString *key = nil;
      id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
      if([currentZone isIncentivized]) {
        id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
        key = [itemManager getCurrentItem].key;
      }
      [self.delegate unityAdsVideoCompleted:key skipped:FALSE];
    }
  }
}

- (void)mainControllerVideoSkipped {
  UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
  if (![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
    
    if (self.delegate != nil) {
      NSString *key = nil;
      id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
      if([currentZone isIncentivized]) {
        id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
        key = [itemManager getCurrentItem].key;
      }
      [self.delegate unityAdsVideoCompleted:key skipped:TRUE];
    }
  }
}

- (void)mainControllerWillLeaveApplication {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsWillLeaveApplication)])
		[self.delegate unityAdsWillLeaveApplication];
}


#pragma mark - UnityAdsDelegate calling methods

- (void)notifyDelegateOfCampaignAvailability {
	if ([self adsCanBeShown]) {
		if (self.delegate != nil && [self.delegate respondsToSelector:@selector(unityAdsFetchCompleted)])
			[self.delegate unityAdsFetchCompleted];
	}
}

@end