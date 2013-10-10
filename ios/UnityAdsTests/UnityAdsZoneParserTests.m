//
//  UnityAdsZoneParserTests.m
//  UnityAds
//
//  Created by Ville Orkas on 10/9/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

#import "UnityAdsZoneParser.h"
#import "UnityAdsIncentivizedZone.h"

@interface UnityAdsZoneParserTests : SenTestCase

@end

@implementation UnityAdsZoneParserTests

- (void)testZoneParserSingleZone {
  UnityAdsZone * zone = [UnityAdsZoneParser parseZone:@{@"id": @"testZone1", @"name": @"testZoneName1"}];
  STAssertTrue(zone != nil, @"Failed to parse valid zone");
}

- (void)testZoneParserIncentivizedZone {
  UnityAdsZone * zone = [UnityAdsZoneParser parseZone:@{@"id": @"testZone1", @"name": @"testZoneName1", @"incentivised": @"true"}];
  STAssertTrue([zone isKindOfClass:[UnityAdsIncentivizedZone class]], @"Failed to return an instance of an incentivised zone");
}

- (void)testZoneParserMultipleZones {
  NSDictionary * zones = [UnityAdsZoneParser parseZones:@[@{@"id": @"testZone1", @"name": @"testZoneName1"}, @{@"id": @"testZone2", @"name": @"testZoneName2"}]];
  STAssertTrue([zones count] == 2, @"Failed to parse multiple zones");
}

- (void)testZoneParserMultipleZonesWithAnInvalidZone {
  NSDictionary * zones = [UnityAdsZoneParser parseZones:@[@{@"id": @"testZone1", @"name": @"testZoneName1"}, @{@"name": @"testZoneName2"}]];
  STAssertTrue([zones count] == 1, @"Parsed invalid zone");
}

@end
