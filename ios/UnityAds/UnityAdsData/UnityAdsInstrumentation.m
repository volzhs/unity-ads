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
#import "../UnityAdsWebView/UnityAdsWebAppController.h"

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

+ (NSDictionary *)makeEventFromEvent:(NSString *)eventType withData:(NSDictionary *)data {
  return @{kUnityAdsGoogleAnalyticsEventTypeKey:eventType, @"data":data};
}

static NSMutableArray *unsentEvents;

+ (void)sendGAInstrumentationEvent:(NSString *)eventType withData:(NSDictionary *)data {
  NSDictionary *eventDataToSend = [self makeEventFromEvent:eventType withData:data];
  
  if (eventDataToSend != nil) {
    if ([[UnityAdsWebAppController sharedInstance] webViewInitialized] && [[UnityAdsWebAppController sharedInstance] webViewLoaded]) {
      NSMutableArray *eventsArray = [NSMutableArray array];
      
      if (unsentEvents != nil) {
        [eventsArray addObjectsFromArray:unsentEvents];
        [unsentEvents removeAllObjects];
        unsentEvents = nil;
      }
      
      [eventsArray addObject:eventDataToSend];
      NSDictionary *finalData = @{@"events":eventsArray};
      [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsGoogleAnalyticsEventKey data:finalData];
    }
    else {
      if (unsentEvents == nil) {
        unsentEvents = [NSMutableArray array];
      }
      
      [unsentEvents addObject:eventDataToSend];
    }
  }
}

+ (void)gaInstrumentationVideoPlay:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  NSDictionary *basicData = [self getBasicGAVideoProperties:campaign];
  NSDictionary *finalData = [self mergeDictionaries:basicData dictionaryToMerge:additionalValues];
  [self sendGAInstrumentationEvent:kUnityAdsGoogleAnalyticsEventTypeVideoPlay withData:finalData];
}

+ (void)gaInstrumentationVideoError:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  NSDictionary *basicData = [self getBasicGAVideoProperties:campaign];
  NSDictionary *finalData = [self mergeDictionaries:basicData dictionaryToMerge:additionalValues];
  [self sendGAInstrumentationEvent:kUnityAdsGoogleAnalyticsEventTypeVideoError withData:finalData];
}

+ (void)gaInstrumentationVideoAbort:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  NSDictionary *basicData = [self getBasicGAVideoProperties:campaign];
  NSDictionary *finalData = [self mergeDictionaries:basicData dictionaryToMerge:additionalValues];
  [self sendGAInstrumentationEvent:kUnityAdsGoogleAnalyticsEventTypeVideoAbort withData:finalData];
}

+ (void)gaInstrumentationVideoCaching:(UnityAdsCampaign *)campaign withValuesFrom:(NSDictionary *)additionalValues {
  NSDictionary *basicData = [self getBasicGAVideoProperties:campaign];
  NSDictionary *finalData = [self mergeDictionaries:basicData dictionaryToMerge:additionalValues];
  [self sendGAInstrumentationEvent:kUnityAdsGoogleAnalyticsEventTypeVideoCaching withData:finalData];
}

@end