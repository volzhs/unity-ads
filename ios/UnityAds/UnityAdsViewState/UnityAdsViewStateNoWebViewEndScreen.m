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
  @property (nonatomic, strong) UnityAdsDialog *dialog;
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
  }
  
  [[UnityAdsMainViewController sharedInstance] presentViewController:self.endScreenController animated:NO completion:nil];
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super exitState:options];
  [[UnityAdsMainViewController sharedInstance] dismissViewControllerAnimated:NO completion:nil];
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
    [self showDialog];
  }
  else if ([options objectForKey:kUnityAdsNativeEventHideSpinner] != nil) {
    [self hideDialog];
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

- (void)showDialog {
  if (self.dialog == nil) {
    int dialogWidth = 230;
    int dialogHeight = 76;
    
    CGRect newRect = CGRectMake((self.endScreenController.view.bounds.size.width / 2) - (dialogWidth / 2), (self.endScreenController.view.bounds.size.height / 2) - (dialogHeight / 2), dialogWidth, dialogHeight);
    
    self.dialog = [[UnityAdsDialog alloc] initWithFrame:newRect useSpinner:true useLabel:true useButton:false];
    self.dialog.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleLeftMargin;
    [self.dialog.label setText:@"Loading..."];
  }
  
  if (self.dialog.superview == nil) {
    [self.endScreenController.view addSubview:self.dialog];
  }
}

- (void)hideDialog {
  if (self.dialog != nil) {
    if (self.dialog.superview != nil) {
      [self.dialog removeFromSuperview];
    }
    
    self.dialog = nil;
  }
}

@end
