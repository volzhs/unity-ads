//
//  UnityAdsIncentivizedZone.h
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "UnityAdsZone.h"
#import "UnityAdsItemManager.h"

@interface UnityAdsIncentivizedZone : UnityAdsZone

- (id)initWithData:(NSDictionary *)options;

- (UnityAdsItemManager *)itemManager;

@end
