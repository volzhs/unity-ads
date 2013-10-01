//
//  UnityAdsIncentivizedZone.m
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsIncentivizedZone.h"
#import "UnityAdsConstants.h"

@interface UnityAdsIncentivizedZone ()

@property (nonatomic, strong) UnityAdsItemManager *_itemManager;

@end

@implementation UnityAdsIncentivizedZone

- (id)initWithData:(NSDictionary *)options {
  self = [super init];
  if(self) {
    id items = [options objectForKey:kUnityAdsRewardItemsKey];
    id defaultItem = [options objectForKey:kUnityAdsRewardItemKey];
    self._itemManager = [[UnityAdsItemManager alloc] initWithItems:items defaultItem:defaultItem];
  }
  return self;
}

- (UnityAdsItemManager *)itemManager {
  return self._itemManager;
}

@end
