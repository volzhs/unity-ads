//
//  UnityAdsItemManager.h
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "UnityAdsItem.h"

@interface UnityAdsItemManager : NSObject

- (id)initWithItems:(NSDictionary *)items defaultItem:(UnityAdsItem *)defaultItem;

- (UnityAdsItem *)getItem:(NSString *)key;
- (UnityAdsItem *)getDefaultItem;

@end
