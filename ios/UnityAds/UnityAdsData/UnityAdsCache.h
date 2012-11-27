//
//  UnityAdsCache.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCache;
@class UnityAdsCampaign;

@protocol UnityAdsCacheDelegate <NSObject>

@required
- (void)cache:(UnityAdsCache *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign;
- (void)cacheFinishedCachingCampaigns:(UnityAdsCache *)cache;

@end

@interface UnityAdsCache : NSObject

@property (nonatomic, assign) id<UnityAdsCacheDelegate> delegate;

- (void)cacheCampaigns:(NSArray *)campaigns;
- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;
- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign;

@end
