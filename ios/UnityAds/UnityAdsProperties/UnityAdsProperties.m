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

NSString * const kUnityAdsVersion = @"1.0.3";

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
    [self setCampaignQueryString:[self _createCampaignQueryString]];
  }
  
  return self;
}

- (NSString *)adsVersion {
  return kUnityAdsVersion;
}

- (NSString *)_createCampaignQueryString {
  NSString *queryParams = @"?";
  
  // Mandatory params
  queryParams = [NSString stringWithFormat:@"%@%@=%@", queryParams, kUnityAdsInitQueryParamDeviceIdKey, [UnityAdsDevice md5DeviceId]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamPlatformKey, @"ios"];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamGameIdKey, [self adsGameId]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamOpenUdidKey, [UnityAdsDevice md5OpenUDIDString]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamMacAddressKey, [UnityAdsDevice md5MACAddressString]];
  queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamSdkVersionKey, kUnityAdsVersion];
  
  if ([UnityAdsDevice ODIN1] != nil) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamOdin1IdKey, [UnityAdsDevice ODIN1]];
  }
  
  // Add advertisingTrackingId info if identifier is available
  if ([UnityAdsDevice md5AdvertisingIdentifierString] != nil) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamAdvertisingTrackingIdKey, [UnityAdsDevice md5AdvertisingIdentifierString]];
    queryParams = [NSString stringWithFormat:@"%@&%@=%i", queryParams, kUnityAdsInitQueryParamTrackingEnabledKey, [UnityAdsDevice canUseTracking]];
  }
  
  // Add tracking params if canUseTracking (returns always true < ios6)
  if ([UnityAdsDevice canUseTracking]) {
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamSoftwareVersionKey, [UnityAdsDevice softwareVersion]];
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamHardwareVersionKey, @"unknown"];
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamDeviceTypeKey, [UnityAdsDevice analyticsMachineName]];
    queryParams = [NSString stringWithFormat:@"%@&%@=%@", queryParams, kUnityAdsInitQueryParamConnectionTypeKey, [UnityAdsDevice currentConnectionType]];
  }
  
  if ([self testModeEnabled]) {
    queryParams = [NSString stringWithFormat:@"%@&%@=true", queryParams, kUnityAdsInitQueryParamTestKey];
  }
  
  return queryParams;
}

- (void)refreshCampaignQueryString {
  [self setCampaignQueryString:[self _createCampaignQueryString]];
}

@end
