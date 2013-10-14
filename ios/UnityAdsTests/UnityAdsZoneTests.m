//
//  UnityAdsZoneTests.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

#import <objc/objc-runtime.h>
extern void __gcov_flush();

#import "UnityAdsSBJsonParser.h"

#import "UnityAdsZone.h"
#import "UnityAdsZoneManager.h"
#import "UnityAdsZoneParser.h"

@interface UnityAdsZoneTests : SenTestCase {
  UnityAdsZone * validZone;
}
@end

@implementation UnityAdsZoneTests

- (void)setUp {
  [super setUp];
  validZone = [[UnityAdsZone alloc] initWithData:@{
                                                         @"id": @"testZoneId",
                                                         @"name": @"testZoneName",
                                                         @"noOfferScreen": @NO,
                                                         @"openAnimated": @YES,
                                                         @"muteVideoSounds": @NO,
                                                         @"useDeviceOrientationForVideo": @YES,
                                                         @"allowClientOverrides": @[@"noOfferScreen", @"openAnimated"]}];
}

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
}

- (void)testZoneValidOverrides {
  [validZone mergeOptions:@{@"openAnimated": @NO}];
  STAssertTrue(![validZone openAnimated], @"Merge options failed");
}

- (void)testZoneInvalidOverrides {
  [validZone mergeOptions:@{@"muteVideoSounds": @YES}];
  STAssertTrue(![validZone muteVideoSounds], @"Merge options failed");
}

@end
