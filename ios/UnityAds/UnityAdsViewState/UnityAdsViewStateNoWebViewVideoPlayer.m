//
//  UnityAdsViewStateNoWebViewVideoPlayer.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateNoWebViewVideoPlayer.h"
#import "../UnityAdsView/UnityAdsDialog.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsData/UnityAdsInstrumentation.h"

@interface UnityAdsViewStateNoWebViewVideoPlayer () <UIWebViewDelegate>
  @property (nonatomic, strong) UnityAdsDialog *spinnerDialog;
  @property (nonatomic, strong) UIWebView *webView;
  @property (nonatomic, assign) BOOL abortInstrumentationSent;
@end

@implementation UnityAdsViewStateNoWebViewVideoPlayer

@synthesize webView = _webView;
@synthesize spinnerDialog = _spinnerDialog;

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeVideoPlayer;
}

- (void)willBeShown {
  [super willBeShown];
  [self showSpinner];
  
  [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:nil];
  UnityAdsCampaign *campaign = [[[UnityAdsCampaignManager sharedInstance] getViewableCampaigns] objectAtIndex:0];
  
  if (campaign != nil) {
    [[UnityAdsCampaignManager sharedInstance] setSelectedCampaign:campaign];
  }
}

- (void)wasShown {
  [super wasShown];
  
  if (self.videoController.parentViewController == nil && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
    [self moveSpinnerToVideoController];
  }
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  self.abortInstrumentationSent = false;
  [super enterState:options];
  [self createVideoController:self];
  [self showSpinner];
  
  if (!self.waitingToBeShown) {
    [self showPlayerAndPlaySelectedVideo];
  }
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super exitState:options];
  [self hideSpinner];
  [[NSURLCache sharedURLCache] removeAllCachedResponses];
}

- (void)applyOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  

  
  [super applyOptions:options];
}


#pragma mark - Video

- (void)videoPlayerStartedPlaying {
  UALOG_DEBUG(@"");
  
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoStartedPlaying];
  }
  
  [self hideSpinner];

  if (!self.waitingToBeShown && [[UnityAdsMainViewController sharedInstance] presentedViewController] != self.videoController) {
    [[UnityAdsMainViewController sharedInstance] presentViewController:self.videoController animated:NO completion:nil];
  }
  
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil &&
      ![[UnityAdsCampaignManager sharedInstance] selectedCampaign].nativeTrackingQuerySent &&
      [[UnityAdsCampaignManager sharedInstance] selectedCampaign].customClickURL != nil &&
      [[[[UnityAdsCampaignManager sharedInstance] selectedCampaign].customClickURL absoluteString] length] > 4) {
   
    UALOG_DEBUG(@"Sending tracking call");
    [[UnityAdsCampaignManager sharedInstance] selectedCampaign].nativeTrackingQuerySent = true;
    
    if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign].customClickURL != nil) {
      [self createWebViewAndSendTracking:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].customClickURL];
    }
  }
}

- (void)videoPlayerEncounteredError {
  UALOG_DEBUG(@"");
  [self hideSpinner];
  [self dismissVideoController];
}

- (void)videoPlayerPlaybackEnded {
  UALOG_DEBUG(@"");

  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionVideoPlaybackEnded];
  }
  
  [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeEndScreen withOptions:nil];
}

- (void)videoPlayerReady {
	UALOG_DEBUG(@"");
  if (![self.videoController isPlaying])
    [self showPlayerAndPlaySelectedVideo];
}


- (void)showPlayerAndPlaySelectedVideo {
	UALOG_DEBUG(@"");
  
  if (![self canViewSelectedCampaign]) return;
  [self startVideoPlayback:true withDelegate:self];
}


- (void)showSpinner {
  if (self.spinnerDialog == nil) {
    int dialogWidth = 230;
    int dialogHeight = 76;
    
    CGRect newRect = CGRectMake(([[UnityAdsMainViewController sharedInstance] view].bounds.size.width / 2) - (dialogWidth / 2), ([[UnityAdsMainViewController sharedInstance] view].bounds.size.height / 2) - (dialogHeight / 2), dialogWidth, dialogHeight);
    
    self.spinnerDialog = [[UnityAdsDialog alloc] initWithFrame:newRect useSpinner:true useLabel:true useButton:false];
    self.spinnerDialog.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleLeftMargin;
    
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:self.spinnerDialog];
  }
}

- (void)hideSpinner {
  if (self.spinnerDialog != nil) {
    [self.spinnerDialog removeFromSuperview];
    self.spinnerDialog = nil;
  }
}

- (void)moveSpinnerToVideoController {
  if (self.spinnerDialog != nil) {
    [self.spinnerDialog removeFromSuperview];
    
    int spinnerWidth = self.spinnerDialog.bounds.size.width;
    int spinnerHeight = self.spinnerDialog.bounds.size.height;
    
    CGRect newRect = CGRectMake((self.videoController.view.bounds.size.width / 2) - (spinnerWidth / 2), (self.videoController.view.bounds.size.height / 2) - (spinnerHeight / 2), spinnerWidth, spinnerHeight);
    
    [self.spinnerDialog setFrame:newRect];
    [self.videoController.view addSubview:self.spinnerDialog];
  }
}

- (void)createWebViewAndSendTracking:(NSURL *)trackingUrl {
  if (self.webView == nil) {
    self.webView = [[UIWebView alloc] initWithFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
    self.webView.delegate = self;
    self.webView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.webView.scalesPageToFit = NO;
    [self.webView setBackgroundColor:[UIColor blackColor]];
  }
  
  [self.webView loadRequest:[NSURLRequest requestWithURL:trackingUrl]];
}


#pragma mark - UIWebViewDelegate

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
	return YES;
}

- (void)webViewDidStartLoad:(UIWebView *)webView {
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
  [self.webView setDelegate:nil];
  [[NSURLCache sharedURLCache] removeAllCachedResponses];
  self.webView = nil;
  return;
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error {
  [self.webView setDelegate:nil];
  [[NSURLCache sharedURLCache] removeAllCachedResponses];
  self.webView = nil;
  return;
}


@end