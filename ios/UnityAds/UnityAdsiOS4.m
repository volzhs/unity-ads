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
#import "UnityAdsViewManager.h"
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsProperties/UnityAdsProperties.h"

NSString * const kUnityAdsVersion = @"1.0";

@interface UnityAdsiOS4 () <UnityAdsCampaignManagerDelegate, UIWebViewDelegate, UIScrollViewDelegate, UnityAdsViewManagerDelegate>
@property (nonatomic, strong) UnityAdsCampaignManager *campaignManager;
@property (nonatomic, strong) UnityAdsAnalyticsUploader *analyticsUploader;
@property (nonatomic, strong) UnityAdsRewardItem *rewardItem;
@property (nonatomic, strong) NSString *gameId;
@property (nonatomic, strong) NSString *campaignJSON;
@property (nonatomic, strong) NSString *machineName;
@property (nonatomic, strong) NSString *md5AdvertisingIdentifier;
@property (nonatomic, strong) NSString *md5DeviceId;
@property (nonatomic, strong) NSString *md5MACAddress;
@property (nonatomic, strong) NSString *md5OpenUDID;
@property (nonatomic, strong) NSString *campaignQueryString;
@property (nonatomic, strong) NSString *gamerID;
@property (nonatomic, strong) NSString *connectionType;
@property (nonatomic, strong) NSThread *backgroundThread;
@property (nonatomic, strong) NSArray *campaigns;
@property (nonatomic, assign) BOOL webViewInitialized;
@property (nonatomic, assign) dispatch_queue_t queue;
@end

@implementation UnityAdsiOS4

#pragma mark - Private

- (NSString *)_queryString
{
	NSString *advertisingIdentifier = self.md5AdvertisingIdentifier != nil ? self.md5AdvertisingIdentifier : @"";
  NSString *queryParams = @"?";
  
  queryParams = [NSString stringWithFormat:@"%@deviceId=%@&platform=%@", queryParams, self.md5DeviceId, @"ios"];
  
  if (self.md5AdvertisingIdentifier != nil)
    queryParams = [NSString stringWithFormat:@"%@&advertisingTrackingId=%@", queryParams, advertisingIdentifier];

  if ([UnityAdsDevice canUseTracking]) {
    queryParams = [NSString stringWithFormat:@"%@&softwareVersion=%@&hardwareVersion=%@&deviceType=%@&apiVersion=%@&connectionType=%@", queryParams, [[UIDevice currentDevice] systemVersion], @"unknown", self.machineName, kUnityAdsVersion, self.connectionType];
    if (self.md5AdvertisingIdentifier == nil) {
      queryParams = [NSString stringWithFormat:@"%@&macAddress=%@&openUdid=%@", queryParams, self.md5MACAddress, self.md5OpenUDID];
    }
  }

  return queryParams;
}

- (void)_backgroundRunLoop:(id)dummy
{
	@autoreleasepool
	{
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while([[NSThread currentThread] isCancelled] == NO)
		{
			@autoreleasepool
			{
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:3.0]];
			}
		}
	}
}

- (void)_refreshCampaignManager
{
	UAAssert( ! [NSThread isMainThread]);
	UAAssert(self.campaignManager != nil);
	
	self.campaignManager.queryString = self.campaignQueryString;
	[self.campaignManager updateCampaigns];
}

- (void)_startCampaignManager
{
	UAAssert( ! [NSThread isMainThread]);
	
	self.campaignManager = [[UnityAdsCampaignManager alloc] init];
	self.campaignManager.delegate = self;
	[self _refreshCampaignManager];
}

- (void)_startAnalyticsUploader
{
	UAAssert( ! [NSThread isMainThread]);
	
	self.analyticsUploader = [[UnityAdsAnalyticsUploader alloc] init];
	[self.analyticsUploader retryFailedUploads];
}

- (void)_logVideoAnalyticsWithPosition:(VideoAnalyticsPosition)videoPosition campaign:(UnityAdsCampaign *)campaign
{
	if (campaign == nil)
	{
		UALOG_DEBUG(@"Campaign is nil.");
		return;
	}
	
	dispatch_async(self.queue, ^{
		NSString *positionString = nil;
		NSString *trackingString = nil;
		if (videoPosition == kVideoAnalyticsPositionStart)
		{
			positionString = @"video_start";
			trackingString = @"start";
		}
		else if (videoPosition == kVideoAnalyticsPositionFirstQuartile)
			positionString = @"first_quartile";
		else if (videoPosition == kVideoAnalyticsPositionMidPoint)
			positionString = @"mid_point";
		else if (videoPosition == kVideoAnalyticsPositionThirdQuartile)
			positionString = @"third_quartile";
		else if (videoPosition == kVideoAnalyticsPositionEnd)
		{
			positionString = @"video_end";
			trackingString = @"view";
		}
		
		NSString *query = [NSString stringWithFormat:@"applicationId=%@&type=%@&trackingId=%@&providerId=%@", self.gameId, positionString, self.gamerID, campaign.id];
		
		[self.analyticsUploader performSelector:@selector(sendViewReportWithQueryString:) onThread:self.backgroundThread withObject:query waitUntilDone:NO];
		
		if (trackingString != nil)
		{
			NSString *trackingQuery = [NSString stringWithFormat:@"%@/%@/%@?gameId=%@", self.gamerID, trackingString, campaign.id, self.gameId];
			[self.analyticsUploader performSelector:@selector(sendTrackingCallWithQueryString:) onThread:self.backgroundThread withObject:trackingQuery waitUntilDone:NO];
		}
	});
}

