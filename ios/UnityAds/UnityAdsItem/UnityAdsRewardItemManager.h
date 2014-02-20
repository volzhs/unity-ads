//
//  UnityAdsItemManager.h
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "UnityAdsRewardItem.h"

@interface UnityAdsRewardItemManager : NSObject

- (id)initWithItems:(NSDictionary *)items defaultItem:(UnityAdsRewardItem *)defaultItem;

- (UnityAdsRewardItem *)getItem:(NSString *)key;
- (UnityAdsRewardItem *)getDefaultItem;
- (UnityAdsRewardItem *)getCurrentItem;
- (BOOL)setCurrentItem:(NSString *)rewardItemKey;

- (NSArray *)allItems;
- (int)itemCount;

@end
