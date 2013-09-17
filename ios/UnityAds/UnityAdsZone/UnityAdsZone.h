//
//  UnityAdsZone.h
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsZone : NSObject

- (id)initWithData:(NSDictionary *)options;

- (NSString *)getZoneId;

@end
