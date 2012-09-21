//
//  UnityAdsAnalyticsUploader.h
//  UnityAdsExample
//
//  Created by Johan Halin on 13.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCampaign;

@interface UnityAdsAnalyticsUploader : NSObject

- (void)sendViewReportWithQueryString:(NSString *)queryString;
- (void)sendTrackingCallWithQueryString:(NSString *)queryString;
- (void)retryFailedUploads;

@end
