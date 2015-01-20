//
//  UnityAdsProperties.m
//  UnityAds
//
//  Created by bluesun on 11/2/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsProperties.h"
#import "UnityAdsConstants.h"
#import "../UnityAds.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"
#import "UnityAdsCacheManager.h"

NSString * const kUnityAdsVersion = @"1310";

@implementation UnityAdsProperties

static UnityAdsProperties *sharedProperties = nil;

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedProperties == nil)
      sharedProperties = [[UnityAdsProperties alloc] init];
	}
	
	return sharedProperties;
}

- (UnityAdsProperties *)init {
  if (self = [super init]) {
    [self setMaxNumberOfAnalyticsRetries:5];
    [self setCampaignDataUrl:@"https://impact.applifier.com/mobile/campaigns"];
    [self setCampaignQueryString:[self createCampaignQueryString]];
    [self setSdkIsCurrent:true];
    [self setExpectedSdkVersion:@"0"];
    [self setAppFiltering:@"off"];
    [self setInstalledApps:[[NSMutableSet alloc] init]];
  }
  
  return self;
}

- (NSString *)adsVersion {
  return kUnityAdsVersion;
}

- (NSString *)createCampaignQueryString {
  return [self createCampaignQueryString:YES];
}

- (NSString *)createCampaignQueryString:(BOOL)withInstalledApps {
  NSString *queryParams = @"?";
  
  // Mandatory params
  queryParams = [NSString stringWithFormat:@"%@%@=%@", queryParams, kUnityAdsInitQueryParamPlatformKey, @"ios"];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamGameIdKey, [self adsGameId]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamSdkVersionKey, kUnityAdsVersion];
  
  if ([UnityAdsDevice getIOSMajorVersion] < 7) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamMacAddressKey, [UnityAdsDevice md5MACAddressString]];
  }
  
  id advertisingIdentifierString = [UnityAdsDevice advertisingIdentifier];
  id md5AdvertisingIdentifierString = [UnityAdsDevice md5AdvertisingIdentifierString];
  
  // Add advertisingTrackingId info if identifier is available
  if (advertisingIdentifierString != nil) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamRawAdvertisingTrackingIdKey, advertisingIdentifierString];
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamAdvertisingTrackingIdKey, md5AdvertisingIdentifierString];
    queryParams = [NSString stringWithFormat:@"%@&%@=%i", queryParams, kUnityAdsInitQueryParamTrackingEnabledKey, [UnityAdsDevice canUseTracking]];
  }
  
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamSoftwareVersionKey, [UnityAdsDevice softwareVersion]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamDeviceTypeKey, [UnityAdsDevice analyticsMachineName]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamConnectionTypeKey, [UnityAdsDevice currentConnectionType]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamIdentifierForVendor, [UnityAdsDevice identifierForVendor]];
  
  id networkType = [UnityAdsDevice getNetworkType];
  if(networkType != nil) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamNetworkTypeKey, networkType];
  }
  
  unsigned long long cachingSpeed = [[UnityAdsCacheManager sharedInstance] cachingSpeed];
  if(cachingSpeed > 0.0) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%llu", queryParams, kUnityAdsInitQueryParamCachingSpeedKey, cachingSpeed];
  }
  
  if ([self testModeEnabled]) {
    queryParams = [NSString stringWithFormat:@"%@&%@=true", queryParams, kUnityAdsInitQueryParamTestKey];
    
    if ([self optionsId] != nil) {
      queryParams = [NSString stringWithFormat:@"%@&optionsId=%@", queryParams, [self optionsId]];
    }
    if ([self developerId] != nil) {
      queryParams = [NSString stringWithFormat:@"%@&developerId=%@", queryParams, [self developerId]];
    }
  }
  else {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamEncryptionKey, [UnityAdsDevice isEncrypted] ? @"true" : @"false"];
  }
  
  if(withInstalledApps && [[self installedApps] count] > 0) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamAppFilterListKey, [[self installedApps].allObjects componentsJoinedByString:@","]];
  }
  
  if (self.sendInternalDetails) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamSendInternalDetailsKey, @"true"];
  }
  
  return queryParams;
}

- (void)refreshCampaignQueryString {
  [self setCampaignQueryString:[self createCampaignQueryString]];
}

- (void)enableUnityDeveloperInternalTestMode {
  [self setCampaignDataUrl:@"https://impact.staging.applifier.com/mobile/campaigns"];
  [self setCampaignQueryString:[self createCampaignQueryString]];
  [self setUnityDeveloperInternalTestMode:true];
}


@end
