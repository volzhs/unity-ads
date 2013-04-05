//
//  UnityAdsViewStateEndScreen.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewState.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"

#import "../UnityAds.h"

@interface UnityAdsViewStateEndScreen : UnityAdsViewState
- (void)openAppStoreWithData:(NSDictionary *)data;
@end
