//
//  UnityAdsCache.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsCacheManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsInstrumentation.h"
#import "UnityAdsConstants.h"

static NSString * const kUnityAdsCacheCampaignKey = @"kUnityAdsCacheCampaignKey";
static NSString * const kUnityAdsCacheConnectionKey = @"kUnityAdsCacheConnectionKey";
static NSString * const kUnityAdsCacheFilePathKey = @"kUnityAdsCacheFilePathKey";
static NSString * const kUnityAdsCacheURLRequestKey = @"kUnityAdsCacheURLRequestKey";
static NSString * const kUnityAdsCacheIndexKey = @"kUnityAdsCacheIndexKey";
static NSString * const kUnityAdsCacheResumeKey = @"kUnityAdsCacheResumeKey";

static NSString * const kUnityAdsCacheDownloadResumeExpected = @"kUnityAdsCacheDownloadResumeExpected";
static NSString * const kUnityAdsCacheDownloadNewDownload = @"kUnityAdsCacheDownloadNewDownload";

static NSString * const kUnityAdsCacheEntryCampaignIDKey = @"kUnityAdsCacheEntryCampaignIDKey";
static NSString * const kUnityAdsCacheEntryFilenameKey = @"kUnityAdsCacheEntryFilenameKey";
static NSString * const kUnityAdsCacheEntryFilesizeKey = @"kUnityAdsCacheEntryFilesizeKey";
static NSString * const kUnityAdsCacheOperationKey = @"kUnityAdsCacheOperationKey";
static NSString * const kUnityAdsCacheOperationCampaignKey = @"kUnityAdsCacheOperationCampaignKey";


@interface UnityAdsCacheManager () <UnityAdsCacheOperationDelegate>
@property (nonatomic, strong) NSOperationQueue * cacheOperationsQueue;
@property (nonatomic, strong) NSMutableDictionary *campaignsOperations;
@end

static UnityAdsCacheManager * _inst = nil;

@implementation UnityAdsCacheManager

+ sharedInstance {
  @synchronized (self) {
    return _inst == nil ? _inst = [[self class] new] : _inst;
  }
}

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

- (NSURL *)_downloadURLFor:(ResourceType)resourceType of:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    NSURL * url = nil;
    switch (resourceType) {
      case ResourceTypeTrailerVideo:
        url = campaign.trailerDownloadableURL;
        break;
      default:
        break;
    }
    return url;
  }
}

- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    BOOL cached = [self _filesizeForPath:[self _videoPathForCampaign:campaign]] == campaign.expectedTrailerSize &&
    campaign.expectedTrailerSize;
    return cached;
  }
}

#pragma mark - Public

- (id)init {
	if ((self = [super init])) {
    UALOG_DEBUG(@"creating downloadqueue");
    self.cacheOperationsQueue = [NSOperationQueue new];
    [self.cacheOperationsQueue setMaxConcurrentOperationCount:1];
    self.campaignsOperations = [NSMutableDictionary new];
    self.cachingSpeed = 0;
	}
	
	return self;
}

- (NSURL *)localURLFor:(ResourceType)resourceType ofCampaign:(UnityAdsCampaign *)campaign {
	@synchronized (self) {
		if (campaign == nil) {
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
		NSString *path = nil;
    switch (resourceType) {
      case ResourceTypeTrailerVideo:
        path = [self _videoPathForCampaign:campaign];
        break;
        
      default:
        break;
    }
		return [NSURL fileURLWithPath:path];
	}
}

- (BOOL)_isCampaignValid:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    return campaign != nil && campaign.expectedTrailerSize && campaign.id && campaign.allowedToCacheVideo;
  }
}

- (BOOL)cache:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    if ([self campaignExistsInQueue:campaign withResourceType:resourceType] ||
        ![self _isCampaignValid:campaign]) return NO;
    
    UnityAdsCacheOperation * cacheOperation = nil;
    
    if (resourceType == ResourceTypeTrailerVideo) {
      UnityAdsCacheFileOperation  * tmp = [UnityAdsCacheFileOperation new];
      tmp.directoryPath = [self _cachePath];
      tmp.downloadURL = [self _downloadURLFor:resourceType of:campaign];
      tmp.filePath = [[self localURLFor:resourceType ofCampaign:campaign] relativePath];
      tmp.expectedFileSize = campaign.expectedTrailerSize;
      tmp.cachingSpeed = 0;
      cacheOperation = tmp;
    }
    
    if (!cacheOperation) return NO;
    
    NSString * key = [self operationKey:campaign resourceType:resourceType];
    cacheOperation.delegate = self;
    cacheOperation.operationKey = key;
    cacheOperation.resourceType = resourceType;
    self.campaignsOperations[key] = @{ kUnityAdsCacheOperationKey : cacheOperation,
                                       kUnityAdsCacheOperationCampaignKey : campaign };
    [self.cacheOperationsQueue addOperation:cacheOperation];
    return YES;
  }
}

