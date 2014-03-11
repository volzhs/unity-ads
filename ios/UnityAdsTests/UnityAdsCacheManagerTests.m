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

typedef enum {
  CachingResultUndefined = 0,
  CachingResultFinished,
  CachingResultFailed,
  CachingResultCancelled,
} CachingResult;

@interface UnityAdsCacheManagerTests : SenTestCase <UnityAdsCacheManagerDelegate> {
  @private
  CachingResult cachingResult;
}

@end

extern void __gcov_flush();

@implementation UnityAdsCacheManagerTests

- (void)threadBlocked:(BOOL (^)())isThreadBlocked {
	@autoreleasepool {
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while(isThreadBlocked) {
			@autoreleasepool {
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.5]];
			}
		}
	}
}

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
  cachingResult = CachingResultUndefined;
  UnityAdsCacheManager * cacheManager = [UnityAdsCacheManager new];
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  [cacheManager cacheCampaign:campaignToCache];
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching empty campaign");
}

- (void)cache:(UnityAdsCacheManager *)cache failedToCacheCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultFailed;
  }
}

- (void)cache:(UnityAdsCacheManager *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultFinished;
  }
}

- (void)cache:(UnityAdsCacheManager *)cache cancelledCaching:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultCancelled;
  }
}

@end