- (BOOL)_adViewCanBeShown
{
	if (self.campaigns != nil && [self.campaigns count] > 0 && self.rewardItem != nil && self.webViewInitialized)
		return YES;
	else
		return NO;
}

- (void)_notifyDelegateOfCampaignAvailability
{
	if ([self _adViewCanBeShown])
	{
		if ([self.delegate respondsToSelector:@selector(unityAdsFetchCompleted:)])
			[self.delegate unityAdsFetchCompleted:self];
	}
}

- (void)_trackInstall
{
	if (self.gameId == nil)
	{
		UALOG_ERROR(@"Unity Ads has not been started properly. Launch with -startWithGameId: first.");
		return;
	}
	
	dispatch_async(self.queue, ^{
		NSString *queryString = [NSString stringWithFormat:@"%@/install", self.gameId];
		NSString *bodyString = [NSString stringWithFormat:@"deviceId=%@", self.md5DeviceId];
		NSDictionary *queryDictionary = @{ kUnityAdsQueryDictionaryQueryKey : queryString, kUnityAdsQueryDictionaryBodyKey : bodyString };
		
		[self.analyticsUploader performSelector:@selector(sendInstallTrackingCallWithQueryDictionary:) onThread:self.backgroundThread withObject:queryDictionary waitUntilDone:NO];
	});
}

