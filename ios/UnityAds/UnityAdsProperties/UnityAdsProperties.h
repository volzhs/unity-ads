//
//  UnityAdsProperties.h
//  UnityAds
//
//  Created by bluesun on 11/2/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "../UnityAdsCampaign/UnityAdsRewardItem.h"

@interface UnityAdsProperties : NSObject
  @property (nonatomic, strong) NSString *webViewBaseUrl;
  @property (nonatomic, strong) NSString *analyticsBaseUrl;
  @property (nonatomic, strong) NSString *campaignDataUrl;
  @property (nonatomic, strong) NSString *adsBaseUrl;
  @property (nonatomic, strong) NSString *campaignQueryString;
  @property (nonatomic, strong) NSString *adsGameId;
  @property (nonatomic, strong) NSString *gamerId;
  @property (nonatomic) BOOL testModeEnabled;
  @property (nonatomic, assign) UIViewController *currentViewController;

+ (UnityAdsProperties *)sharedInstance;
- (void)refreshCampaignQueryString;

@end
