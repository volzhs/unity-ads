//
//  UnityAds.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "UnityAdsCampaign/UnityAdsCampaign.h"
#import "UnityAdsCampaign/UnityAdsRewardItem.h"
#import "UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsProperties/UnityAdsProperties.h"
#import "UnityAdsView/UnityAdsMainViewController.h"
#import "UnityAdsProperties/UnityAdsShowOptionsParser.h"

#import "UnityAdsInitializer/UnityAdsDefaultInitializer.h"
#import "UnityAdsInitializer/UnityAdsNoWebViewInitializer.h"

NSString * const kUnityAdsRewardItemPictureKey = @"picture";
NSString * const kUnityAdsRewardItemNameKey = @"name";

NSString * const kUnityAdsOptionNoOfferscreenKey = @"noOfferScreen";
NSString * const kUnityAdsOptionOpenAnimatedKey = @"openAnimated";
NSString * const kUnityAdsOptionGamerSIDKey = @"sid";
NSString * const kUnityAdsOptionMuteVideoSounds = @"muteVideoSounds";
NSString * const kUnityAdsOptionVideoUsesDeviceOrientation = @"useDeviceOrientationForVideo";

@interface UnityAds () <UnityAdsInitializerDelegate, UnityAdsMainViewControllerDelegate>
  @property (nonatomic, strong) UnityAdsInitializer *initializer;
  @property (nonatomic, assign) UnityAdsMode mode;
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

- (void)setAdsMode:(UnityAdsMode)adsMode {
  self.mode = adsMode;
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
  if ([self.delegate respondsToSelector:@selector(unityAdsFetchFailed:)])
    [self.delegate unityAdsFetchFailed:self];
}


#pragma mark - Public

- (void)setTestMode:(BOOL)testModeEnabled {
  if (![UnityAds isSupported]) return;
  [[UnityAdsProperties sharedInstance] setTestModeEnabled:testModeEnabled];
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
  
  self.initializer = [self selectInitializerFromMode:self.mode];
  
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

- (BOOL)show:(NSDictionary *)options {
  UAAssertV([NSThread mainThread], false);
  if (![UnityAds isSupported]) return false;
  if (![self canShow]) return false;
  
  UnityAdsViewStateType state = kUnityAdsViewStateTypeOfferScreen;
  [[UnityAdsShowOptionsParser sharedInstance] parseOptions:options];
  
  // If Unity Ads is in "No WebView" -mode, always skip offerscreen
  if (self.mode == kUnityAdsModeNoWebView)
    [[UnityAdsShowOptionsParser sharedInstance] setNoOfferScreen:true];
  
  if ([[UnityAdsShowOptionsParser sharedInstance] noOfferScreen]) {
    if (![self canShowAds]) return false;
    state = kUnityAdsViewStateTypeVideoPlayer;
  }
  
  [[UnityAdsMainViewController sharedInstance] openAds:[[UnityAdsShowOptionsParser sharedInstance] openAnimated] inState:state withOptions:options];
  
  return true;
}

- (BOOL)show {
  return [self show:nil];
}

- (BOOL)hasMultipleRewardItems {
  if ([[UnityAdsCampaignManager sharedInstance] rewardItems] != nil && [[[UnityAdsCampaignManager sharedInstance] rewardItems] count] > 0) {
    return true;
  }
  
  return false;
}

- (NSArray *)getRewardItemKeys {
  return [[UnityAdsCampaignManager sharedInstance] rewardItemKeys];
}

- (NSString *)getDefaultRewardItemKey {
  return [[UnityAdsCampaignManager sharedInstance] defaultRewardItem].key;
}

- (NSString *)getCurrentRewardItemKey {
  return [[UnityAdsCampaignManager sharedInstance] currentRewardItemKey];
}

- (BOOL)setRewardItemKey:(NSString *)rewardItemKey {
  if (![[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
    return [[UnityAdsCampaignManager sharedInstance] setSelectedRewardItemKey:rewardItemKey];
  }
  
  return false;
}

- (void)setDefaultRewardItemAsRewardItem {
  [[UnityAdsCampaignManager sharedInstance] setSelectedRewardItemKey:[self getDefaultRewardItemKey]];
}

- (NSDictionary *)getRewardItemDetailsWithKey:(NSString *)rewardItemKey {
  if ([self hasMultipleRewardItems] && rewardItemKey != nil) {
    return [[UnityAdsCampaignManager sharedInstance] getPublicRewardItemDetails:rewardItemKey];
  }
  
  return nil;
}

- (BOOL)hide {
  UAAssertV([NSThread mainThread], false);
  if (![UnityAds isSupported]) false;
  return [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:YES withOptions:nil];
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds {
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
  
  BOOL openAnimated = false;
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) {
    openAnimated = YES;
  } else {
    if([[UnityAdsMainViewController sharedInstance] isOpen]) {
      [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:NO withOptions:nil];
    }
  }
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  if (applyAds && [self canShow]) {
    [[UnityAdsMainViewController sharedInstance] openAds:openAnimated inState:kUnityAdsViewStateTypeOfferScreen withOptions:nil];
  }
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
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil && [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 && [[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem] != nil && self.initializer != nil && [self.initializer initWasSuccessfull]) {
		return true;
  }
  
  return false;
}

- (UnityAdsInitializer *)selectInitializerFromMode:(UnityAdsMode)mode {
  switch (mode) {
    case kUnityAdsModeDefault:
      return [[UnityAdsDefaultInitializer alloc] init];
    case kUnityAdsModeNoWebView:
      return [[UnityAdsNoWebViewInitializer alloc] init];
  }
  
  return nil;
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
	
	if ([self.delegate respondsToSelector:@selector(unityAdsWillHide:)])
		[self.delegate unityAdsWillHide:self];
}

- (void)mainControllerDidClose {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if ([self.delegate respondsToSelector:@selector(unityAdsDidHide:)])
		[self.delegate unityAdsDidHide:self];
}

- (void)mainControllerWillOpen {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if ([self.delegate respondsToSelector:@selector(unityAdsWillShow:)])
		[self.delegate unityAdsWillShow:self];
}

- (void)mainControllerDidOpen {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if ([self.delegate respondsToSelector:@selector(unityAdsDidShow:)])
		[self.delegate unityAdsDidShow:self];
}

- (void)mainControllerStartedPlayingVideo {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if ([self.delegate respondsToSelector:@selector(unityAdsVideoStarted:)])
		[self.delegate unityAdsVideoStarted:self];
}

- (void)mainControllerVideoEnded {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
  if (![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
    [self.delegate unityAdsVideoCompleted:self rewardItemKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key skipped:FALSE];
  }
}

- (void)mainControllerVideoSkipped {
  UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
  if (![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
    [self.delegate unityAdsVideoCompleted:self rewardItemKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key skipped:TRUE];
  }
}

- (void)mainControllerWillLeaveApplication {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
  
  if ([self.delegate respondsToSelector:@selector(unityAdsWillLeaveApplication:)])
		[self.delegate unityAdsWillLeaveApplication:self];
}


#pragma mark - UnityAdsDelegate calling methods

- (void)notifyDelegateOfCampaignAvailability {
	if ([self adsCanBeShown]) {
		if ([self.delegate respondsToSelector:@selector(unityAdsFetchCompleted:)])
			[self.delegate unityAdsFetchCompleted:self];
	}
}

@end