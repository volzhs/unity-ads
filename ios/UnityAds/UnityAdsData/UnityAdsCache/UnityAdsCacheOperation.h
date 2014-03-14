//
//  UnityAdsCacheOperation.h
//  UnityAds
//
//  Created by Sergey D on 3/13/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum {
  ResourceTypeTrailerVideo = 0,
} ResourceType;


@class UnityAdsCacheOperation;

@protocol UnityAdsCacheOperationDelegate <NSObject>

@required
- (void)operationStarted:(UnityAdsCacheOperation *)cacheOperation;
- (void)operationFinished:(UnityAdsCacheOperation *)cacheOperation;
- (void)operationFailed:(UnityAdsCacheOperation *)cacheOperation;
- (void)operationCancelled:(UnityAdsCacheOperation *)cacheOperation;

@end

@interface UnityAdsCacheOperation : NSOperation

@property (nonatomic, assign) id <UnityAdsCacheOperationDelegate> delegate;
@property (nonatomic, assign) NSUInteger expectedFileSize;
@property (nonatomic, copy)   NSString * operationKey;
@property (nonatomic, assign) ResourceType resourceType;

@end
