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

- (void)sendViewReportForCampaign:(UnityAdsCampaign *)campaign positionString:(NSString *)positionString;
- (void)retryFailedUploads;

@end
