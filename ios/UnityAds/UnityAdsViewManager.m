//
//  UnityAdsViewManager.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/20/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>

#import "UnityAdsViewManager.h"
#import "UnityAds.h"
#import "UnityAdsCampaign/UnityAdsCampaign.h"
#import "UnityAdsVideo/UnityAdsVideo.h"
#import "UnityAdsWebView/UnityAdsWebAppController.h"
#import "UnityAdsUtils/UnityAdsUtils.h"
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsProperties/UnityAdsProperties.h"
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"

@interface UnityAdsViewManager () <UIWebViewDelegate, UIScrollViewDelegate>
@property (nonatomic, strong) UIWindow *window;
@property (nonatomic, strong) UIView *adContainerView;
@property (nonatomic, strong) UILabel *progressLabel;
@property (nonatomic, strong) UnityAdsVideo *player;
@property (nonatomic, assign) UIViewController *storePresentingViewController;
@end

@implementation UnityAdsViewManager

#pragma mark - Private

- (void)closeAdView {
	[self.delegate viewManagerWillCloseAdView];
	
	[self.window addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
	[self.adContainerView removeFromSuperview];
}

- (BOOL)_canOpenStoreProductViewController {
	Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
	return [storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)];
}

- (Float64)_currentVideoDuration {
	CMTime durationTime = self.player.currentItem.asset.duration;
	Float64 duration = CMTimeGetSeconds(durationTime);
	
	return duration;
}

- (void)_updateTimeRemainingLabelWithTime:(CMTime)currentTime {
	Float64 duration = [self _currentVideoDuration];
	Float64 current = CMTimeGetSeconds(currentTime);
	NSString *descriptionText = [NSString stringWithFormat:NSLocalizedString(@"This video ends in %.0f seconds.", nil), duration - current];
	self.progressLabel.text = descriptionText;
}

- (void)_displayProgressLabel
{
	CGFloat padding = 10.0;
	CGFloat height = 30.0;
	CGRect labelFrame = CGRectMake(padding, self.adContainerView.frame.size.height - height, self.adContainerView.frame.size.width - (padding * 2.0), height);
	self.progressLabel.frame = labelFrame;
	self.progressLabel.hidden = NO;
	[self.adContainerView bringSubviewToFront:self.progressLabel];
}

- (NSValue *)_valueWithDuration:(Float64)duration
{
	CMTime time = CMTimeMakeWithSeconds(duration, NSEC_PER_SEC);
	return [NSValue valueWithCMTime:time];
}

- (void)openAppStoreWithGameId:(NSString *)gameId
{
	if (gameId == nil || [gameId length] == 0)
	{
		UALOG_DEBUG(@"Game ID not set or empty.");
		return;
	}
	
	if ( ! [self _canOpenStoreProductViewController])
	{
		UALOG_DEBUG(@"Cannot open store product view controller, falling back to click URL.");
		[[UnityAdsWebAppController sharedInstance] openExternalUrl:[[[UnityAdsCampaignManager sharedInstance] selectedCampaign].clickURL absoluteString]];
    //[self _openURL:[self.selectedCampaign.clickURL absoluteString]];
		return;
	}

#if __IPHONE_OS_VERSION_MAX_ALLOWED > __IPHONE_5_1
	SKStoreProductViewController *storeController = [[SKStoreProductViewController alloc] init];
	storeController.delegate = (id)self;
	NSDictionary *productParameters = @{ SKStoreProductParameterITunesItemIdentifier : gameId};
	[storeController loadProductWithParameters:productParameters completionBlock:^(BOOL result, NSError *error) {
		if (result)
		{
			self.storePresentingViewController = [self.delegate viewControllerForPresentingViewControllersForViewManager:self];
			[self.storePresentingViewController presentModalViewController:storeController animated:YES];
		}
		else
			UALOG_DEBUG(@"Loading product information failed: %@", error);
	}];
#endif
}

// FIX

/*
- (void)_webViewInitComplete
{
	_webApp.webViewInitialized = YES;
	[self.delegate viewManagerWebViewInitialized:self];
}*/

// FIX

/*
- (void)_webViewShow
{
  [_webApp setWebViewCurrentView:@"start" data:@""];
}*/


#pragma mark - Public

static UnityAdsViewManager *sharedUnityAdsInstanceViewManager = nil;

+ (id)sharedInstance
{
	@synchronized(self)
	{
		if (sharedUnityAdsInstanceViewManager == nil)
				sharedUnityAdsInstanceViewManager = [[UnityAdsViewManager alloc] init];
	}
	
	return sharedUnityAdsInstanceViewManager;
}

