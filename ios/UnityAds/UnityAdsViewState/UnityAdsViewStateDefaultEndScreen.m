//
//  UnityAdsViewStateDefaultEndScreen.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateDefaultEndScreen.h"

#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsItem/UnityAdsRewardItem.h"

@implementation UnityAdsViewStateDefaultEndScreen

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeEndScreen;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  
  if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:[[UnityAdsMainViewController sharedInstance] view]]) {
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
    
    [[[UnityAdsMainViewController sharedInstance] view] bringSubviewToFront:[[UnityAdsWebAppController sharedInstance] webView]];
  }
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  [super exitState:options];
  
  if ([options objectForKey:kUnityAdsWebViewEventDataRewatchKey] == nil || [[options valueForKey:kUnityAdsWebViewEventDataRewatchKey] boolValue] == false) {
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeNone data:@{}];
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
  
  if ([options objectForKey:kUnityAdsWebViewEventDataClickUrlKey] != nil) {
    [self openAppStoreWithData:options inViewController:[UnityAdsMainViewController sharedInstance]];
  }
}

@end
