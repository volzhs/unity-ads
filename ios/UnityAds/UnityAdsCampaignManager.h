//
//  UnityAdsCampaignManager.h
//  UnityAdsExample
//
//  Created by Johan Halin on 5.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCampaignManager;
@class UnityAdsRewardItem;

@protocol UnityAdsCampaignManagerDelegate <NSObject>

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem;

@end

@interface UnityAdsCampaignManager : NSObject

@property (nonatomic, assign) id<UnityAdsCampaignManagerDelegate> delegate;

- (void)updateCampaigns;

@end
