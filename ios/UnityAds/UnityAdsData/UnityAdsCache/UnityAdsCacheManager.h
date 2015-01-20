
//
//  UnityAdsCache.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "UnityAdsCacheFileOperation.h"


@class UnityAdsCacheManager;
@class UnityAdsCampaign;

@protocol UnityAdsCacheManagerDelegate <NSObject>
@optional
- (void)startedCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (void)finishedCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (void)failedCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (void)cancelledCaching:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (void)cachingQueueEmpty;
@end

@interface UnityAdsCacheManager : NSObject

@property (nonatomic, weak) id <UnityAdsCacheManagerDelegate> delegate;
@property (nonatomic, assign) unsigned long long cachingSpeed;

- (BOOL)cache:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign withResourceType:(ResourceType)resourceType;

- (NSURL *)localURLFor:(ResourceType)resourceType ofCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)is:(ResourceType)resourceType cachedForCampaign:(UnityAdsCampaign *)campaign;

- (void)cancelCacheForCampaign:(UnityAdsCampaign *)campaign withResourceType:(ResourceType)resourceType;
- (void)cancelAllDownloads;

+ sharedInstance;

@end
