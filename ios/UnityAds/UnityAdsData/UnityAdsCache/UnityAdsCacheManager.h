
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

- (void)cacheCampaign:(UnityAdsCampaign *)campaignToCache;
- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign;
- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;

@end
