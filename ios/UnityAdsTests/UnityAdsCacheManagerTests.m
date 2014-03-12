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
#import "UnityAdsSBJsonParser.h"
#import "NSObject+UnityAdsSBJson.h"
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
  CachingResult cachingResult;
  UnityAdsCacheManager * _cacheManager;
}

@end

extern void __gcov_flush();

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

- (void)setUp
{
  [super setUp];
  _cacheManager = [UnityAdsCacheManager new];
  _cacheManager.delegate = self;
  // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
  __gcov_flush();
  [super tearDown];
  _cacheManager = nil;
}

- (void)testCacheNilCampaign {
  cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = nil;
  [_cacheManager cacheCampaign:campaignToCache];
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching nil campaign");
}

- (void)testCacheEmptyCampaign {
  cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  [_cacheManager cacheCampaign:campaignToCache];
  
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching empty campaign");
}

- (void)testCachePartiallyFilledCampaign {
  cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  campaignToCache.id = @"tmp";
  [_cacheManager cacheCampaign:campaignToCache];
  
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching partially empty campaign");
  
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = NO;
  [_cacheManager cacheCampaign:campaignToCache];
  
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching partially empty campaign");
  
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = YES;
  [_cacheManager cacheCampaign:campaignToCache];
  
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail instantly in same thread when caching partially empty campaign");
}

- (void)testCacheCampaignFilledWithWrongValues {
  cachingResult = CachingResultUndefined;
  UnityAdsCampaign * campaignToCache = [UnityAdsCampaign new];
  campaignToCache.id = @"tmp";
  campaignToCache.isValidCampaign = YES;
  campaignToCache.trailerDownloadableURL = [NSURL URLWithString:@"tmp"];
  [_cacheManager cacheCampaign:campaignToCache];
  
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return cachingResult == CachingResultUndefined;
    }
  }];
  
  STAssertTrue(cachingResult == CachingResultFailed,
               @"caching should fail campaign filled with wrong values");
}

- (void)testCacheSingleValidCampaign {
  cachingResult = CachingResultUndefined;
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  NSDictionary * jsonDataDictionary = [jsonString JSONValue];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  UnityAdsCampaign * campaignToCache = campaigns[0];
  STAssertTrue(campaignToCache != nil, @"campaign is nil");
  [_cacheManager cacheCampaign:campaignToCache];
  
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
}

- (void)testCacheAllCampaigns {
  cachingResult = CachingResultUndefined;
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  NSDictionary * jsonDataDictionary = [jsonString JSONValue];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  [_cacheManager cacheCampaigns:campaigns];
  sleep(1);
  [_cacheManager cancelAllDownloads];
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
}

- (void)testCancelAllOperatons {
  cachingResult = CachingResultUndefined;
  NSError * error = nil;
  NSStringEncoding encoding = NSStringEncodingConversionAllowLossy;
  NSString * pathToResource = [[NSBundle bundleForClass:[self class]] pathForResource:@"jsonData.txt" ofType:nil];
  NSString * jsonString = [[NSString alloc] initWithContentsOfFile:pathToResource
                                                      usedEncoding:&encoding
                                                             error:&error];
  NSDictionary * jsonDataDictionary = [jsonString JSONValue];
  NSDictionary *jsonDictionary = [jsonDataDictionary objectForKey:kUnityAdsJsonDataRootKey];
  NSArray  * campaignsDataArray = [jsonDictionary objectForKey:kUnityAdsCampaignsKey];
  NSArray * campaigns = [[UnityAdsCampaignManager sharedInstance] performSelector:@selector(deserializeCampaigns:) withObject:campaignsDataArray];
  STAssertTrue(jsonString != nil, @"empty json string");
  [_cacheManager cacheCampaigns:campaigns];
  sleep(4);
  [_cacheManager cancelAllDownloads];
  [self threadBlocked:^BOOL{
    @synchronized(self) {
      return cachingResult != CachingResultFinishedAll;
    }
  }];
  
  STAssertTrue(cachingResult == CachingResultFinishedAll,
               @"caching should be ok when caching valid campaigns");
}

#pragma mark - UnityAdsCacheManagerDelegate

- (void)cache:(UnityAdsCacheManager *)cache failedToCacheCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultFailed;
  }
}

- (void)cache:(UnityAdsCacheManager *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultFinished;
    NSLog(@"finished %@",campaign.trailerDownloadableURL);
  }
}

- (void)cache:(UnityAdsCacheManager *)cache cancelledCachingCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    cachingResult = CachingResultCancelled;
  }
}

- (void)cache:(UnityAdsCacheManager *)cache finishedCachingAllCampaigns:(NSArray *)campaigns {
  @synchronized(self) {
    cachingResult = CachingResultFinishedAll;
  }
}

@end
