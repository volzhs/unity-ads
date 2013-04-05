//
//  UnityAdsAdViewController.m
//  UnityAds
//
//  Created by bluesun on 11/21/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsMainViewController.h"

#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"

#import "../UnityAdsViewState/UnityAdsViewStateDefaultOffers.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultVideoPlayer.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultEndScreen.h"
#import "../UnityAdsViewState/UnityAdsViewStateDefaultSpinner.h"

#import "../UnityAds.h"

@interface UnityAdsMainViewController ()
  @property (nonatomic, strong) void (^closeHandler)(void);
  @property (nonatomic, strong) void (^openHandler)(void);
  @property (nonatomic, strong) NSArray *viewStateHandlers;
  @property (nonatomic, strong) UnityAdsViewState *currentViewState;
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


#pragma mark - Orientation handling

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return YES;
}

- (NSUInteger)supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  return YES;
}


#pragma mark - Public

- (void)applyOptionsToCurrentState:(NSDictionary *)options {
  if (self.currentViewState != nil)
    [self.currentViewState applyOptions:options];
}

- (BOOL)hasState:(UnityAdsViewStateType)requestedState {
  for (UnityAdsViewState *currentState in self.viewStateHandlers) {
    if ([currentState getStateType] == requestedState) {
      return YES;
    }
  }
  
  return NO;
}

- (UnityAdsViewState *)selectState:(UnityAdsViewStateType)requestedState {
  self.currentViewState = nil;
  UnityAdsViewState *viewStateManager = nil;
  
  for (UnityAdsViewState *currentState in self.viewStateHandlers) {
    if ([currentState getStateType] == requestedState) {
      viewStateManager = currentState;
      break;
    }
  }
  
  if (viewStateManager != nil) {
    self.currentViewState = viewStateManager;
  }
  
  return self.currentViewState;
}

- (BOOL)changeState:(UnityAdsViewStateType)requestedState withOptions:(NSDictionary *)options {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (self.currentViewState != nil) {
      [self.currentViewState exitState:options];
    }
    
    [self selectState:requestedState];
    
    if (self.currentViewState != nil) {
      [self.currentViewState enterState:options];
    }
  });
  
  if ([self hasState:requestedState]) {
    return YES;
  }
  
  return NO;
}

- (BOOL)closeAds:(BOOL)forceMainThread withAnimations:(BOOL)animated withOptions:(NSDictionary *)options {
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

- (BOOL)openAds:(BOOL)animated inState:(UnityAdsViewStateType)requestedState withOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) return NO;
  
  // FIX: TEST, DO NOT GENERATE LIST OF MANAGERS HERE
  if (self.viewStateHandlers == nil) {
    UnityAdsViewStateDefaultOffers *defaultOffers = [[UnityAdsViewStateDefaultOffers alloc] init];
    defaultOffers.delegate = self;
    UnityAdsViewStateDefaultVideoPlayer *defaultVideoPlayer = [[UnityAdsViewStateDefaultVideoPlayer alloc] init];
    defaultVideoPlayer.delegate = self;
    UnityAdsViewStateDefaultEndScreen *defaultEndScreen = [[UnityAdsViewStateDefaultEndScreen alloc] init];
    defaultEndScreen.delegate = self;
    UnityAdsViewStateDefaultSpinner *defaultSpinner = [[UnityAdsViewStateDefaultSpinner alloc] init];
    defaultSpinner.delegate = self;
    self.viewStateHandlers = [[NSArray alloc] initWithObjects:defaultOffers, defaultVideoPlayer, defaultEndScreen, defaultSpinner, nil];
  }
  
  dispatch_async(dispatch_get_main_queue(), ^{
    [self selectState:requestedState];
    if (self.currentViewState != nil) {
      [self.delegate mainControllerWillOpen];
      [self.currentViewState willBeShown];
      [self changeState:requestedState withOptions:options];
      //[viewStateManager enterState];
      
      if (![UnityAdsDevice isSimulator]) {
        if (self.openHandler == nil) {
          __unsafe_unretained typeof(self) weakSelf = self;
          
          self.openHandler = ^(void) {
            UALOG_DEBUG(@"Running openhandler after opening view");
            if (weakSelf != NULL) {
              if (weakSelf.currentViewState != nil) {
                [weakSelf.currentViewState wasShown];
              }
              [weakSelf.delegate mainControllerDidOpen];
            }
          };
        }
      }
      else {
        [self.delegate mainControllerDidOpen];
      }
      
      [[[UnityAdsProperties sharedInstance] currentViewController] presentViewController:self animated:animated completion:self.openHandler];
    }
  });
  
  if (self.currentViewState != nil) {
    self.isOpen = YES;
  }
  
  return self.isOpen;
}

