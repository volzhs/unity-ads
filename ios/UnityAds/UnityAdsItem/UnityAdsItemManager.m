//
//  UnityAdsItemManager.m
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsItemManager.h"

@interface UnityAdsItemManager ()

@property (nonatomic, strong) NSDictionary * _items;
@property (nonatomic, strong) UnityAdsItem * _defaultItem;

@end

@implementation UnityAdsItemManager

- (id)initWithItems:(NSDictionary *)items defaultItem:(UnityAdsItem *)defaultItem {
  self = [super init];
  if(self) {
    self._items = [NSDictionary dictionaryWithDictionary:items];
    self._defaultItem = defaultItem;
  }
  return self;
}

- (UnityAdsItem *)getItem:(NSString *)key {
  return [self._items objectForKey:key];
}

- (UnityAdsItem *)getDefaultItem {
  return self._defaultItem;
}

@end
