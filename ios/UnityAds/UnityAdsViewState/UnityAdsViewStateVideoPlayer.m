//
//  UnityAdsViewStateVideoPlayer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateVideoPlayer.h"
#import "../UnityAdsVideo/UnityAdsVideoViewController.h"

@implementation UnityAdsViewStateVideoPlayer

- (void)enterState:(NSDictionary *)options {
  [super enterState:options];
  
  UALOG_DEBUG(@"campaign=%@  byPassAppSheet=%i", [[UnityAdsCampaignManager sharedInstance] selectedCampaign], [[UnityAdsCampaignManager sharedInstance] selectedCampaign].bypassAppSheet);
  
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil &&
      ![[UnityAdsCampaignManager sharedInstance] selectedCampaign].bypassAppSheet &&
      ![[UnityAdsCampaignManager sharedInstance] selectedCampaign].viewed) {
    [self preloadAppSheetWithId:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].itunesID];
  }
  
  self.checkIfWatched = YES;
  if ([options objectForKey:kUnityAdsWebViewEventDataRewatchKey] != nil && [[options valueForKey:kUnityAdsWebViewEventDataRewatchKey] boolValue] == true) {
    self.checkIfWatched = NO;
  }
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super exitState:options];
  [self dismissVideoController];
  
  if (self.storeController != nil) {
    self.storeController = nil;
  }
}

- (void)applyOptions:(NSDictionary *)options {
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
    [self.videoController playCampaign:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
  }
}

@end
