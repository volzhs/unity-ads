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
  @property (nonatomic, strong) UnityAdsViewState *currentViewState;
  @property (nonatomic, strong) UnityAdsViewState *previousViewState;
  @property (nonatomic, strong) NSMutableArray *viewStateHandlers;
  @property (nonatomic, assign) BOOL simulatorOpeningSupportCallSent;
@end

@implementation UnityAdsMainViewController

@synthesize currentViewState = _currentViewState;
@synthesize previousViewState = _previousViewState;
@synthesize viewStateHandlers = _viewStateHandlers;


- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  
    if (self) {
      // Add notification listener
      NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
      [notificationCenter addObserver:self selector:@selector(notificationHandler:) name:UIApplicationDidEnterBackgroundNotification object:nil];
      self.simulatorOpeningSupportCallSent = false;
      self.isClosing = false;
    }
  
    return self;
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  
  if ([UnityAdsDevice isSimulator] && !self.simulatorOpeningSupportCallSent) {
    UALOG_DEBUG(@"");
    self.simulatorOpeningSupportCallSent = true;
    [self.currentViewState wasShown];
    [self.delegate mainControllerDidOpen];
  }
}

- (void)viewWillDisappear:(BOOL)animated {
  [super viewWillDisappear:animated];
  
  if (self.isClosing && [[UnityAdsProperties sharedInstance] statusBarWasVisible]) {
    UALOG_DEBUG(@"Statusbar was originally visible. Bringing it back.");
    [[UnityAdsProperties sharedInstance] setStatusBarWasVisible:false];
    [[UIApplication sharedApplication] setStatusBarHidden:false];
  }
}

- (void)viewDidDisappear:(BOOL)animated {
  [super viewDidDisappear:animated];
  self.isClosing = false;
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  
  if (![UIApplication sharedApplication].statusBarHidden) {
    UALOG_DEBUG(@"Hiding statusbar");
    [[UnityAdsProperties sharedInstance] setStatusBarWasVisible:true];
    [[UIApplication sharedApplication] setStatusBarHidden:true];
  }
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

- (void)applyViewStateHandler:(UnityAdsViewState *)viewState {
  if (viewState != nil) {
    viewState.delegate = self;
    if (self.viewStateHandlers == nil) {
      self.viewStateHandlers = [[NSMutableArray alloc] init];
    }
    [self.viewStateHandlers addObject:viewState];
  }
}

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
    if (self.currentViewState != nil && [[self currentViewState] getStateType] != requestedState) {
      [self.currentViewState exitState:options];
      self.previousViewState = self.currentViewState;
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

- (UnityAdsViewState *)getCurrentViewState {
  return self.currentViewState;
}

- (UnityAdsViewState *)getPreviousViewState {
  return self.previousViewState;
}

- (BOOL)closeAds:(BOOL)forceMainThread withAnimations:(BOOL)animated withOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  self.isClosing = true;
  
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) return NO;
  
  if (forceMainThread) {
    dispatch_async(dispatch_get_main_queue(), ^{
      [self _dismissMainViewController:forceMainThread withAnimations:animated];
    });
  }
  else {
    [self _dismissMainViewController:forceMainThread withAnimations:animated];
  }
  
  if ([UnityAdsDevice isSimulator]) {
    self.simulatorOpeningSupportCallSent = false;
  }
  
  return YES;
}

- (BOOL)openAds:(BOOL)animated inState:(UnityAdsViewStateType)requestedState withOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  if ([[UnityAdsProperties sharedInstance] currentViewController] == nil) return NO;

  [self selectState:requestedState];

  dispatch_async(dispatch_get_main_queue(), ^{
    if ([self hasState:requestedState]) {
      [self.currentViewState willBeShown];
      [self.delegate mainControllerWillOpen];
      [self changeState:requestedState withOptions:options];
      
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
    [self applyOptionsToCurrentState:@{kUnityAdsNativeEventForceStopVideoPlayback:@true, @"sendAbortInstrumentation":@true, @"type":kUnityAdsGoogleAnalyticsEventVideoAbortExit}];
    
    
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
  } else if (action == kUnityAdsStateActionVideoPlaybackSkipped) {
    if (self.delegate != nil) {
      [self.delegate mainControllerVideoSkipped];
    }
  }
}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
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