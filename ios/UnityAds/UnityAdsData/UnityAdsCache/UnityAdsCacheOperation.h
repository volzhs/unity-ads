//
//  UnityAdsCacheOperation.h
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCampaign.h"

@class UnityAdsCacheOperation;

@protocol UnityAdsCacheOperationDelegate <NSObject>

@optional
- (void)operationStarted:(UnityAdsCacheOperation *)cacheOperation;

@required
- (void)operationFinished:(UnityAdsCacheOperation *)cacheOperation;
- (void)operationFailed:(UnityAdsCacheOperation *)cacheOperation;
- (void)operationCancelled:(UnityAdsCacheOperation *)cacheOperation;

@end

@interface UnityAdsCacheOperation : NSOperation

@property (nonatomic, strong) UnityAdsCampaign * campaignToCache;
@property (nonatomic, assign) id <UnityAdsCacheOperationDelegate> delegate;

@end
