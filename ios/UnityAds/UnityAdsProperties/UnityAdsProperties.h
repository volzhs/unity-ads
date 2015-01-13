//
//  UnityAdsProperties.h
//  UnityAds
//
//  Created by bluesun on 11/2/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "../UnityAdsItem/UnityAdsRewardItem.h"

@interface UnityAdsProperties : NSObject
  @property (nonatomic, strong) NSString *webViewBaseUrl;
  @property (nonatomic, strong) NSString *analyticsBaseUrl;
  @property (nonatomic, strong) NSString *campaignDataUrl;
  @property (nonatomic, strong) NSString *adsBaseUrl;
  @property (nonatomic, strong) NSString *campaignQueryString;
  @property (nonatomic, strong) NSString *adsGameId;
  @property (nonatomic, strong) NSString *gamerId;
  @property (nonatomic) BOOL testModeEnabled;
  @property (nonatomic, weak) UIViewController *currentViewController;
  @property (nonatomic, assign) int maxNumberOfAnalyticsRetries;
  @property (nonatomic, strong) NSString *expectedSdkVersion;
  @property (nonatomic, strong) NSString *developerId;
  @property (nonatomic, strong) NSString *optionsId;
  @property (nonatomic, strong) NSString *appFiltering;
  @property (nonatomic, strong) NSMutableSet *installedApps;
  @property (nonatomic, strong) NSString *urlSchemeMap;
  @property (nonatomic, strong) NSString *installedAppsUrl;
  @property (nonatomic, assign) BOOL sdkIsCurrent;
  @property (nonatomic, assign) BOOL statusBarWasVisible;
  @property (nonatomic, assign) BOOL unityDeveloperInternalTestMode;
  @property (nonatomic, assign) BOOL sendInternalDetails;

+ (UnityAdsProperties *)sharedInstance;
- (NSString *)createCampaignQueryString;
- (NSString *)createCampaignQueryString:(BOOL)withInstalledApps;
- (void)refreshCampaignQueryString;
- (void)enableUnityDeveloperInternalTestMode;
- (NSString *)adsVersion;

@end
