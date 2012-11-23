//
//  UnityAdsAdViewController.m
//  UnityAds
//
//  Created by bluesun on 11/21/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsMainViewController.h"
#import "UnityAds.h"

#import "UnityAdsVideo/UnityAdsVideoView.h"
#import "UnityAdsWebView/UnityAdsWebAppController.h"
#import "UnityAdsVideo/UnityAdsVideo.h"
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "UnityAdsCampaign/UnityAdsCampaign.h"
#import "UnityAdsProperties/UnityAdsProperties.h"

@interface UnityAdsMainViewController ()
  @property (nonatomic, strong) UILabel *progressLabel;
  @property (nonatomic, strong) UnityAdsVideoView *videoView;
  @property (nonatomic, strong) UnityAdsVideo *player;
  @property (nonatomic, strong) UIViewController *storeController;
@end

@implementation UnityAdsMainViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
      // Add notification listener
      NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
      [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationDidEnterBackgroundNotification object:nil];
      
      // "init" WebAppController
      [UnityAdsWebAppController sharedInstance];
      [[UnityAdsWebAppController sharedInstance] setDelegate:self];
    }
    return self;
}

- (void)dealloc {
	UALOG_DEBUG(@"");
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)viewDidLoad {
	UALOG_DEBUG(@"");
  [super viewDidLoad];
  [self _createProgressLabel];
  [self _createVideoView];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return (interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationLandscapeRight);
}

- (NSUInteger)supportedInterfaceOrientations {
  UALOG_DEBUG(@"");
  return UIInterfaceOrientationMaskLandscape | UIInterfaceOrientationMaskPortrait;
}


#pragma mark - Public

- (BOOL)closeAds {
  [[[UnityAdsProperties sharedInstance] currentViewController] dismissViewControllerAnimated:YES completion:nil];
  return YES;
}

- (BOOL)openAds {
  UALOG_DEBUG(@"");
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:@"start" data:@{}];
  [[[UnityAdsProperties sharedInstance] currentViewController] presentViewController:self animated:YES completion:nil];
  
  if (![self.videoView.superview isEqual:self.view]) {
    [self.view addSubview:self.videoView];
    [self.videoView setFrame:self.view.bounds];
    self.videoView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
  }
  
  if (![self.progressLabel.superview isEqual:self.view]) {
    [self.view addSubview:self.progressLabel];
    [self.progressLabel setFrame:self.view.bounds];
  }
  
  if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:self.view]) {
    [self.view addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:self.view.bounds];
  }
  
  return YES;
}

- (BOOL)mainControllerVisible {
  if (self.view.superview != nil) {
    return YES;
  }
  
  return NO;
}


#pragma mark - Video

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

  if (self.player == nil) {
    self.player = [[UnityAdsVideo alloc] initWithPlayerItem:nil];
    self.player.delegate = self;
    [_videoView setPlayer:self.player];
  }
  
  [self.player preparePlayer];
  [self.player replaceCurrentItemWithPlayerItem:item];
  
  [self.view bringSubviewToFront:self.videoView];
  [self.player playSelectedVideo];
}

- (void)_hidePlayer {
  if (self.player != nil) {
    self.progressLabel.hidden = YES;
    [self.view sendSubviewToBack:self.progressLabel];
    [self.view sendSubviewToBack:self.videoView];
  }
}

- (void)_clearPlayer {
  [self.player clearPlayer];
  [self.videoView setPlayer:nil];
  self.player.delegate = nil;
  self.player = nil;
}

- (Float64)_currentVideoDuration {
	CMTime durationTime = self.player.currentItem.asset.duration;
	Float64 duration = CMTimeGetSeconds(durationTime);
	
	return duration;
}

- (NSValue *)_valueWithDuration:(Float64)duration {
	CMTime time = CMTimeMakeWithSeconds(duration, NSEC_PER_SEC);
	return [NSValue valueWithCMTime:time];
}


#pragma mark - UnityAdsVideoDelegate

- (void)videoPositionChanged:(CMTime)time {
  [self _updateTimeRemainingLabelWithTime:time];
}

