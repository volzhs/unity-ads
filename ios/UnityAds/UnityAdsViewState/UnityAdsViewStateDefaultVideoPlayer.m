//
//  UnityAdsViewStateDefaultVideoPlayer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateDefaultVideoPlayer.h"

#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsRewardItem.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAdsProperties/UnityAdsShowOptionsParser.h"
#import "../UnityAds.h"

@interface UnityAdsViewStateDefaultVideoPlayer ()
  @property (nonatomic, strong) UnityAdsVideoViewController *videoController;
@end


@implementation UnityAdsViewStateDefaultVideoPlayer

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeVideoPlayer;
}

- (void)willBeShown {
  [super willBeShown];
  
  if ([[UnityAdsShowOptionsParser sharedInstance] noOfferScreen]) {
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
    
    [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:nil];
    
    UnityAdsCampaign *campaign = [[[UnityAdsCampaignManager sharedInstance] getViewableCampaigns] objectAtIndex:0];
    
    if (campaign != nil) {
      [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:campaign];
    }
  }
}

- (void)wasShown {
  [super wasShown];
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  
  BOOL checkIfWatched = YES;
  if ([options objectForKey:kUnityAdsWebViewEventDataRewatchKey] != nil && [[options valueForKey:kUnityAdsWebViewEventDataRewatchKey] boolValue] == true) {
    checkIfWatched = NO;
  }
  
  [self showPlayerAndPlaySelectedVideo:checkIfWatched];
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super exitState:options];
  [self _dismissVideoController];
}

- (void)applyOptions:(NSDictionary *)options {
  [super applyOptions:options];
  
  if (options != nil) {
    if ([options objectForKey:kUnityAdsNativeEventForceStopVideoPlayback] != nil) {
      [self _destroyVideoController];
    }
  }
}


#pragma mark - Video

- (void)videoPlayerStartedPlaying {
  UALOG_DEBUG(@"");
  
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoStartedPlaying];
  }
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];

  // Set completed view for the webview right away, so we don't get flickering after videoplay from start->end
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  
  [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
}

- (void)videoPlayerEncounteredError {
  UALOG_DEBUG(@"");
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventVideoCompleted data:@{kUnityAdsNativeEventCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowError data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyVideoPlaybackError}];
  
  [self _dismissVideoController];
}

- (void)videoPlayerPlaybackEnded {
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoPlaybackEnded];
  }
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventVideoCompleted data:@{kUnityAdsNativeEventCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeEndScreen withOptions:nil];
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
  if (self.videoController != nil) {
    [self.videoController forceStopVideoPlayer];
    self.videoController.delegate = nil;
  }
  
  self.videoController = nil;
}

- (void)_dismissVideoController {
  if ([[[UnityAdsMainViewController sharedInstance] presentedViewController] isEqual:self.videoController])
    [[[UnityAdsMainViewController sharedInstance] presentedViewController] dismissViewControllerAnimated:NO completion:nil];
  
  [self _destroyVideoController];
}

@end
