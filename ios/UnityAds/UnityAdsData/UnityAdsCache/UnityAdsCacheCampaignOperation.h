//
//  UnityAdsCacheOperation.h
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCampaign.h"

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

@class UnityAdsCacheCampaignOperation;

@protocol UnityAdsCacheOperationDelegate <NSObject>

@optional
- (void)operationStarted:(UnityAdsCacheCampaignOperation *)cacheOperation;

@required
- (void)operationFinished:(UnityAdsCacheCampaignOperation *)cacheOperation;
- (void)operationFailed:(UnityAdsCacheCampaignOperation *)cacheOperation;
- (void)operationCancelled:(UnityAdsCacheCampaignOperation *)cacheOperation;

@end

@interface UnityAdsCacheCampaignOperation : NSOperation

@property (nonatomic, assign) UnityAdsCampaign * campaignToCache;
@property (nonatomic, copy) NSString * filePathURL, * directoryPath;
@property (nonatomic, assign) id <UnityAdsCacheOperationDelegate> delegate;

@end
