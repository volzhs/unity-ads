//
//  UnityAdsItemManagerTests.m
//  UnityAds
//
//  Created by Ville Orkas on 10/2/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>

#import <objc/objc-runtime.h>
extern void __gcov_flush();

#import "UnityAdsRewardItemManager.h"

@interface UnityAdsRewardItemManagerTests : SenTestCase {
  UnityAdsRewardItem * validItem1, * validItem2;
}

@end

@implementation UnityAdsRewardItemManagerTests

- (void)setUp {
  [super setUp];
  validItem1 = [[UnityAdsRewardItem alloc] initWithData:@{@"key": @"testItemKey1", @"name": @"testItemName1", @"picture": @"http://invalid.url.com"}];
  validItem2 = [[UnityAdsRewardItem alloc] initWithData:@{@"key": @"testItemKey2", @"name": @"testItemName2", @"picture": @"http://invalid.url.com"}];
}

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
}

- (void)testEmptyItems {
  UnityAdsRewardItemManager * itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:@{} defaultItem:validItem1];
  STAssertTrue(itemManager == nil, @"Invalid item manager was created");
}

- (void)testEmptyDefaultItem {
  UnityAdsRewardItemManager * itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:@{@"testItemKey1": validItem1} defaultItem:nil];
  STAssertTrue(itemManager == nil, @"Invalid item manager was created");
}

- (void)testMissingDefaultItem {
  UnityAdsRewardItemManager * itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:@{@"testItemKey1": validItem1} defaultItem:validItem2];
  STAssertTrue(itemManager == nil, @"Invalid item manager was created");
}

- (void)testValidItems {
  UnityAdsRewardItemManager * itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:@{@"testItemKey1": validItem1, @"testItemKey2": validItem2} defaultItem:validItem2];
  STAssertTrue(itemManager != nil, @"Valid item manager was not created");
  STAssertTrue([itemManager itemCount] == 2, @"Invalid item count");
}

@end