- (void)_refresh
{
	if (self.gameId == nil)
	{
		UALOG_ERROR(@"Unity Ads has not been started properly. Launch with -startWithGameId: first.");
		return;
	}
	
	UALOG_DEBUG(@"");
	
	dispatch_async(self.queue, ^{
		self.connectionType = [UnityAdsDevice currentConnectionType];
		self.campaignQueryString = [self _queryString];
		
		[self performSelector:@selector(_refreshCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
		[self.analyticsUploader performSelector:@selector(retryFailedUploads) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	});
}

#pragma mark - Public

- (void)startWithGameId:(NSString *)gameId
{
	UAAssert([NSThread isMainThread]);
	
	if (gameId == nil || [gameId length] == 0)
	{
		UALOG_ERROR(@"gameId empty or not set.");
		return;
	}
	
	if (self.gameId != nil)
		return;
	
	self.gameId = gameId;
	self.queue = dispatch_queue_create("com.unity3d.ads", NULL);
	
	dispatch_async(self.queue, ^{
		self.machineName = [UnityAdsDevice analyticsMachineName];
		self.md5AdvertisingIdentifier = [UnityAdsDevice md5AdvertisingIdentifierString];
		self.md5MACAddress = [UnityAdsDevice md5MACAddressString];
		self.md5OpenUDID = [UnityAdsDevice md5OpenUDIDString];
		self.connectionType = [UnityAdsDevice currentConnectionType];
    self.md5DeviceId = self.md5AdvertisingIdentifier != nil ? self.md5AdvertisingIdentifier : self.md5OpenUDID;
		self.campaignQueryString = [self _queryString];
		
		self.backgroundThread = [[NSThread alloc] initWithTarget:self selector:@selector(_backgroundRunLoop:) object:nil];
		[self.backgroundThread start];

		[self performSelector:@selector(_startCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
		[self performSelector:@selector(_startAnalyticsUploader) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
		
    dispatch_sync(dispatch_get_main_queue(), ^{
      [[UnityAdsViewManager sharedInstance] setDelegate:self];
      [[UnityAdsViewManager sharedInstance] setMachineName:self.machineName];
      [[UnityAdsViewManager sharedInstance] setMd5AdvertisingIdentifier:self.md5AdvertisingIdentifier];
      [[UnityAdsViewManager sharedInstance] setMd5DeviceId:self.md5DeviceId];
      [[UnityAdsViewManager sharedInstance] setMd5MACAddress:self.md5MACAddress];
      [[UnityAdsViewManager sharedInstance] setMd5OpenUDID:self.md5OpenUDID];
		});
	});
}

- (UIView *)adsView
{
	UAAssertV([NSThread mainThread], nil);
	
	if ([self _adViewCanBeShown])
	{
		UIView *adView = [[UnityAdsViewManager sharedInstance] adView];
		if (adView != nil)
		{
			if ([self.delegate respondsToSelector:@selector(unityAdsWillShow:)])
				[self.delegate unityAdsWillShow:self];

			return adView;
		}
	}
	
	return nil;
}

- (BOOL)canShow
{
	UAAssertV([NSThread mainThread], NO);
	
	return [self _adViewCanBeShown];
}

- (void)stopAll
{
	UAAssert([NSThread isMainThread]);
	
	[self.campaignManager performSelector:@selector(cancelAllDownloads) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
}

- (void)trackInstall
{
	UAAssert([NSThread isMainThread]);
	
	[self _trackInstall];
}

- (void)refresh
{
	UAAssert([NSThread isMainThread]);
	
	if ([[UnityAdsViewManager sharedInstance] adViewVisible])
		UALOG_DEBUG(@"Ad view visible, not refreshing.");
	else
		[self _refresh];
}

- (void)dealloc
{
	self.campaignManager.delegate = nil;
	[[UnityAdsViewManager sharedInstance] setDelegate:nil];
	
	dispatch_release(self.queue);
}

#pragma mark - UnityAdsCampaignManagerDelegate

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem gamerID:(NSString *)gamerID
{
	UAAssert([NSThread isMainThread]);
	
	UALOG_DEBUG(@"");
	
	self.campaigns = campaigns;
	self.rewardItem = rewardItem;
	self.gamerID = gamerID;
	
	[self _notifyDelegateOfCampaignAvailability];
}

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedJSON:(NSString *)json
{
	UAAssert([NSThread isMainThread]);

  // If the view manager already has campaign JSON data, it means that
	// campaigns were updated, and we might want to update the webapp.
	if ([[UnityAdsViewManager sharedInstance] campaignJSON] != nil) {
		self.webViewInitialized = NO;
	}
  
  if (self.webViewInitialized == NO) {
    [[UnityAdsViewManager sharedInstance] loadWebView];
  }
  
  [[UnityAdsViewManager sharedInstance] setCampaignJSON:json];
}

#pragma mark - UnityAdsViewManagerDelegate

-(UnityAdsCampaign *)viewManager:(UnityAdsViewManager *)viewManager campaignWithID:(NSString *)campaignID
{
	UAAssertV([NSThread isMainThread], nil);
	
	UnityAdsCampaign *foundCampaign = nil;
	
	for (UnityAdsCampaign *campaign in self.campaigns)
	{
		if ([campaign.id isEqualToString:campaignID])
		{
			foundCampaign = campaign;
			break;
		}
	}
	
	UALOG_DEBUG(@"");
	
	return foundCampaign;
}

-(NSURL *)viewManager:(UnityAdsViewManager *)viewManager videoURLForCampaign:(UnityAdsCampaign *)campaign
{
	UAAssertV([NSThread isMainThread], nil);
	UALOG_DEBUG(@"");
	
	return [self.campaignManager videoURLForCampaign:campaign];
}

- (void)viewManagerStartedPlayingVideo:(UnityAdsViewManager *)viewManager
{
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if ([self.delegate respondsToSelector:@selector(unityAdsVideoStarted:)])
		[self.delegate unityAdsVideoStarted:self];
}

- (void)viewManagerVideoEnded:(UnityAdsViewManager *)viewManager
{
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	[self.delegate unityAds:self completedVideoWithRewardItemKey:self.rewardItem.key];
}

- (void)viewManager:(UnityAdsViewManager *)viewManager loggedVideoPosition:(VideoAnalyticsPosition)videoPosition campaign:(UnityAdsCampaign *)campaign
{
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	[self _logVideoAnalyticsWithPosition:videoPosition campaign:campaign];
}

- (UIViewController *)viewControllerForPresentingViewControllersForViewManager:(UnityAdsViewManager *)viewManager
{
	UAAssertV([NSThread isMainThread], nil);
	UALOG_DEBUG(@"");
	
	return [self.delegate viewControllerForPresentingViewControllersForAds:self];
}

- (void)viewManagerWillCloseAdView:(UnityAdsViewManager *)viewManager
{
	UAAssert([NSThread isMainThread]);
	UALOG_DEBUG(@"");
	
	if ([self.delegate respondsToSelector:@selector(unityAdsWillHide:)])
		[self.delegate unityAdsWillHide:self];
}

- (void)viewManagerWebViewInitialized:(UnityAdsViewManager *)viewManager
{
	UAAssert([NSThread isMainThread]);	
	UALOG_DEBUG(@"");
	
	self.webViewInitialized = YES;
	
	[self _notifyDelegateOfCampaignAvailability];
}

@end