- (void)videoPlaybackStarted {
  [self.delegate mainControllerStartedPlayingVideo];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:@"showSpinner" data:@{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id, @"text":@"Buffering..."}];
  [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = NO;
}

- (void)videoStartedPlaying {
  NSDictionary *data = @{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:@"hideSpinner" data:@{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id, @"text":@"Buffering..."}];
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:data];
  [self _displayProgressLabel];
}

- (void)videoPlaybackEnded {
  [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = YES;
  [self.delegate mainControllerVideoEnded];
  [self _hidePlayer];
  [self _clearPlayer];
	
	[[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
}


#pragma mark - Video Progress Label

- (void)_updateTimeRemainingLabelWithTime:(CMTime)currentTime {
	Float64 duration = [self _currentVideoDuration];
	Float64 current = CMTimeGetSeconds(currentTime);
	NSString *descriptionText = [NSString stringWithFormat:NSLocalizedString(@"This video ends in %.0f seconds.", nil), duration - current];
	self.progressLabel.text = descriptionText;
}

- (void)_displayProgressLabel {
	CGFloat padding = 10.0;
	CGFloat height = 30.0;
	CGRect labelFrame = CGRectMake(padding, self.view.frame.size.height - height, self.view.frame.size.width - (padding * 2.0), height);
	self.progressLabel.frame = labelFrame;
	self.progressLabel.hidden = NO;
	[self.view bringSubviewToFront:self.progressLabel];
}


#pragma mark - Notification receiver

- (void)notificationHandler: (id) notification {
  NSString *name = [notification name];

  UALOG_DEBUG(@"notification: %@", name);
  
  if ([name isEqualToString:UIApplicationDidEnterBackgroundNotification]) {
    [[UnityAdsWebAppController sharedInstance] webView].userInteractionEnabled = YES;
    if (self.player != nil) {
      UALOG_DEBUG(@"Destroying player");
      [self _hidePlayer];
      [self _clearPlayer];
    }
    
    [self closeAds];
  }
}


#pragma mark - AppStore opening

- (BOOL)_canOpenStoreProductViewController {
	Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
	return [storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)];
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
    NSDictionary *productParams = @{SKStoreProductParameterITunesItemIdentifier:gameId};
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    if ([self.storeController respondsToSelector:@selector(setDelegate:)]) {
      [self.storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"RESULT: %i", result);
      if (result) {
        [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:@"hideSpinner" data:@{@"campaignId":[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
        [[UnityAdsMainViewController sharedInstance] presentModalViewController:self.storeController animated:YES];
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
      [self.storeController performSelector:loadProduct withObject:productParams withObject:storeControllerComplete];
#pragma clang diagnostic pop
    }
  }
}


#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController {
	UALOG_DEBUG(@"");
  [[UnityAdsMainViewController sharedInstance] dismissViewControllerAnimated:YES completion:nil];
}


#pragma mark - WebAppController

- (void)webAppReady {
  [self.delegate mainControllerWebViewInitialized];
}


#pragma mark - Shared Instance

static UnityAdsMainViewController *sharedMainViewController = nil;

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedMainViewController == nil) {
      sharedMainViewController = [[UnityAdsMainViewController alloc] initWithNibName:nil bundle:nil];
		}
	}
	
	return sharedMainViewController;
}


#pragma mark - Private view creations

- (void)_createProgressLabel {
  self.progressLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  self.progressLabel.backgroundColor = [UIColor clearColor];
  self.progressLabel.textColor = [UIColor whiteColor];
  self.progressLabel.font = [UIFont systemFontOfSize:12.0];
  self.progressLabel.textAlignment = UITextAlignmentRight;
  self.progressLabel.shadowColor = [UIColor blackColor];
  self.progressLabel.shadowOffset = CGSizeMake(0, 1.0);
  self.progressLabel.autoresizingMask = UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleWidth;
  [self.view addSubview:self.progressLabel];
}

- (void)_createVideoView {
  self.videoView = [[UnityAdsVideoView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
}

@end