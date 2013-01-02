//
//  UnityAdsProperties.m
//  UnityAds
//
//  Created by bluesun on 11/2/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsProperties.h"
#import "../UnityAds.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"

NSString * const kUnityAdsVersion = @"1.0.2";

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
    [self setCampaignDataUrl:@"https://impact.applifier.com/mobile/campaigns"];
    //[self setCampaignDataUrl:@"http://192.168.1.152:3500/mobile/campaigns"];
    [self setCampaignQueryString:[self _createCampaignQueryString]];
  }
  
  return self;
}

- (NSString *)adsVersion {
  return kUnityAdsVersion;
}

- (NSString *)_createCampaignQueryString {
  NSString *queryParams = @"?";
  
  queryParams = [NSString stringWithFormat:@"%@deviceId=%@&platform=%@&gameId=%@", queryParams, [UnityAdsDevice md5DeviceId], @"ios", [self adsGameId]];
  queryParams = [NSString stringWithFormat:@"%@&openUdid=%@", queryParams, [UnityAdsDevice md5OpenUDIDString]];
  queryParams = [NSString stringWithFormat:@"%@&macAddress=%@", queryParams, [UnityAdsDevice md5MACAddressString]];
  
  if ([UnityAdsDevice md5AdvertisingIdentifierString] != nil) {
    queryParams = [NSString stringWithFormat:@"%@&advertisingTrackingId=%@", queryParams, [UnityAdsDevice md5AdvertisingIdentifierString]];
    queryParams = [NSString stringWithFormat:@"%@&trackingEnabled=%i", queryParams, [UnityAdsDevice canUseTracking]];
  }
  
  if ([UnityAdsDevice canUseTracking]) {
    queryParams = [NSString stringWithFormat:@"%@&softwareVersion=%@&hardwareVersion=%@&deviceType=%@&apiVersion=%@&connectionType=%@", queryParams, [UnityAdsDevice softwareVersion], @"unknown", [UnityAdsDevice analyticsMachineName], kUnityAdsVersion, [UnityAdsDevice currentConnectionType]];
  }
  
  if ([self testModeEnabled]) {
    queryParams = [NSString stringWithFormat:@"%@&test=true", queryParams];
  }
  
  return queryParams;
}

- (void)refreshCampaignQueryString {
  [self setCampaignQueryString:[self _createCampaignQueryString]];
}

@end
