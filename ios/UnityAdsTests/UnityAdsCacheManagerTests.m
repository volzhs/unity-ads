//
//  UnityAdsCacheManagerTests.m
//  UnityAds
//
//  Created by Sergey D on 3/11/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import "UnityAdsCampaign.h"
#import "UnityAdsCacheManager.h"

@interface UnityAdsCacheManagerTests : SenTestCase

@end

extern void __gcov_flush();

@implementation UnityAdsCacheManagerTests

- (void)setUp
{
  [super setUp];
  // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
}

- (void)testCacheEmptyCampaign {
  UnityAdsCacheManager * cacheManager = [UnityAdsCacheManager new];
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  [cacheManager cacheCampaign:campaignToCache];
}

@end
