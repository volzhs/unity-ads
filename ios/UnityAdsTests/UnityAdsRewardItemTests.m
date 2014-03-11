//
//  UnityAdsItemTests.m
//  UnityAds
//
//  Created by Ville Orkas on 10/2/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

extern void __gcov_flush();

#import "UnityAdsRewardItem.h"

@interface UnityAdsRewardItemTests : SenTestCase

@end

@implementation UnityAdsRewardItemTests

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
}

- (void)testValidItem {
  id itemData = @{@"key": @"testItemKey", @"name": @"testItemName", @"picture": @"http://invalid.url.com"};
  UnityAdsRewardItem * item = [[UnityAdsRewardItem alloc] initWithData:itemData];
  STAssertTrue([item.key isEqual:@"testItemKey"], @"Item key is not valid");
  STAssertTrue([item.name isEqual:@"testItemName"], @"Item name is not valid");
  STAssertTrue([[item.pictureURL absoluteString] isEqual:@"http://invalid.url.com"], @"Item picture is not valid");
}

- (void)testInvalidItem {
  id itemData = @{@"key": @"", @"picture": @"asd"};
  UnityAdsRewardItem * item = [[UnityAdsRewardItem alloc] initWithData:itemData];
  STAssertTrue(item == nil, @"Invalid item was created");
}

@end
