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
#import "UnityAdsMainViewController.h"

@interface UnityAds () <UnityAdsCampaignManagerDelegate, UIWebViewDelegate, UIScrollViewDelegate, UnityAdsMainViewControllerDelegate>
@property (nonatomic, strong) NSThread *backgroundThread;
@property (nonatomic, assign) dispatch_queue_t queue;
@end

@implementation UnityAds


#pragma mark - Static accessors

+ (BOOL)isSupported {
  if (kCFCoreFoundationVersionNumber < kCFCoreFoundationVersionNumber_iOS_5_0) {
    return NO;
  }
  
  return YES;
}

static UnityAds *sharedUnityAdsInstance = nil;

+ (UnityAds *)sharedInstance {
	@synchronized(self) {
		if (sharedUnityAdsInstance == nil) {
      sharedUnityAdsInstance = [[UnityAds alloc] init];
		}
	}
	
	return sharedUnityAdsInstance;
}


#pragma mark - Public

- (void)setTestMode:(BOOL)testModeEnabled {
  if (![UnityAds isSupported]) return;
  [[UnityAdsProperties sharedInstance] setTestModeEnabled:testModeEnabled];
}

- (void)startWithGameId:(NSString *)gameId {
  if (![UnityAds isSupported]) return;
  [self startWithGameId:gameId andViewController:nil];
}

- (void)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController {
  UAAssert([NSThread isMainThread]);
  UALOG_DEBUG(@"");
  if (![UnityAds isSupported]) return;
  if ([[UnityAdsProperties sharedInstance] adsGameId] != nil) return;
	if (gameId == nil || [gameId length] == 0) return;
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
  [notificationCenter addObserver:self selector:@selector(_notificationHandler:) name:UIApplicationWillEnterForegroundNotification object:nil];
	[[UnityAdsProperties sharedInstance] setAdsGameId:gameId];
	
  self.queue = dispatch_queue_create("com.unity3d.ads", NULL);
	 
	dispatch_async(self.queue, ^{
    self.backgroundThread = [[NSThread alloc] initWithTarget:self selector:@selector(_backgroundRunLoop:) object:nil];
		[self.backgroundThread start];

		[self performSelector:@selector(_startCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
		[self performSelector:@selector(_startAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
		
    dispatch_sync(dispatch_get_main_queue(), ^{
      [[UnityAdsMainViewController sharedInstance] setDelegate:self];
		});
	});
}

- (BOOL)canShow {
	UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) return NO;
	return [self _adViewCanBeShown];
}

- (BOOL)show {
  UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) return NO;
  if (![self canShow]) return NO;
  [[UnityAdsMainViewController sharedInstance] openAds:YES];
  return YES;
}

- (BOOL)hide {
  UAAssertV([NSThread mainThread], NO);
  if (![UnityAds isSupported]) NO;
  return [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:YES];
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds {
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
  
  BOOL openAnimated = NO;
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) {
    openAnimated = YES;
  }
  
  [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:NO];
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  if (applyAds && [self canShow]) {
    [[UnityAdsMainViewController sharedInstance] openAds:openAnimated];
  }
}

- (void)stopAll{
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
  [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(cancelAllDownloads) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}

- (void)trackInstall{
	UAAssert([NSThread isMainThread]);
  if (![UnityAds isSupported]) return;
	[[UnityAdsAnalyticsUploader sharedInstance] sendManualInstallTrackingCall];
}

- (void)dealloc {
  [[UnityAdsCampaignManager sharedInstance] setDelegate:nil];
  [[UnityAdsMainViewController sharedInstance] setDelegate:nil];
  [[UnityAdsWebAppController sharedInstance] setDelegate:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	dispatch_release(self.queue);
}


#pragma mark - Private uncategorized

- (void)_notificationHandler: (id) notification {
  NSString *name = [notification name];
  UALOG_DEBUG(@"Got notification from notificationCenter: %@", name);
  
  if ([name isEqualToString:UIApplicationWillEnterForegroundNotification]) {
    UAAssert([NSThread isMainThread]);
    
    if ([[UnityAdsMainViewController sharedInstance] mainControllerVisible]) {
      UALOG_DEBUG(@"Ad view visible, not refreshing.");
    }
    else {
      [self _refresh];
    }
  }
}

- (void)_backgroundRunLoop:(id)dummy {
	@autoreleasepool {
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while([[NSThread currentThread] isCancelled] == NO) {
			@autoreleasepool {
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:3.0]];
			}
		}
	}
}

- (BOOL)_adViewCanBeShown {
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil && [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 && [[UnityAdsCampaignManager sharedInstance] rewardItem] != nil && [[UnityAdsWebAppController sharedInstance] webViewInitialized])
		return YES;
	else
		return NO;
  
  return NO;
}


#pragma mark - Private initalization

- (void)_startCampaignManager {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
  [[UnityAdsCampaignManager sharedInstance] setDelegate:self];
	[self _refreshCampaignManager];
}

- (void)_startAnalyticsUploader {
	UAAssert(![NSThread isMainThread]);
	UALOG_DEBUG(@"");
	[[UnityAdsAnalyticsUploader sharedInstance] retryFailedUploads];
}


#pragma mark - Private data refreshing

- (void)_refresh {
	if ([[UnityAdsProperties sharedInstance] adsGameId] == nil) {
		UALOG_ERROR(@"Unity Ads has not been started properly. Launch with -startWithGameId: first.");
		return;
	}
	
	UALOG_DEBUG(@"");
	dispatch_async(self.queue, ^{
		[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
		[self performSelector:@selector(_refreshCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
    [[UnityAdsAnalyticsUploader sharedInstance] performSelector:@selector(retryFailedUploads) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	});
}

- (void)_refreshCampaignManager {
	UAAssert(![NSThread isMainThread]);
	[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
	[[UnityAdsCampaignManager sharedInstance] updateCampaigns];
}


#pragma mark - UnityAdsCampaignManagerDelegate

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem gamerID:(NSString *)gamerID {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	[self _notifyDelegateOfCampaignAvailability];
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

 
#pragma mark - UnityAdsViewManagerDelegate

- (void)mainControllerStartedPlayingVideo {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if ([self.delegate respondsToSelector:@selector(unityAdsVideoStarted:)])
		[self.delegate unityAdsVideoStarted:self];
}

- (void)mainControllerVideoEnded {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	[self.delegate unityAds:self completedVideoWithRewardItemKey:[[UnityAdsCampaignManager sharedInstance] rewardItem].key];
}

- (void)mainControllerWillCloseAdView {
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if ([self.delegate respondsToSelector:@selector(unityAdsWillHide:)])
		[self.delegate unityAdsWillHide:self];
}

- (void)mainControllerWebViewInitialized {
	UAAssert([NSThread isMainThread]);	
	UALOG_DEBUG(@"");

	[self _notifyDelegateOfCampaignAvailability];
}


#pragma mark - UnityAdsDelegate calling methods

- (void)_notifyDelegateOfCampaignAvailability {
	if ([self _adViewCanBeShown]) {
		if ([self.delegate respondsToSelector:@selector(unityAdsFetchCompleted:)])
			[self.delegate unityAdsFetchCompleted:self];
	}
}

@end