//
//  UnityAdsiOS4.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsiOS4.h"
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "UnityAdsCampaign/UnityAdsCampaign.h"
#import "UnityAdsCampaign/UnityAdsRewardItem.h"
#import "UnityAdsOpenUDID/UnityAdsOpenUDID.h"
#import "UnityAdsData/UnityAdsAnalyticsUploader.h"
//#import "UnityAdsViewManager.h"
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsProperties/UnityAdsProperties.h"

#import "UnityAdsMainViewController.h"

@interface UnityAdsiOS4 () <UnityAdsCampaignManagerDelegate, UIWebViewDelegate, UIScrollViewDelegate, UnityAdsMainViewControllerDelegate>
@property (nonatomic, strong) NSThread *backgroundThread;
@property (nonatomic, assign) dispatch_queue_t queue;
@end

@implementation UnityAdsiOS4


#pragma mark - Private

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

- (void)_refreshCampaignManager {
	UAAssert(![NSThread isMainThread]);
	[[UnityAdsProperties sharedInstance] refreshCampaignQueryString];
	[[UnityAdsCampaignManager sharedInstance] updateCampaigns];
}

- (void)_startCampaignManager {
	UAAssert(![NSThread isMainThread]);
	
  [[UnityAdsCampaignManager sharedInstance] setDelegate:self];
	[self _refreshCampaignManager];
}

- (void)_startAnalyticsUploader {
	UAAssert(![NSThread isMainThread]);
	[[UnityAdsAnalyticsUploader sharedInstance] retryFailedUploads];
}

- (BOOL)_adViewCanBeShown {
  if ([[UnityAdsCampaignManager sharedInstance] campaigns] != nil && [[[UnityAdsCampaignManager sharedInstance] campaigns] count] > 0 && [[UnityAdsCampaignManager sharedInstance] rewardItem] != nil && [[UnityAdsWebAppController sharedInstance] webViewInitialized])
		return YES;
	else
		return NO;
  
  return NO;
}

- (void)_notifyDelegateOfCampaignAvailability {
	if ([self _adViewCanBeShown]) {
		if ([self.delegate respondsToSelector:@selector(unityAdsFetchCompleted:)])
			[self.delegate unityAdsFetchCompleted:self];
	}
}

- (void)_trackInstall {
	if ([[UnityAdsProperties sharedInstance] adsGameId] == nil) {
		UALOG_ERROR(@"Unity Ads has not been started properly. Launch with -startWithGameId: first.");
		return;
	}
	
	dispatch_async(self.queue, ^{
    // FIX
    NSString *queryString = [NSString stringWithFormat:@"%@/install", [[UnityAdsProperties sharedInstance] adsGameId]];
    NSString *bodyString = [NSString stringWithFormat:@"deviceId=%@", [UnityAdsDevice md5DeviceId]];
		NSDictionary *queryDictionary = @{ kUnityAdsQueryDictionaryQueryKey : queryString, kUnityAdsQueryDictionaryBodyKey : bodyString };
    [[UnityAdsAnalyticsUploader sharedInstance] performSelector:@selector(sendInstallTrackingCallWithQueryDictionary:) onThread:self.backgroundThread withObject:queryDictionary waitUntilDone:NO];
	});
}

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

#pragma mark - Public

- (void)setTestMode:(BOOL)testModeEnabled {
  [[UnityAdsProperties sharedInstance] setTestModeEnabled:testModeEnabled];
}

- (void)startWithGameId:(NSString *)gameId {
  [self startWithGameId:gameId andViewController:nil];
}

- (void)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController {
  UAAssert([NSThread isMainThread]);
  
	if (gameId == nil || [gameId length] == 0) {
		UALOG_ERROR(@"gameId empty or not set.");
		return;
	}
  
  if ([[UnityAdsProperties sharedInstance] adsGameId] != nil) {
    return;
  }
  
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
  [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationWillEnterForegroundNotification object:nil];
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

- (void)notificationHandler: (id) notification {
  NSString *name = [notification name];
  
  UALOG_DEBUG(@"notification: %@", name);
  
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

/*
- (UIView *)adsView {
	UAAssertV([NSThread mainThread], nil);
	
  UnityAdsMainViewController *adViewController = nil;
  adViewController = [[UnityAdsMainViewController alloc] initWithNibName:nil bundle:[NSBundle mainBundle]];
  [[[UnityAdsProperties sharedInstance] currentViewController].view.window.rootViewController presentViewController:adViewController animated:NO completion:nil];
	if ([self _adViewCanBeShown]) {
		UIView *adView = [[UnityAdsViewManager sharedInstance] adView];
		if (adView != nil) {
			if ([self.delegate respondsToSelector:@selector(unityAdsWillShow:)])
				[self.delegate unityAdsWillShow:self];

			return adView;
		}
	}
	
	return nil;
}*/

- (BOOL)canShow {
	UAAssertV([NSThread mainThread], NO);
	return [self _adViewCanBeShown];
}

- (BOOL)show {
  UAAssertV([NSThread mainThread], NO);
  /*
  return [[UnityAdsViewManager sharedInstance] applyAdViewToCurrentViewController];
  */
  /*
  [[[UnityAdsProperties sharedInstance] currentViewController].view.window.rootViewController presentViewController:[UnityAdsMainViewController sharedInstance] animated:NO completion:nil];*/
  [[UnityAdsMainViewController sharedInstance] openAds];
  
  return YES;
}

- (BOOL)hide {
  UAAssertV([NSThread mainThread], NO);
  return [[UnityAdsMainViewController sharedInstance] closeAds];
}

- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds {
  [[UnityAdsMainViewController sharedInstance] closeAds];
  [[UnityAdsProperties sharedInstance] setCurrentViewController:viewController];
  
  if (applyAds) {
    [[UnityAdsMainViewController sharedInstance] openAds];
  }
}

- (void)stopAll{
	UAAssert([NSThread isMainThread]);
  [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(cancelAllDownloads) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}

- (void)trackInstall{
	UAAssert([NSThread isMainThread]);
	[self _trackInstall];
}

- (void)dealloc {
  UALOG_DEBUG(@"");
  [[UnityAdsCampaignManager sharedInstance] setDelegate:nil];
  [[UnityAdsMainViewController sharedInstance] setDelegate:nil];
	//[[UnityAdsViewManager sharedInstance] setDelegate:nil];
  [[UnityAdsWebAppController sharedInstance] setDelegate:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	dispatch_release(self.queue);
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

@end