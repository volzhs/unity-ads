//
//  UnityAdsZoneParser.h
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "UnityAdsZone.h"

@interface UnityAdsZoneParser : NSObject

+ (NSDictionary *)parseZones:(NSArray *)zoneArray;
+ (UnityAdsZone *)parseZone:(NSDictionary *)zone;

@end