- (id)init
{
	UAAssertV([NSThread isMainThread], nil);
	
	if ((self = [super init]))
	{
		_window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    [UnityAdsWebAppController sharedInstance];
		//_webApp = [[UnityAdsWebAppController alloc] init];

    // FIX
    [_window addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
		//[_window addSubview:_webApp.webView];
	}
	
	return self;
}

- (void)loadWebView
{
	UAAssert([NSThread isMainThread]);
  //[_webApp setup:_window.bounds webAppParams:valueDictionary];
}

// FIX: Rename this method to something more descriptive
- (UIView *)adView
{
	UAAssertV([NSThread isMainThread], nil);
	
	if ([[UnityAdsWebAppController sharedInstance] webViewInitialized])
	{
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{}];
		
		if (self.adContainerView == nil)
		{
			self.adContainerView = [[UIView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
			
			self.progressLabel = [[UILabel alloc] initWithFrame:CGRectZero];
			self.progressLabel.backgroundColor = [UIColor clearColor];
			self.progressLabel.textColor = [UIColor whiteColor];
			self.progressLabel.font = [UIFont systemFontOfSize:12.0];
			self.progressLabel.textAlignment = UITextAlignmentRight;
			self.progressLabel.shadowColor = [UIColor blackColor];
			self.progressLabel.shadowOffset = CGSizeMake(0, 1.0);
			self.progressLabel.autoresizingMask = UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleWidth;
			[self.adContainerView addSubview:self.progressLabel];
		}
		
		if ([[UnityAdsWebAppController sharedInstance] webView].superview != self.adContainerView)
		{
			[[[UnityAdsWebAppController sharedInstance] webView] setBounds:self.adContainerView.bounds];
			[self.adContainerView addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
		}
		
		return self.adContainerView;
	}
	else
	{
		UALOG_DEBUG(@"Web view not initialized.");
		return nil;
	}
}

- (void)initWebApp {
	UAAssert([NSThread isMainThread]);
 
  NSDictionary *persistingData = @{@"campaignData":[[UnityAdsCampaignManager sharedInstance] campaignData], @"platform":@"ios", @"deviceId":[UnityAdsDevice md5DeviceId]};
  
  NSDictionary *trackingData = @{@"iOSVersion":[UnityAdsDevice softwareVersion], @"deviceType":[UnityAdsDevice analyticsMachineName]};
  NSMutableDictionary *webAppValues = [NSMutableDictionary dictionaryWithDictionary:persistingData];
  
  if ([UnityAdsDevice canUseTracking]) {
    [webAppValues addEntriesFromDictionary:trackingData];
  }
  
  [[UnityAdsWebAppController sharedInstance] setDelegate:self];
  [[UnityAdsWebAppController sharedInstance] setup:_window.bounds webAppParams:webAppValues];
}

- (BOOL)adViewVisible
{
	UAAssertV([NSThread isMainThread], NO);
	
	if ([[UnityAdsWebAppController sharedInstance] webView].superview == self.window)
		return NO;
	else
		return YES;
}

- (void)dealloc
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}


#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController
{
	[self.storePresentingViewController dismissViewControllerAnimated:YES completion:nil];

	self.storePresentingViewController = nil;
}


#pragma mark - UnityAdsVideoDelegate

- (void)videoPositionChanged:(CMTime)time {
  [self _updateTimeRemainingLabelWithTime:time];
}

- (void)videoPlaybackStarted {
  [self _displayProgressLabel];
  [self.delegate viewManagerStartedPlayingVideo];
}

- (void)videoPlaybackEnded {
	[self.delegate viewManagerVideoEnded];
	[self hidePlayer];
	
  NSDictionary *data = @{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:data];
	[[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
}


#pragma mark - Video

- (void)hidePlayer {
 	self.progressLabel.hidden = YES;
	[self.player.playerLayer removeFromSuperlayer];
	self.player.playerLayer = nil;
	self.player = nil;
}

- (void)showPlayerAndPlaySelectedVideo {
	UALOG_DEBUG(@"");
	
	NSURL *videoURL = [[UnityAdsCampaignManager sharedInstance] getVideoURLForCampaign:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
	if (videoURL == nil)
	{
		UALOG_DEBUG(@"Video not found!");
		return;
	}
	
	AVPlayerItem *item = [AVPlayerItem playerItemWithURL:videoURL];
  
  self.player = [[UnityAdsVideo alloc] initWithPlayerItem:item];
  self.player.delegate = self;
  [self.player createPlayerLayer];
  self.player.playerLayer.frame = self.adContainerView.bounds;
	[self.adContainerView.layer addSublayer:self.player.playerLayer];
  [self.player playSelectedVideo];
}


#pragma mark - WebAppController

- (void)webAppReady {
  _webViewInitialized = YES;
  [self.delegate viewManagerWebViewInitialized];
}

@end
