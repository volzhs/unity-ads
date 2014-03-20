//
//  UnityAdsItemManager.m
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsRewardItemManager.h"

@interface UnityAdsRewardItemManager ()

@property (nonatomic, strong) NSDictionary * _items;
@property (nonatomic, strong) UnityAdsRewardItem * _defaultItem;
@property (nonatomic, strong) UnityAdsRewardItem * _currentItem;

@end

@implementation UnityAdsRewardItemManager

- (id)initWithItems:(NSDictionary *)items defaultItem:(UnityAdsRewardItem *)defaultItem {
  self = [super init];
  if(self) {
    self._items = [NSDictionary dictionaryWithDictionary:items];
    
    if(defaultItem == nil) return nil;
    
    __block BOOL itemsIncludesDefault = NO;
    [self._items enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
      if([key isEqualToString:defaultItem.key]) {
        itemsIncludesDefault = YES;
        *stop = YES;
      }
    }];
    if(!itemsIncludesDefault) return nil;
    
    self._defaultItem = defaultItem;
    self._currentItem = defaultItem;
  }
  return self;
}

- (UnityAdsRewardItem *)getItem:(NSString *)key {
  return [self._items objectForKey:key];
}

- (UnityAdsRewardItem *)getDefaultItem {
  return self._defaultItem;
}

- (UnityAdsRewardItem *)getCurrentItem {
  return self._currentItem;
}

- (BOOL)setCurrentItem:(NSString *)rewardItemKey {
  id newItem = [self._items objectForKey:rewardItemKey];
  if(newItem != nil) {
    self._currentItem = newItem;
    return TRUE;
  }
  return FALSE;
}

- (NSArray *)allItems {
  return [self._items allValues];
}

- (NSUInteger)itemCount {
  return self._items.count;
}

@end
