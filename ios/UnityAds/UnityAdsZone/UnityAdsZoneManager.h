//
//  UnityAdsZoneManager.h
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "UnityAdsZone.h"

@interface UnityAdsZoneManager : NSObject

+ (id)sharedInstance;

- (int)addZones:(NSDictionary *)zones;
- (void)clearZones;

- (NSDictionary *)getZones;
- (UnityAdsZone *)getZone:(NSString *)zoneId;
- (BOOL)removeZone:(NSString *)zoneId;

- (BOOL)setCurrentZone:(NSString *)zoneId;
- (UnityAdsZone *)getCurrentZone;

- (NSUInteger)zoneCount;

@end
