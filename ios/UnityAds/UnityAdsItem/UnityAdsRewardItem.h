//
//  UnityAdsItem.h
//  UnityAds
//
//  Created by Ville Orkas on 10/1/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsRewardItem : NSObject

@property (nonatomic, strong) NSString *key;
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) NSURL *pictureURL;

- (id)initWithData:(NSDictionary *)data;

- (NSDictionary *)getDetails;

@end
