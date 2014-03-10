//
//  UnityAdsCache.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCacheManager;
@class UnityAdsCampaign;

@protocol UnityAdsCacheDelegate <NSObject>

@required
- (void)cache:(UnityAdsCacheManager *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign;
- (void)cacheFinishedCachingCampaigns:(UnityAdsCacheManager *)cache;

@end

@interface UnityAdsCacheManager : NSObject

@property (nonatomic, weak) id<UnityAdsCacheDelegate> delegate;

- (void)cacheCampaigns:(NSArray *)campaigns;
- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;
- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign;

@end
