//
//  UnityAdsZoneParserTests.m
//  UnityAds
//
//  Created by Ville Orkas on 10/9/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

extern void __gcov_flush();

#import "UnityAdsZoneParser.h"
#import "UnityAdsIncentivizedZone.h"

@interface UnityAdsZoneParserTests : SenTestCase

@end

@implementation UnityAdsZoneParserTests

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
}

- (void)testZoneParserSingleZone {
  UnityAdsZone * zone = [UnityAdsZoneParser parseZone:@{@"id": @"testZone1", @"name": @"testZoneName1"}];
  STAssertTrue(zone != nil, @"Failed to parse valid zone");
}

- (void)testZoneParserIncentivizedZone {
  UnityAdsZone * zone = [UnityAdsZoneParser parseZone:@{
    @"id": @"testZone1",
    @"name": @"testZoneName1",
    @"incentivised": @"true",
    @"defaultRewardItem": @{@"key": @"testItemKey1", @"name": @"testItemName1", @"picture": @"http://invalid.url.com"},
    @"rewardItems": @[@{@"key": @"testItemKey1", @"name": @"testItemName1", @"picture": @"http://invalid.url.com"}]
  }];
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
