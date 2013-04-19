//
//  UnityAdsRewardItem.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsRewardItem : NSObject

@property (nonatomic, strong) NSString *key;
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) NSURL *pictureURL;
@property (nonatomic, assign) BOOL isValidRewardItem;

- (id)initWithData:(NSDictionary *)data;

@end
