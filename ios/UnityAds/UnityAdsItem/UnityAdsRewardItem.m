//
//  UnityAdsItem.m
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsRewardItem.h"

#import "UnityAds.h"
#import "UnityAdsConstants.h"

@implementation UnityAdsRewardItem

- (id)initWithData:(NSDictionary *)data
{
    self = [super init];
    if (self)
    {
        id keyValue = data[kUnityAdsRewardItemKeyKey];
        _key = [keyValue isKindOfClass:[NSNumber class]] ? [keyValue stringValue] : keyValue;
        
        id nameValue = data[kUnityAdsRewardNameKey];
        _name = [nameValue isKindOfClass:[NSNumber class]] ? [nameValue stringValue] : nameValue;
        
        NSString *pictureURLString = data[kUnityAdsRewardPictureKey];
        _pictureURL = [NSURL URLWithString:pictureURLString];
        
        if(_key == nil || _name == nil || _pictureURL == nil)
        {
            return nil;
        }
    }
    return self;
}

- (NSDictionary *)getDetails {
    return @{kUnityAdsRewardItemNameKey:self.name, kUnityAdsRewardItemPictureKey:self.pictureURL};
}

@end