- (BOOL)mainControllerVisible {
  if (self.view.superview != nil || self.isOpen) {
    return YES;
  }
  
  return NO;
}


#pragma mark - Private

- (void)_dismissMainViewController:(BOOL)forcedToMainThread withAnimations:(BOOL)animated {
  if ([self.currentViewState getStateType] == kUnityAdsViewStateTypeVideoPlayer) {
    [self dismissViewControllerAnimated:NO completion:nil];
  }
  
  if (!forcedToMainThread) {
    if (self.currentViewState != nil) {
      [self.currentViewState exitState:nil];
    }
  }
  
  [self.delegate mainControllerWillClose];
  
  if (![UnityAdsDevice isSimulator]) {
    if (self.closeHandler == nil) {
      __unsafe_unretained typeof(self) weakSelf = self;
      self.closeHandler = ^(void) {
        if (weakSelf != NULL) {
          if (weakSelf.currentViewState != nil) {
            [weakSelf.currentViewState exitState:nil];
          }
          weakSelf.isOpen = NO;
          [weakSelf.delegate mainControllerDidClose];
        }
      };
    }
  }
  else {
    self.isOpen = NO;
    [self.delegate mainControllerDidClose];
  }
  
  [[[UnityAdsProperties sharedInstance] currentViewController] dismissViewControllerAnimated:animated completion:self.closeHandler];
}


#pragma mark - Notification receivers

- (void)notificationHandler: (id) notification {
  NSString *name = [notification name];

  UALOG_DEBUG(@"Notification: %@", name);
  
  if ([name isEqualToString:UIApplicationDidEnterBackgroundNotification]) {
    // FIX: Find a better way to re-initialize when needed
    //[[UnityAdsWebAppController sharedInstance] setWebViewInitialized:NO];

    [self applyOptionsToCurrentState:@{kUnityAdsNativeEventForceStopVideoPlayback:@true}];

    if (self.isOpen)
      [self closeAds:NO withAnimations:NO withOptions:nil];
  }
}

- (void)stateNotification:(UnityAdsViewStateAction)action {
  UALOG_DEBUG(@"Got state action: %i", action);
  
  if (action == kUnityAdsStateActionWillLeaveApplication) {
    if (self.delegate != nil) {
      [self.delegate mainControllerWillLeaveApplication];
    }
  }
  else if (action == kUnityAdsStateActionVideoStartedPlaying) {
    if (self.delegate != nil) {
      [self.delegate mainControllerStartedPlayingVideo];
    }
  }
  else if (action == kUnityAdsStateActionVideoPlaybackEnded) {
    if (self.delegate != nil) {
      [self.delegate mainControllerVideoEnded];
    }
  }
}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
}


#pragma mark - WebAppController

- (void)webAppReady {
  [self.delegate mainControllerWebViewInitialized];
  dispatch_async(dispatch_get_main_queue(), ^{
    [self checkForVersionAndShowAlertDialog];
    
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIInitComplete, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key}];
  });
}

- (void)checkForVersionAndShowAlertDialog {
  if ([[UnityAdsProperties sharedInstance] expectedSdkVersion] != nil && ![[[UnityAdsProperties sharedInstance] expectedSdkVersion] isEqualToString:[[UnityAdsProperties sharedInstance] adsVersion]]) {
    UALOG_DEBUG(@"Got different sdkVersions, checking further.");
    
    if (![UnityAdsDevice isEncrypted]) {
      if ([UnityAdsDevice isJailbroken]) {
        UALOG_DEBUG(@"Build is not encrypted, but device seems to be jailbroken. Not showing version alert");
        return;
      }
      else {
        // Build is not encrypted and device is not jailbroken, alert dialog is shown that SDK is not the latest version.
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Unity Ads SDK"
                                                        message:@"The Unity Ads SDK you are running is not the current version, please update your SDK"
                                                       delegate:nil
                                              cancelButtonTitle:@"OK"
                                              otherButtonTitles:nil];
        [alert show];
      }
    }
  }
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


#pragma mark - Lifecycle

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [self applyOptionsToCurrentState:@{kUnityAdsNativeEventForceStopVideoPlayback:@true}];
}

- (void)viewDidLoad {
  [self.view setBackgroundColor:[UIColor blackColor]];
  [super viewDidLoad];
}

@end