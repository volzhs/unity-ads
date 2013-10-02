//
//  UnityAdsIncentivizedZone.m
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsIncentivizedZone.h"
#import "UnityAdsConstants.h"
#import "UnityAdsRewardItem.h"

@interface UnityAdsIncentivizedZone ()

@property (nonatomic, strong) UnityAdsRewardItemManager *_itemManager;

@end

@implementation UnityAdsIncentivizedZone

- (id)initWithData:(NSDictionary *)options {
  self = [super initWithData:options];
  if(self) {
    id items = [options objectForKey:kUnityAdsZoneRewardItemsKey];
    NSMutableDictionary * itemsDictionary = [[NSMutableDictionary alloc] init];
    [items enumerateObjectsUsingBlock:^(id rawItem, NSUInteger idx, BOOL *stop) {
      id item = [[UnityAdsRewardItem alloc] initWithData:rawItem];
      [itemsDictionary setObject:item forKey:[item key]];
    }];
    id defaultItem = [[UnityAdsRewardItem alloc] initWithData:[options objectForKey:kUnityAdsZoneDefaultRewardItemKey]];
    self._itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:itemsDictionary defaultItem:defaultItem];
  }
  return self;
}

- (BOOL)isIncentivized {
  return TRUE;
}

- (UnityAdsRewardItemManager *)itemManager {
  return self._itemManager;
}

@end
