//
//  UnityAdsCacheOperation.h
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCampaign.h"

typedef enum {
  ResourceTypeTrailerVideo = 0,
} ResourceType;

@class UnityAdsCacheFileOperation;

@protocol UnityAdsFileCacheOperationDelegate <NSObject>

@optional
- (void)operationStarted:(UnityAdsCacheFileOperation *)cacheOperation;
- (void)operationFinished:(UnityAdsCacheFileOperation *)cacheOperation;
- (void)operationFailed:(UnityAdsCacheFileOperation *)cacheOperation;
- (void)operationCancelled:(UnityAdsCacheFileOperation *)cacheOperation;

@end

@interface UnityAdsCacheFileOperation : NSOperation

@property (nonatomic, strong) NSURL * downloadURL;
@property (nonatomic, copy)   NSString * filePath, * directoryPath;
@property (nonatomic, assign) id <UnityAdsFileCacheOperationDelegate> delegate;
@property (nonatomic, assign) NSUInteger expectedFileSize;
@property (nonatomic, copy)   NSString * operationKey;
@property (nonatomic, assign) ResourceType resourceType;

@end
