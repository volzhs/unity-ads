//
//  UnityAdsZoneParser.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsZoneParser.h"
#import "UnityAdsIncentivizedZone.h"
#import "UnityAdsConstants.h"

@implementation UnityAdsZoneParser

+ (NSDictionary *)parseZones:(NSArray *)zoneArray {
  NSMutableDictionary *zones = [[NSMutableDictionary alloc] init];
  [zoneArray enumerateObjectsUsingBlock:^(id rawZone, NSUInteger index, BOOL *stop) {
    id zone = [UnityAdsZoneParser parseZone:rawZone];
    if(zone != nil) {
      [zones setObject:zone forKey:[zone getZoneId]];
    }
  }];
  return zones;
}

+ (UnityAdsZone *)parseZone:(NSDictionary *)rawZone {
  NSString * zoneId = [rawZone objectForKey:kUnityAdsZoneIdKey];
  if([zoneId length] == 0) {
    return nil;
  }
  
  NSString * zoneName = [rawZone objectForKey:kUnityAdsZoneNameKey];
  if([zoneName length] == 0) {
    return nil;
  }
  
  
  
  BOOL isIncentivized = [[rawZone objectForKey:kUnityAdsZoneIsIncentivizedKey] boolValue];
  if(isIncentivized) {
    return [[UnityAdsIncentivizedZone alloc] initWithData:rawZone];
  } else {
    return [[UnityAdsZone alloc] initWithData:rawZone];
  }
}

@end
