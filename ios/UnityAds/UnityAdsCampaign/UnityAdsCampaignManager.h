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
@class UnityAdsCampaign;

@protocol UnityAdsCampaignManagerDelegate <NSObject>

@required
- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem gamerID:(NSString *)gamerID;
- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager campaignData:(NSDictionary *)data;

@end

@interface UnityAdsCampaignManager : NSObject

@property (nonatomic, assign) id<UnityAdsCampaignManagerDelegate> delegate;
@property (nonatomic, strong) NSString *queryString;
//@property (nonatomic, strong) id campaignData;

- (void)updateCampaigns;
- (NSURL *)videoURLForCampaign:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;

@end
