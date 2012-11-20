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
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsProperties/UnityAdsProperties.h"
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"

@interface UnityAdsViewManager () <UIWebViewDelegate, UIScrollViewDelegate>
@property (nonatomic, strong) UIWindow *window;
@property (nonatomic, strong) UIView *adContainerView;
@property (nonatomic, strong) UILabel *progressLabel;
@property (nonatomic, strong) UnityAdsVideo *player;
@property (nonatomic, assign) UIViewController *storePresentingViewController;
@property (nonatomic, strong) NSDictionary *productParams;
@property (nonatomic, strong) UIViewController *storeController;
@end

@implementation UnityAdsViewManager

#pragma mark - Private

- (void)closeAdView {
	[self.delegate viewManagerWillCloseAdView];
	[[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{}];
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

- (NSValue *)_valueWithDuration:(Float64)duration {
	CMTime time = CMTimeMakeWithSeconds(duration, NSEC_PER_SEC);
	return [NSValue valueWithCMTime:time];
}

- (void)openAppStoreWithData:(NSDictionary *)data {
	UALOG_DEBUG(@"");
	
  if (![self _canOpenStoreProductViewController]) {
		NSString *clickUrl = [data objectForKey:@"clickUrl"];
    if (clickUrl == nil) return;    
    UALOG_DEBUG(@"Cannot open store product view controller, falling back to click URL.");
		[[UnityAdsWebAppController sharedInstance] openExternalUrl:clickUrl];
		return;
	}
  
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  if ([storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)] == YES) {
    NSString *gameId = [data objectForKey:@"iTunesId"];
    if (gameId == nil || [gameId length] < 1) return;
    self.productParams = @{SKStoreProductParameterITunesItemIdentifier:gameId};
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    if ([self.storeController respondsToSelector:@selector(setDelegate:)]) {
      [self.storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"RESULT: %i", result);
      if (result) {
        [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:@"hideSpinner" data:@{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
        self.storePresentingViewController = [self.delegate viewControllerForPresentingViewControllersForViewManager:self];
        [self.storePresentingViewController presentModalViewController:self.storeController animated:YES];
      }
      else {
        UALOG_DEBUG(@"Loading product information failed: %@", error);
      }
    };
    
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:@"showSpinner" data:@{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
    SEL loadProduct = @selector(loadProductWithParameters:completionBlock:);
    if ([self.storeController respondsToSelector:loadProduct]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
      [self.storeController performSelector:loadProduct withObject:self.productParams withObject:storeControllerComplete];
#pragma clang diagnostic pop
    }
  }
}


#pragma mark - Public

static UnityAdsViewManager *sharedUnityAdsInstanceViewManager = nil;

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedUnityAdsInstanceViewManager == nil)
				sharedUnityAdsInstanceViewManager = [[UnityAdsViewManager alloc] init];
	}
	
	return sharedUnityAdsInstanceViewManager;
}

- (id)init {
	UAAssertV([NSThread isMainThread], nil);
	
	if ((self = [super init])) {
    NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
    [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationDidEnterBackgroundNotification object:nil];
    _window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    [UnityAdsWebAppController sharedInstance];
    [_window addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
	}
	
	return self;
}

#pragma mark - Notification receiver

- (void)notificationHandler: (id) notification {
  NSString *name = [notification name];
  
  UALOG_DEBUG(@"notification: %@", name);
  
  if ([name isEqualToString:UIApplicationDidEnterBackgroundNotification]) {
    [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = YES;
    if (self.player != nil) {
      UALOG_DEBUG(@"Destroying player");
      [self.player destroyPlayer];
      [self hidePlayer];
    }
    
    [self closeAdView];
  }
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
      [self.adContainerView setBackgroundColor:[UIColor blackColor]];
			
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
  [[UnityAdsWebAppController sharedInstance] setupWebApp:_window.bounds];
  [[UnityAdsWebAppController sharedInstance] loadWebApp:webAppValues];
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
	UALOG_DEBUG(@"");
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}


#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController
{
	UALOG_DEBUG(@"");
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
  [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = NO;
}

- (void)videoPlaybackEnded {
  [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = YES;
	[self.delegate viewManagerVideoEnded];
	[self hidePlayer];
	
  NSDictionary *data = @{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
  
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:data];
	[[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
}


#pragma mark - Video

- (void)hidePlayer {
  if (self.player != nil) {
    self.progressLabel.hidden = YES;
    [self.player.playerLayer removeFromSuperlayer];
    self.player.playerLayer = nil;
    self.player = nil;
  }
}

- (void)showPlayerAndPlaySelectedVideo:(BOOL)checkIfWatched {
	UALOG_DEBUG(@"");
  
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed && checkIfWatched) {
    UALOG_DEBUG(@"Trying to watch a campaign that is already viewed!");
    return;
  }
  
	NSURL *videoURL = [[UnityAdsCampaignManager sharedInstance] getVideoURLForCampaign:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
  
	if (videoURL == nil) {
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
