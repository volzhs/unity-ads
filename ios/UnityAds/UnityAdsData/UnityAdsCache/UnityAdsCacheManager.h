
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
- (void)cacheQueueEmpty;
@end

@interface UnityAdsCacheManager : NSObject

@property (nonatomic, weak) id <UnityAdsCacheManagerDelegate> delegate;

- (BOOL)cache:(ResourceType)resourceType forCampaign:(UnityAdsCampaign *)campaign;
- (NSURL *)localURLFor:(ResourceType)resourceType ofCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)is:(ResourceType)resourceType cachedForCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign withResourceType:(ResourceType)resourceType;
- (void)cancelAllDownloads;

@end
