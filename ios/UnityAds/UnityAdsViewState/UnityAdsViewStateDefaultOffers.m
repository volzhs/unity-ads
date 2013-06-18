//
//  UnityAdsViewStateDefaultOffers.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateDefaultOffers.h"

#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsRewardItem.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsShowOptionsParser.h"

@implementation UnityAdsViewStateDefaultOffers

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeOfferScreen;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  [self placeToViewHiearchy];
}

- (void)willBeShown {
  [super willBeShown];
  
  [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIOpen, kUnityAdsRewardItemKeyKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id}];
  
  [self placeToViewHiearchy];
}

- (void)wasShown {
  [super wasShown];
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super exitState:options];
}

- (void)placeToViewHiearchy {
  if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:[[UnityAdsMainViewController sharedInstance] view]]) {
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
    
    [[[UnityAdsMainViewController sharedInstance] view] bringSubviewToFront:[[UnityAdsWebAppController sharedInstance] webView]];
  }
}

- (void)applyOptions:(NSDictionary *)options {
  [super applyOptions:options];
  
  if ([options objectForKey:kUnityAdsWebViewEventDataClickUrlKey] != nil) {
    [self openAppStoreWithData:options inViewController:[UnityAdsMainViewController sharedInstance]];
  }
}

@end
