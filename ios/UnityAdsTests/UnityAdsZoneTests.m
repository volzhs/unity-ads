//
//  UnityAdsZoneTests.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

extern void __gcov_flush();

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

- (void)testZoneInitialOptionsMerge {
  [self setUp];
  [validZone mergeOptions:@{@"noOfferScreen": @YES}];
  STAssertTrue([validZone noOfferScreen], @"Merge options failed");
  [validZone mergeOptions:@{@"openAnimated": @NO}];
  STAssertTrue(![validZone openAnimated], @"Merge options failed");
  STAssertTrue(![validZone noOfferScreen], @"Failed to reset zone to initial options");
}

- (void)testZoneSetSid {
  [self setUp];
  [validZone mergeOptions:@{@"sid": @"testSid"}];
  STAssertTrue([[validZone getGamerSid] isEqualToString:@"testSid"], @"Failed to set zone gamer sid from options");
}

- (void)testZoneRemoveSid {
  [self setUp];
  [validZone mergeOptions:@{@"sid": @"testSid"}];
  STAssertTrue([[validZone getGamerSid] isEqualToString:@"testSid"], @"Failed to set zone gamer sid from options");
  [validZone mergeOptions:nil];
  STAssertTrue([validZone getGamerSid] == nil, @"Failed to reset gamer sid to nil after zone options reset");
}

@end
