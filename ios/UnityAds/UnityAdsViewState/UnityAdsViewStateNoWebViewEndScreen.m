//
//  UnityAdsViewStateNoWebViewEndScreen.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateNoWebViewEndScreen.h"
#import "../UnityAdsView/UnityAdsNoWebViewEndScreenViewController.h"
#import "../UnityAdsView/UnityAdsDialog.h"
#import "../UnityAdsView/UnityAdsNativeSpinner.h"


@interface UnityAdsViewStateNoWebViewEndScreen ()
  @property (nonatomic, strong) UnityAdsNoWebViewEndScreenViewController *endScreenController;
@end

@implementation UnityAdsViewStateNoWebViewEndScreen

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeEndScreen;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  
  if (self.endScreenController == nil) {
    [self createEndScreenController];
    [self showSpinnerDialog];
  }
  
  [[UnityAdsMainViewController sharedInstance] presentViewController:self.endScreenController animated:NO completion:nil];
  /*
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeCompleted data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIActionVideoStartedPlaying, kUnityAdsItemKeyKey:[[UnityAdsCampaignManager sharedInstance] getCurrentRewardItem].key, kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
   */
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super exitState:options];
  [[UnityAdsMainViewController sharedInstance] dismissViewControllerAnimated:NO completion:nil];

  // FIX: Doesn't always work right with rewatch (setView:None (null))
  if ([options objectForKey:kUnityAdsWebViewEventDataRewatchKey] == nil || [[options valueForKey:kUnityAdsWebViewEventDataRewatchKey] boolValue] == false) {
    //[[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{}];
  }
}

- (void)willBeShown {
  [super willBeShown];
}

- (void)wasShown {
  [super wasShown];
}

- (void)applyOptions:(NSDictionary *)options {
  [super applyOptions:options];
  
  if ([options objectForKey:kUnityAdsNativeEventShowSpinner] != nil) {
    //[[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:[options objectForKey:kUnityAdsNativeEventShowSpinner]];
  }
  else if ([options objectForKey:kUnityAdsNativeEventHideSpinner] != nil) {
    //[[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:[options objectForKey:kUnityAdsNativeEventHideSpinner]];
  }
  else if ([options objectForKey:kUnityAdsWebViewEventDataClickUrlKey] != nil) {
    [self openAppStoreWithData:options inViewController:self.endScreenController];
  }
}

#pragma mark - Private controller handling

- (void)createEndScreenController {
  UALOG_DEBUG(@"");
  self.endScreenController = [[UnityAdsNoWebViewEndScreenViewController alloc] initWithNibName:nil bundle:nil];
}

- (void)showSpinnerDialog {
  int dialogWidth = 230;
  int dialogHeight = 70;
  
  CGRect newRect = CGRectMake(([[UnityAdsMainViewController sharedInstance] view].bounds.size.width / 2) - (dialogWidth / 2), ([[UnityAdsMainViewController sharedInstance] view].bounds.size.height / 2) - (dialogHeight / 2), dialogWidth, dialogHeight);
  
  UnityAdsDialog *spinnerDialog = [[UnityAdsDialog alloc] initWithFrame:newRect useSpinner:true];
  [spinnerDialog setDrawSpinner:true];
  spinnerDialog.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleLeftMargin;
   
  [self.endScreenController.view addSubview:spinnerDialog];
}

@end