- (void)cancelCacheForCampaign:(UnityAdsCampaign *)campaign withResourceType:(ResourceType)resourceType {
  @synchronized(self) {
    UnityAdsCacheOperation * cacheOperation =
    self.campaignsOperations[[self operationKey:campaign resourceType:resourceType]][kUnityAdsCacheOperationKey];
    [cacheOperation cancel];
  }
}

- (BOOL)is:(ResourceType)resourceType cachedForCampaign:(UnityAdsCampaign *)campaign {
  @synchronized(self) {
    BOOL result = NO;
    switch (resourceType) {
      case ResourceTypeTrailerVideo:
        result = [self isCampaignVideoCached:campaign];
        break;
      default:
        break;
    }
    return result;
  }
}

- (NSString *)operationKey:(UnityAdsCampaign *)campaign resourceType:(ResourceType)resourceType {
  @synchronized(self) {
    return [NSString stringWithFormat:@"%@-%d", campaign.id, resourceType];
  }
}


- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign
             withResourceType:(ResourceType)resourceType {
  @synchronized(self) {
    return self.campaignsOperations[[self operationKey:campaign resourceType:resourceType]] != nil;
  }
}

- (void)cancelAllDownloads {
  @synchronized(self) {
    [self.cacheOperationsQueue cancelAllOperations];
  }
}

- (void)_removeOperation:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    if (!cacheOperation.operationKey) return;
    [self.campaignsOperations removeObjectForKey:cacheOperation.operationKey];
    
    if (self.campaignsOperations.count == 0 &&
        [self.delegate respondsToSelector:@selector(cachingQueueEmpty)])
      [self.delegate cachingQueueEmpty];
  }
}

#pragma mark ----
#pragma mark UnityAdsFileCacheOperationDelegate
#pragma mark ----

- (void)cacheOperationStarted:(UnityAdsCacheOperation *)cacheOperation  {
  @synchronized(self) {
    NSDictionary * operationInfo = self.campaignsOperations[cacheOperation.operationKey];
    UnityAdsCampaign * campaign = operationInfo[kUnityAdsCacheOperationCampaignKey];
    UALOG_DEBUG(@"for campaign %@", campaign.id);
    if ([self.delegate respondsToSelector:@selector(startedCaching:forCampaign:)])
      [self.delegate startedCaching:cacheOperation.resourceType forCampaign:campaign];
  }
}

- (void)cacheOperationFinished:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    NSDictionary * operationInfo = self.campaignsOperations[cacheOperation.operationKey];
    UnityAdsCampaign * campaign = operationInfo[kUnityAdsCacheOperationCampaignKey];
    UALOG_DEBUG(@"for campaign %@", campaign.id);
    if(cacheOperation.cachingSpeed > 0) {
      [self setCachingSpeed:cacheOperation.cachingSpeed];
    }    
    if ([self.delegate respondsToSelector:@selector(finishedCaching:forCampaign:)])
      [self.delegate finishedCaching:cacheOperation.resourceType forCampaign:campaign];
    [self _removeOperation:cacheOperation];
  }
}

- (void)cacheOperationFailed:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    NSDictionary * operationInfo = self.campaignsOperations[cacheOperation.operationKey];
    UnityAdsCampaign * campaign = operationInfo[kUnityAdsCacheOperationCampaignKey];
    UALOG_DEBUG(@"for campaign %@", campaign.id);
    if ([self.delegate respondsToSelector:@selector(failedCaching:forCampaign:)])
      [self.delegate failedCaching:cacheOperation.resourceType forCampaign:campaign];
    [self _removeOperation:cacheOperation];
  }
}

- (void)cacheOperationCancelled:(UnityAdsCacheOperation *)cacheOperation {
  @synchronized(self) {
    NSDictionary * operationInfo = self.campaignsOperations[cacheOperation.operationKey];
    UnityAdsCampaign * campaign = operationInfo[kUnityAdsCacheOperationCampaignKey];
    UALOG_DEBUG(@"for campaign %@", campaign.id);
    if ([self.delegate respondsToSelector:@selector(cancelledCaching:forCampaign:)])
      [self.delegate cancelledCaching:cacheOperation.resourceType forCampaign:campaign];
    [self _removeOperation:cacheOperation];
  }
}

@end