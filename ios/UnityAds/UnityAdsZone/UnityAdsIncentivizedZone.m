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

@property (nonatomic, strong) UnityAdsRewardItemManager *itemManager;

@end

@implementation UnityAdsIncentivizedZone

- (id)initWithData:(NSDictionary *)options {
  self = [super initWithData:options];
  if(self) {
    id items = options[kUnityAdsZoneRewardItemsKey];
    NSMutableDictionary * itemsDictionary = [[NSMutableDictionary alloc] init];
    
    for(id rawItem in items) {
      id item = [[UnityAdsRewardItem alloc] initWithData:rawItem];
      if(item == nil) {
        continue;
      }
      [itemsDictionary setObject:item forKey:[item key]];
    }
    
    id defaultItem = [[UnityAdsRewardItem alloc] initWithData:options[kUnityAdsZoneDefaultRewardItemKey]];
    
    if([itemsDictionary count] == 0 || defaultItem == nil) {
      return nil;
    }
    
    _itemManager = [[UnityAdsRewardItemManager alloc] initWithItems:itemsDictionary defaultItem:defaultItem];
  }
  return self;
}

- (BOOL)isIncentivized {
  return TRUE;
}

@end
