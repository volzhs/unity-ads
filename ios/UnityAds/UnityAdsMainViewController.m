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
#import "UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "UnityAdsCampaign/UnityAdsCampaign.h"
#import "UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "UnityAdsProperties/UnityAdsProperties.h"
#import "UnityAdsProperties/UnityAdsConstants.h"

@interface UnityAdsMainViewController ()
  @property (nonatomic, strong) UnityAdsVideoViewController *videoController;
  @property (nonatomic, strong) UIViewController *storeController;
  @property (nonatomic, strong) void (^closeHandler)(void);
  @property (nonatomic, strong) void (^openHandler)(void);
  @property (nonatomic, assign) BOOL isOpen;
@end

@implementation UnityAdsMainViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  
    if (self) {
      // Add notification listener
      NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
      [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationDidEnterBackgroundNotification object:nil];
      
      // Start WebAppController
      [UnityAdsWebAppController sharedInstance];
      [[UnityAdsWebAppController sharedInstance] setDelegate:self];
    }
  
    return self;
}

- (void)dealloc {
	UALOG_DEBUG(@"");
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [self _destroyVideoController];
}

- (void)viewDidLoad {
	UALOG_DEBUG(@"");
  [self.view setBackgroundColor:[UIColor blackColor]];
  [super viewDidLoad];
}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
}


#pragma mark - Orientation handling

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return YES;
}

- (NSUInteger)supportedInterfaceOrientations {
  UALOG_DEBUG(@"");
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  return YES;
}


#pragma mark - Public

- (BOOL)closeAds:(BOOL)forceMainThread withAnimations:(BOOL)animated {
  UALOG_DEBUG(@"");
  
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) return NO;
  
  if (forceMainThread) {
    dispatch_async(dispatch_get_main_queue(), ^{
      [self _dismissMainViewController:forceMainThread withAnimations:animated];
    });
  }
  else {
    [self _dismissMainViewController:forceMainThread withAnimations:animated];
  }
  
  return YES;
}

- (void)_dismissMainViewController:(BOOL)forcedToMainThread withAnimations:(BOOL)animated {
  if (self.videoController.view.superview != nil) {
    [self dismissViewControllerAnimated:NO completion:nil];
  }
  
  if (!forcedToMainThread) {
    UALOG_DEBUG(@"Setting startview right now. No time for block completion");
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIClose}];
  }
  
  [self.delegate mainControllerWillClose];
  
  if (![UnityAdsDevice isSimulator]) {
    if (self.closeHandler == nil) {
      self.closeHandler = ^(void) {
        UALOG_DEBUG(@"Setting start view after close");
        [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIClose}];
        self.isOpen = NO;
        [self.delegate mainControllerDidClose];
      };
    }
  }
  else {
    self.isOpen = NO;
    [self.delegate mainControllerDidClose];
  }
  
  [[[UnityAdsProperties sharedInstance] currentViewController] dismissViewControllerAnimated:animated completion:self.closeHandler];
}

- (BOOL)openAds:(BOOL)animated {
  UALOG_DEBUG(@"");
  
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) return NO;
  
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.delegate mainControllerWillOpen];
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIOpen, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key}];
    
    if (![UnityAdsDevice isSimulator]) {
      if (self.openHandler == nil) {
        self.openHandler = ^(void) {
          UALOG_DEBUG(@"Running openhandler after opening view");
          [self.delegate mainControllerDidOpen];
        };
      }
    }
    else {
      [self.delegate mainControllerDidOpen];
    }
    
    [[[UnityAdsProperties sharedInstance] currentViewController] presentViewController:self animated:animated completion:self.openHandler];
    
    if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:self.view]) {
      [self.view addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
      [[[UnityAdsWebAppController sharedInstance] webView] setFrame:self.view.bounds];
    }
  });
  
  self.isOpen = YES;
  return YES;
}

- (BOOL)mainControllerVisible {
  if (self.view.superview != nil || self.isOpen) {
    return YES;
  }
  
  return NO;
}


#pragma mark - Video

- (void)videoPlayerStartedPlaying {
  [self.delegate mainControllerStartedPlayingVideo];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key}];
  [self presentViewController:self.videoController animated:NO completion:nil];
}

- (void)videoPlayerEncounteredError {
  UALOG_DEBUG(@"");
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  [self _dismissVideoController];
}

- (void)videoPlayerPlaybackEnded {
  [self.delegate mainControllerVideoEnded];
  [self _dismissVideoController];
}

- (void)showPlayerAndPlaySelectedVideo:(BOOL)checkIfWatched {
	UALOG_DEBUG(@"");
    
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed && checkIfWatched) {
    UALOG_DEBUG(@"Trying to watch a campaign that is already viewed!");
    return;
  }

  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  
  [self _createVideoController];
  [self.videoController playCampaign:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
}

- (void)_createVideoController {
  self.videoController = [[UnityAdsVideoViewController alloc] initWithNibName:nil bundle:nil];
  self.videoController.delegate = self;
}

- (void)_destroyVideoController {
  self.videoController.delegate = nil;
  self.videoController = nil;
}

- (void)_dismissVideoController {
  [self dismissViewControllerAnimated:NO completion:nil];
  [self _destroyVideoController];
}


#pragma mark - Notification receiver

- (void)notificationHandler: (id) notification {
  NSString *name = [notification name];

  UALOG_DEBUG(@"Notification: %@", name);
  
  if ([name isEqualToString:UIApplicationDidEnterBackgroundNotification]) {
    [[UnityAdsWebAppController sharedInstance] setWebViewInitialized:NO];
    [self.videoController forceStopVideoPlayer];
    [self closeAds:NO withAnimations:NO];
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
    [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
    
    if (self.delegate != nil) {
      [self.delegate mainControllerWillLeaveApplication];
    }
    
		[[UnityAdsWebAppController sharedInstance] openExternalUrl:clickUrl];
		return;
	}
  
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  if ([storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)] == YES) {
    if (![[data objectForKey:@"iTunesId"] isKindOfClass:[NSString class]]) return;
    NSString *gameId = nil;
    gameId = [data valueForKey:@"iTunesId"];
    if (gameId == nil || [gameId length] < 1) return;
    
    /*
      FIX: This _could_ bug someday. The key @"id" is written literally (and 
      not using SKStoreProductParameterITunesItemIdentifier), so that
      with some compiler options (or linker flags) you wouldn't get errors.
      
      The way this could bug someday is that Apple changes the contents of 
      SKStoreProductParameterITunesItemIdentifier.
     
      HOWTOFIX: Find a way to reflect global constant SKStoreProductParameterITunesItemIdentifier
      by using string value and not the constant itself.
    */
    NSDictionary *productParams = @{@"id":gameId};
    
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    if ([self.storeController respondsToSelector:@selector(setDelegate:)]) {
      [self.storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"RESULT: %i", result);
      if (result) {
        [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}];
        dispatch_async(dispatch_get_main_queue(), ^{
          [[UnityAdsMainViewController sharedInstance] presentViewController:self.storeController animated:YES completion:nil];
          [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
        });
      }
      else {
        UALOG_DEBUG(@"Loading product information failed: %@", error);
      }
    };
    
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}];
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
  dispatch_async(dispatch_get_main_queue(), ^{
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIInitComplete, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key}];
  });
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

@end