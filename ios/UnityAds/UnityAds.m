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

NSString * const kUnityAdsRewardItemPictureKey = @"picture";
NSString * const kUnityAdsRewardItemNameKey = @"name";
NSString * const kUnityAdsOptionNoOfferscreenKey = @"noOfferScreen";
NSString * const kUnityAdsOptionOpenAnimatedKey = @"openAnimated";
NSString * const kUnityAdsOptionGamerSIDKey = @"sid";

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
  
  NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
  [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationWillEnterForegroundNotification object:nil];
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
	[[UnityAdsProperties sharedInstance] setAdsGameId:gameId];
  [[UnityAdsMainViewController sharedInstance] setDelegate:self];
  
  self.initializer = [[UnityAdsDefaultInitializer alloc] init];
  [self.initializer setDelegate:self];
  [self.initializer initAds:nil];
  
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
  UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) return NO;
  if (![self canShow]) return NO;
  
  UnityAdsViewStateType state = kUnityAdsViewStateTypeOfferScreen;
  [[UnityAdsShowOptionsParser sharedInstance] parseOptions:options];
  
  if ([[UnityAdsShowOptionsParser sharedInstance] noOfferScreen]) {
    if (![self canShowAds]) return NO;
    state = kUnityAdsViewStateTypeVideoPlayer;
  }
  
  [[UnityAdsMainViewController sharedInstance] openAds:[[UnityAdsShowOptionsParser sharedInstance] openAnimated] inState:state withOptions:options];
  
  return YES;
}

- (BOOL)show {
  UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) return NO;
  if (![self canShow]) return NO;
  [[UnityAdsMainViewController sharedInstance] openAds:YES inState:kUnityAdsViewStateTypeOfferScreen withOptions:nil];
  return YES;
}

- (BOOL)hasMultipleRewardItems {
  if ([[UnityAdsCampaignManager sharedInstance] rewardItems] != nil && [[[UnityAdsCampaignManager sharedInstance] rewardItems] count] > 0) {
    return YES;
  }
  
  return NO;
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
  
  return NO;
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
  UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) NO;
  return [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:YES withOptions:nil];
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds {
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
  
  BOOL openAnimated = NO;
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) {
    openAnimated = YES;
  }
  
  [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:NO withOptions:nil];
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
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil && [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 && [[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem] != nil && self.initializer != nil && [self.initializer initWasSuccessfull])
		return YES;
	else
		return NO;
  
  return NO;
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
    [self.delegate unityAds:self completedVideoWithRewardItemKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key];
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