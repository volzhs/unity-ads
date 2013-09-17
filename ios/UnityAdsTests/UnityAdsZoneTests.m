//
//  UnityAdsZoneTests.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsZoneTests.h"

#import "UnityAdsSBJsonParser.h"

@implementation UnityAdsZoneTests

- (void)testZone {
  id parser = [[UnityAdsSBJsonParser alloc] init];
  id json = [parser objectWithString:@"{\"id\": \"testId\", \"name\": \"testName\"}"];
  id test = [UnityAdsZoneParser parseZone:json];
  id testZoneId = [test getZoneId];
  STAssertTrue([testZoneId isEqual:@"testId"], @"zoneId does not match");
}

@end
