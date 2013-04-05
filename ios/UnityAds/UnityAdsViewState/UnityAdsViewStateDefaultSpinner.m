//
//  UnityAdsViewStateDefaultSpinner.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateDefaultSpinner.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAds.h"

@implementation UnityAdsViewStateDefaultSpinner

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeSpinner;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:options];

}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super exitState:options];
  [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:options];
}

- (void)willBeShown {
  [super willBeShown];
}

- (void)wasShown {
  [super wasShown];
}

@end
