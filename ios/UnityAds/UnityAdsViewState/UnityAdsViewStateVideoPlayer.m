//
//  UnityAdsViewStateVideoPlayer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateVideoPlayer.h"
#import "../UnityAdsVideo/UnityAdsVideoViewController.h"
#import "UnityAdsAppSheetManager.h"
#import "UnityAdsZoneManager.h"
#import "UnityAdsIncentivizedZone.h"
#import "UnityAdsWebAppController.h"

@implementation UnityAdsViewStateVideoPlayer

- (void)enterState:(NSDictionary *)options {
  [super enterState:options];
  
  UALOG_DEBUG(@"campaign=%@  byPassAppSheet=%i", [[UnityAdsCampaignManager sharedInstance] selectedCampaign], [[UnityAdsCampaignManager sharedInstance] selectedCampaign].bypassAppSheet);
  
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil &&
      ![[UnityAdsCampaignManager sharedInstance] selectedCampaign].bypassAppSheet &&
      ![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [[UnityAdsAppSheetManager sharedInstance] preloadAppSheetWithId:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].itunesID];
  }
  
  self.checkIfWatched = YES;
  if ([options objectForKey:kUnityAdsWebViewEventDataRewatchKey] != nil && [[options valueForKey:kUnityAdsWebViewEventDataRewatchKey] boolValue] == true) {
    self.checkIfWatched = NO;
  }
  
  [self createVideoController:self];
  
  if (!self.waitingToBeShown) {
    [self showPlayerAndPlaySelectedVideo];
  }
  
  if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:[[UnityAdsMainViewController sharedInstance] view]]) {
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
    
    [[[UnityAdsMainViewController sharedInstance] view] bringSubviewToFront:[[UnityAdsWebAppController sharedInstance] webView]];
  }
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super exitState:options];
  [self dismissVideoController];
}

- (void)applyOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  if (options != nil) {
    if ([options objectForKey:kUnityAdsNativeEventForceStopVideoPlayback] != nil) {
      [self destroyVideoController];
    }
  }
}

- (void)destroyVideoController {
  if (self.videoController != nil) {
    [self.videoController forceStopVideoPlayer];
    self.videoController.delegate = nil;
  }
  
  self.videoController = nil;
}

- (void)createVideoController:(id)targetDelegate {
  self.videoController = [[UnityAdsVideoViewController alloc] initWithNibName:nil bundle:nil];
  self.videoController.delegate = targetDelegate;
}

- (void)dismissVideoController {
  if ([[[UnityAdsMainViewController sharedInstance] presentedViewController] isEqual:self.videoController])
    [[[UnityAdsMainViewController sharedInstance] presentedViewController] dismissViewControllerAnimated:NO completion:nil];
  
  [self destroyVideoController];
}

- (BOOL)canViewSelectedCampaign {
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed && self.checkIfWatched) {
    UALOG_DEBUG(@"Trying to watch a campaign that is already viewed!");
    return false;
  }
  
  return true;
}

- (void)startVideoPlayback:(BOOL)createVideoController withDelegate:(id)videoControllerDelegate {
  if ([[UnityAdsMainViewController sharedInstance] isOpen]) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
      [[UnityAdsCampaignManager sharedInstance] cacheNextCampaignAfter:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
    });
    [self.videoController playCampaign:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
  }
}

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeVideoPlayer;
}

- (void)willBeShown {
  [super willBeShown];
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone noOfferScreen]) {
    [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:nil];
    
    UnityAdsCampaign *campaign = [[[UnityAdsCampaignManager sharedInstance] getViewableCampaigns] objectAtIndex:0];
    
    if (campaign != nil) {
      [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:campaign];
    }
  }
}

- (void)wasShown {
  [super wasShown];
  if (self.videoController.parentViewController == nil && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
    [[[UnityAdsWebAppController sharedInstance] webView] removeFromSuperview];
    [self.videoController.view addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:self.videoController.view.bounds];
  }
}

#pragma mark - Video

- (void)videoPlayerStartedPlaying {
  UALOG_DEBUG(@"");
  
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoStartedPlaying];
  }
  
  if ([[UnityAdsWebAppController sharedInstance] webView].superview != nil) {
    [[[UnityAdsWebAppController sharedInstance] webView] removeFromSuperview];
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
  }
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  
  // Set completed view for the webview right away, so we don't get flickering after videoplay from start->end
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if([currentZone isIncentivized]) {
    id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsItemKeyKey:[itemManager getCurrentItem].key, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  } else {
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  }
  
  if (!self.waitingToBeShown && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    UALOG_DEBUG(@"Placing videoview to hierarchy");
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
  }
}

- (void)videoPlayerEncounteredError {
  UALOG_DEBUG(@"");
  [[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed = YES;
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventVideoCompleted data:@{kUnityAdsNativeEventCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if([currentZone isIncentivized]) {
    id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoPlaybackError, kUnityAdsItemKeyKey:[itemManager getCurrentItem].key, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  } else {
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoPlaybackError, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  }
  
  [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeEndScreen withOptions:nil];
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowError data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyVideoPlaybackError}];
  
  if ([[UnityAdsWebAppController sharedInstance] webView].superview != nil) {
    [[[UnityAdsWebAppController sharedInstance] webView] removeFromSuperview];
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
  }
}

- (void)videoPlayerPlaybackEnded:(BOOL)skipped {
  UALOG_DEBUG(@"");
  if (self.delegate != nil) {
    if(skipped) {
      [self.delegate stateNotification:kUnityAdsStateActionVideoPlaybackSkipped];
    } else {
      [self.delegate stateNotification:kUnityAdsStateActionVideoPlaybackEnded];
    }
  }
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventVideoCompleted data:@{kUnityAdsNativeEventCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeEndScreen withOptions:nil];
}

- (void)videoPlayerReady {
	UALOG_DEBUG(@"");
  
  if (![self.videoController isPlaying])
    [self showPlayerAndPlaySelectedVideo];
}

- (void)showPlayerAndPlaySelectedVideo {
  if ([[UnityAdsMainViewController sharedInstance] isOpen]) {
    UALOG_DEBUG(@"");
    
    if (![self canViewSelectedCampaign]) return;
    
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
    
    [self startVideoPlayback:true withDelegate:self];
  }
}

@end
