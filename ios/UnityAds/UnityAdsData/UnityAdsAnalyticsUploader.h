//
//  UnityAdsAnalyticsUploader.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "../UnityAdsVideo/UnityAdsVideoPlayer.h"

@class UnityAdsCampaign;

@interface UnityAdsAnalyticsUploader : NSObject

- (void)sendOpenAppStoreRequest:(UnityAdsCampaign *)campaign;
- (void)sendTrackingCallWithQueryString:(NSString *)queryString;
- (void)sendInstallTrackingCallWithQueryDictionary:(NSDictionary *)queryDictionary;
- (void)retryFailedUploads;
- (void)logVideoAnalyticsWithPosition:(VideoAnalyticsPosition)videoPosition campaignId:(NSString *)campaignId viewed:(BOOL)viewed cached:(BOOL)cached;

+ (UnityAdsAnalyticsUploader *)sharedInstance;
@end