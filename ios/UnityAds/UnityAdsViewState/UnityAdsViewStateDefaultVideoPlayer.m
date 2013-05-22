//
//  UnityAdsViewStateDefaultVideoPlayer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateDefaultVideoPlayer.h"

#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsRewardItem.h"
#import "../UnityAdsProperties/UnityAdsShowOptionsParser.h"
#import "../UnityAdsData/UnityAdsInstrumentation.h"

@interface UnityAdsViewStateDefaultVideoPlayer ()
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
  if (self.videoController.parentViewController == nil && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
    [[[UnityAdsWebAppController sharedInstance] webView] removeFromSuperview];
    [self.videoController.view addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:self.videoController.view.bounds];
  }
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super enterState:options];
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
}

- (void)applyOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  if (options != nil) {
    if ([options objectForKey:@"sendAbortInstrumentation"] != nil && [[options objectForKey:@"sendAbortInstrumentation"] boolValue] == true) {
      NSString *eventType = nil;
      
      if ([options objectForKey:@"type"] != nil) {
        
        eventType = [options objectForKey:@"type"];
        [UnityAdsInstrumentation gaInstrumentationVideoAbort:[[UnityAdsCampaignManager sharedInstance] selectedCampaign] withValuesFrom:@{kUnityAdsGoogleAnalyticsEventValueKey:eventType, kUnityAdsGoogleAnalyticsEventBufferingDurationKey:@([[[UnityAdsCampaignManager sharedInstance] selectedCampaign] geBufferingDuration])}];
      }
    }
  }
  
  [super applyOptions:options];
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
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  
  if (!self.waitingToBeShown && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    UALOG_DEBUG(@"Placing videoview to hierarchy");
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
  }
}

- (void)videoPlayerEncounteredError {
  UALOG_DEBUG(@"");
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyBuffering}];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventVideoCompleted data:@{kUnityAdsNativeEventCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowError data:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyVideoPlaybackError}];
  
  if ([[UnityAdsWebAppController sharedInstance] webView].superview != nil) {
    [[[UnityAdsWebAppController sharedInstance] webView] removeFromSuperview];
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
  }
  
  if (![[UnityAdsShowOptionsParser sharedInstance] noOfferScreen]) {
    [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeOfferScreen withOptions:nil];
  }
  //[self dismissVideoController];
}

- (void)videoPlayerPlaybackEnded {
  UALOG_DEBUG(@"");
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoPlaybackEnded];
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