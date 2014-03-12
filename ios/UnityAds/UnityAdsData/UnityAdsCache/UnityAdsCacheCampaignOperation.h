//
//  UnityAdsCacheOperation.h
//  testApp
//
//  Created by Sergey D on 3/10/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCampaign.h"

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
@property (nonatomic, assign) id <UnityAdsCacheOperationDelegate> delegate;

@end
