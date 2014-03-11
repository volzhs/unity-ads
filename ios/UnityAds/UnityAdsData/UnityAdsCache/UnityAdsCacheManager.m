//
//  UnityAdsCache.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsCacheManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsInstrumentation.h"
#import "UnityAdsConstants.h"
#import "UnityAdsCacheOperation.h"

NSString * const kUnityAdsCacheCampaignKey = @"kUnityAdsCacheCampaignKey";
NSString * const kUnityAdsCacheConnectionKey = @"kUnityAdsCacheConnectionKey";
NSString * const kUnityAdsCacheFilePathKey = @"kUnityAdsCacheFilePathKey";
NSString * const kUnityAdsCacheURLRequestKey = @"kUnityAdsCacheURLRequestKey";
NSString * const kUnityAdsCacheIndexKey = @"kUnityAdsCacheIndexKey";
NSString * const kUnityAdsCacheResumeKey = @"kUnityAdsCacheResumeKey";

NSString * const kUnityAdsCacheDownloadResumeExpected = @"kUnityAdsCacheDownloadResumeExpected";
NSString * const kUnityAdsCacheDownloadNewDownload = @"kUnityAdsCacheDownloadNewDownload";

NSString * const kUnityAdsCacheEntryCampaignIDKey = @"kUnityAdsCacheEntryCampaignIDKey";
NSString * const kUnityAdsCacheEntryFilenameKey = @"kUnityAdsCacheEntryFilenameKey";
NSString * const kUnityAdsCacheEntryFilesizeKey = @"kUnityAdsCacheEntryFilesizeKey";

@interface UnityAdsCacheManager () <UnityAdsCacheOperationDelegate>
@property (nonatomic, strong) NSOperationQueue * cacheOperationsQueue;
@property (nonatomic, strong) NSMutableDictionary *campaignsOperations;
@end

@implementation UnityAdsCacheManager

#pragma mark - Private

- (NSString *)_cachePath {
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
	UAAssertV(paths != nil && [paths count] > 0, nil);
	
	return [[paths objectAtIndex:0] stringByAppendingPathComponent:@"unityads"];
}

- (NSString *)_videoFilenameForCampaign:(UnityAdsCampaign *)campaign {
  if ([campaign.trailerDownloadableURL lastPathComponent] == nil || [campaign.trailerDownloadableURL lastPathComponent].length < 3) {
    return [NSString stringWithFormat:@"%@-%@", campaign.id, @"failed.mp4"];
  }
  
	return [NSString stringWithFormat:@"%@-%@", campaign.id, [campaign.trailerDownloadableURL lastPathComponent]];
}

- (NSString *)_videoPathForCampaign:(UnityAdsCampaign *)campaign {
	return [[self _cachePath] stringByAppendingPathComponent:[self _videoFilenameForCampaign:campaign]];
}

- (long long)_cachedFilesizeForVideoFilename:(NSString *)filename {
	NSArray *index = [[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsCacheIndexKey];
	long long size = 0;
	
	for (NSDictionary *cacheEntry in index) {
		NSString *indexFilename = [cacheEntry objectForKey:kUnityAdsCacheEntryFilenameKey];
		if ([filename isEqualToString:indexFilename]) {
			size = [[cacheEntry objectForKey:kUnityAdsCacheEntryFilesizeKey] longLongValue];
			break;
		}
	}
	
	return size;
}

- (long long)_filesizeForPath:(NSString *)path {
	long long size = 0;
	
	if ([[NSFileManager defaultManager] fileExistsAtPath:path]) {
		NSDictionary *attributes = [[NSFileManager defaultManager] attributesOfItemAtPath:path error:nil];
		size = [attributes fileSize];
	}
	
	return size;
}

#pragma mark - Public

- (id)init {
	UAAssertV(![NSThread isMainThread], nil);
	
	if ((self = [super init])) {
    UALOG_DEBUG(@"creating downloadqueue");
    self.cacheOperationsQueue = [NSOperationQueue new];
    [self.cacheOperationsQueue setMaxConcurrentOperationCount:1];
    self.campaignsOperations = [NSMutableDictionary new];
	}
	
	return self;
}

- (BOOL)_isValidCampaignToCache:(UnityAdsCampaign *)campaignToCache {
  @synchronized(self) {
    return ![self campaignExistsInQueue:campaignToCache];
  }
}

- (void)cacheCampaign:(UnityAdsCampaign *)campaignToCache {
  @synchronized(self) {
    if (![self _isValidCampaignToCache:campaignToCache]) return;
    UnityAdsCacheOperation * cacheOperation = [UnityAdsCacheOperation new];
    cacheOperation.campaignToCache = campaignToCache;
    cacheOperation.delegate = self;
    self.campaignsOperations[campaignToCache.id] = cacheOperation;
    [self.cacheOperationsQueue addOperation:cacheOperation];
  }
}

- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    return self.campaignsOperations[campaign.id] != nil;
  }
}

- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign {
  BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:[self _videoPathForCampaign:campaign]];
  UALOG_DEBUG(@"File exists at path: %@, %i", [self _videoPathForCampaign:campaign], exists);
  return exists;
}

- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign {
	@synchronized (self) {
		if (campaign == nil) {
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
    
		NSString *path = [self _videoPathForCampaign:campaign];
		
		return [NSURL fileURLWithPath:path];
	}
}

- (void)cancelAllDownloads {
  @synchronized(self) {
    [self.cacheOperationsQueue cancelAllOperations];
  }
}

- (void)_removeCacheOperationForCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    [self.campaignsOperations removeObjectForKey:campaign.id];
  }
}

#pragma mark ----
#pragma mark UnityAdsCacheOperationDelegate
#pragma mark ----

- (void)operationStarted:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    [self _removeCacheOperationForCampaign:cacheOperation.campaignToCache];
  }
}

- (void)operationFinished:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    [self _removeCacheOperationForCampaign:cacheOperation.campaignToCache];
  }
}

- (void)operationFailed:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    [self _removeCacheOperationForCampaign:cacheOperation.campaignToCache];
  }
}

- (void)operationCancelled:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    [self _removeCacheOperationForCampaign:cacheOperation.campaignToCache];
  }
}

@end