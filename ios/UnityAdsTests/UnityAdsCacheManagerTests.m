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
#import "UnityAdsCampaignManager.h"
#import "UnityAdsConstants.h"

typedef enum {
  CachingResultUndefined = 0,
  CachingResultFinished,
  CachingResultFinishedAll,
  CachingResultFailed,
  CachingResultCancelled,
} CachingResult;

@interface UnityAdsCacheManagerTests : SenTestCase <UnityAdsCacheManagerDelegate> {
@private
  CachingResult _cachingResult;
  UnityAdsCacheManager * _cacheManager;
}

- (NSString *)cachePath;

extern void __gcov_flush();

@end

@implementation UnityAdsCacheManagerTests

- (void)threadBlocked:(BOOL (^)())isThreadBlocked {
	@autoreleasepool {
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while(isThreadBlocked()) {
			@autoreleasepool {
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode
                                 beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.5]];
			}
		}
	}
}

- (NSString *)cachePath {
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
	return [[paths objectAtIndex:0] stringByAppendingPathComponent:@"unityads"];
}

- (void)setUp
{
  [super setUp];
  _cacheManager = [UnityAdsCacheManager sharedInstance];
  _cacheManager.delegate = self;
  [[NSFileManager defaultManager] removeItemAtPath:[self cachePath] error:nil];
  // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
  __gcov_flush();
  _cacheManager = nil;
  [super tearDown];
}

- (void)testCacheNilCampaign {
  _cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = nil;
  STAssertTrue([_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache] != YES,
               @"caching should fail instantly in same thread when caching nil campaign");
}

- (void)testCacheEmptyCampaign {
  _cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  STAssertTrue([_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache] != YES,
               @"caching should fail instantly in same thread when caching empty campaign");
}

- (void)testCachePartiallyFilledCampaign {
  _cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  campaignToCache.id = @"tmp";
  STAssertTrue([_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache] != YES,
               @"caching should fail instantly in same thread when caching partially filled campaign");
  
  _cachingResult = CachingResultUndefined;
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = NO;
  STAssertTrue([_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache] != YES,
               @"caching should fail instantly in same thread when caching partially filled campaign");
  
  _cachingResult = CachingResultUndefined;
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = YES;
  STAssertTrue([_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache] != YES,
               @"caching should fail instantly in same thread when caching partially filled campaign");
}

- (void)testCacheCampaignFilledWithWrongValues {
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = YES;
  campaignToCache.trailerDownloadableURL = [NSURL URLWithString:@"tmp"];
  BOOL addedToQueue = [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache];
  
  if (addedToQueue) {
    _cachingResult = CachingResultUndefined;
    [self threadBlocked:^BOOL{
      @synchronized(self) {
        return _cachingResult != CachingResultFinishedAll;
      }
    }];
  }
  
  STAssertTrue(addedToQueue != true, @"operation should not added to queue");
  
  if (addedToQueue) {
    STAssertTrue(_cachingResult == CachingResultFinishedAll,
                 @"caching should fail campaign filled with wrong values");
    STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaignToCache] != YES,
                 @"video should not be cached");
  }
}

- (void)testCacheSingleValidCampaign {
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  
  NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *jsonDataDictionary = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  UnityAdsCampaign * campaignToCache = campaigns[0];
  STAssertTrue(campaignToCache != nil, @"campaign is nil");
  BOOL addedToQueue = [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaignToCache];
  
  if (addedToQueue) {
    _cachingResult = CachingResultUndefined;
    [self threadBlocked:^BOOL{
      @synchronized(self) {
        return _cachingResult != CachingResultFinishedAll;
      }
    }];
  }
  
  STAssertTrue(addedToQueue == true, @"operation shoulb be added to queue");
  
  if (addedToQueue) {
    STAssertTrue(_cachingResult == CachingResultFinishedAll,
                 @"caching should be ok when caching valid campaigns");
    STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaignToCache],
                 @"video should be cached");
  }
  
  STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaignToCache] == true,
               @"cache invalid for campaign %@",
               campaignToCache.id);
}

- (void)testCacheAllCampaigns {
  NSError *error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString *pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString *jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  
  STAssertTrue(jsonString != nil, @"empty json string");
  
  NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *jsonDataDictionary = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray *campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray *campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];

  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaign];
    if (idx > 2) {
      *stop = YES;
    }
  }];
  _cachingResult = CachingResultUndefined;
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return _cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(_cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
  
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaign] == true, @"cache invalid for campaign %@", campaign.id);
    if (idx > 2) {
      *stop = YES;
    }
  }];
}

- (void)testCancelAllOperatons {
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *jsonDataDictionary = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaign];
    if (idx > 2) {
      *stop = YES;
    }
  }];

  [_cacheManager cancelAllDownloads];
  
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaign];
    if (idx > 2) {
      *stop = YES;
    }
  }];
  
  _cachingResult = CachingResultUndefined;
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return _cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(_cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
  
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaign] == true, @"cache invalid for campaign %@", campaign.id);
    if (idx > 2) {
      *stop = YES;
    }
  }];
}

- (void)testCacheAllOperationsTwice {
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *jsonDataDictionary = [NSJSONSerialization JSONObjectWithData:jsonData options:0 error:nil];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaign];
    if (idx > 2) {
      *stop = YES;
    }
  }];
  _cachingResult = CachingResultUndefined;
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return _cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(_cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
  
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    [_cacheManager cache:ResourceTypeTrailerVideo forCampaign:campaign];
    if (idx > 2) {
      *stop = YES;
    }
  }];
  _cachingResult = CachingResultUndefined;
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return _cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(_cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
  [campaigns  enumerateObjectsUsingBlock:^(UnityAdsCampaign *campaign, NSUInteger idx, BOOL *stop) {
    STAssertTrue([_cacheManager is:ResourceTypeTrailerVideo cachedForCampaign:campaign] == true, @"cache invalid for campaign %@", campaign.id);
    if (idx > 2) {
      *stop = YES;
    }
  }];
}

#pragma mark - UnityAdsCacheManagerDelegate

- (void)finishedCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    _cachingResult = CachingResultFinished;
  }
}

- (void)failedCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    _cachingResult = CachingResultFailed;
  }
}

- (void)cancelledCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    _cachingResult = CachingResultCancelled;
  }
}

- (void)cachingQueueEmpty {
  @synchronized(self) {
    _cachingResult = CachingResultFinishedAll;
  }
}

@end
