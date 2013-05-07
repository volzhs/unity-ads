//
//  UnityAdsInstrumentation.m
//  UnityAds
//
//  Created by Pekka Palmu on 5/7/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsInstrumentation.h"

#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"

@implementation UnityAdsInstrumentation

+ (NSDictionary *)getBasicGAVideoProperties:(UnityAdsCampaign *)campaign {
  if (campaign != nil) {
    NSString *videoPlayType = kUnityAdsGoogleAnalyticsEventVideoPlayStream;
    
    if (campaign.shouldCacheVideo) {
      videoPlayType = kUnityAdsGoogleAnalyticsEventVideoPlayCached;
    }
    
    NSString *connectionType = [UnityAdsDevice currentConnectionType];
    NSDictionary *data = @{kUnityAdsGoogleAnalyticsEventVideoPlaybackTypeKey:videoPlayType, kUnityAdsGoogleAnalyticsEventConnectionTypeKey:connectionType, kUnityAdsGoogleAnalyticsEventCampaignIdKey:campaign.id};

    return data;
  }
  
  return nil;
}

+ (NSDictionary *)mergeDictionaries:(NSDictionary *)dict1 dictionaryToMerge:(NSDictionary *)dict2 {
  NSMutableDictionary *finalData = [NSMutableDictionary dictionary];
  
  if (dict1 != nil) {
    [finalData addEntriesFromDictionary:dict1];
  }
  
  if (dict2 != nil) {
    [finalData addEntriesFromDictionary:dict2];
  }
  
  return finalData;
}

+ (NSArray *)getUnsentGAInstrumentationEvents {
  return nil;
}

+ (void)sendGAInstrumentationEvent:(NSString *)eventType {
  
}

+ (void)gaInstrumentationVideoPlay:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  NSDictionary *basicData = [self getBasicGAVideoProperties:campaign];
  NSDictionary *finalData = [self mergeDictionaries:basicData dictionaryToMerge:additionalValues];
}

+ (void)gaInstrumentationVideoError:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  
}

+ (void)gaInstrumentationVideoAbort:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  
}

+ (void)gaInstrumentationVideoCaching:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  
}

@end
