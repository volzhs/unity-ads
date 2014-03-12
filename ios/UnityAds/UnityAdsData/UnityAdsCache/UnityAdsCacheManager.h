
//
//  UnityAdsCache.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCacheManager;
@class UnityAdsCampaign;

@protocol UnityAdsCacheManagerDelegate <NSObject>
@optional
- (void)cache:(UnityAdsCacheManager *)cache failedToCacheCampaign:(UnityAdsCampaign *)campaign;
- (void)cache:(UnityAdsCacheManager *)cache cancelledCachingCampaign:(UnityAdsCampaign *)campaign;

@required
- (void)cache:(UnityAdsCacheManager *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign;

@end

@interface UnityAdsCacheManager : NSObject

@property (nonatomic, weak) id<UnityAdsCacheManagerDelegate> delegate;

- (void)cacheCampaigns:(NSArray *)campaigns;
- (void)cacheCampaign:(UnityAdsCampaign *)campaignToCache;
- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign;
- (BOOL)campaignExistsInQueue:(UnityAdsCampaign *)campaign;
- (BOOL)isCampaignVideoCached:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;

@end
