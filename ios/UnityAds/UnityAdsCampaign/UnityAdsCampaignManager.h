//
//  UnityAdsCampaignManager.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@class UnityAdsCampaignManager;
@class UnityAdsRewardItem;
@class UnityAdsCampaign;

@protocol UnityAdsCampaignManagerDelegate <NSObject>

@required
- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem gamerID:(NSString *)gamerID;
- (void)campaignManagerCampaignDataReceived;
- (void)campaignManagerCampaignDataFailed;

@end

@interface UnityAdsCampaignManager : NSObject

@property (nonatomic, assign) id<UnityAdsCampaignManagerDelegate> delegate;
@property (nonatomic, strong) NSArray *campaigns;
@property (nonatomic, strong) NSDictionary *campaignData;
@property (nonatomic, strong) UnityAdsCampaign *selectedCampaign;
@property (nonatomic, strong) UnityAdsRewardItem *defaultRewardItem;
@property (nonatomic, strong) NSArray *rewardItems;
@property (nonatomic, strong) NSArray *rewardItemKeys;
@property (nonatomic, strong) NSString *currentRewardItemKey;

- (void)updateCampaigns;
- (NSURL *)getVideoURLForCampaign:(UnityAdsCampaign *)campaign;
- (void)cancelAllDownloads;
- (UnityAdsCampaign *)getCampaignWithId:(NSString *)campaignId;
- (NSArray *)getViewableCampaigns;
- (BOOL)setSelectedRewardItemKey:(NSString *)rewardItemKey;
- (UnityAdsRewardItem *)getCurrentRewardItem;

+ (id)sharedInstance;

@end
