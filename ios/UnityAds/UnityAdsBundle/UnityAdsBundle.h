//
//  UnityAdsBundle.h
//  UnityAds
//
//  Created by Matti Savolainen on 5/3/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface UnityAdsBundle : NSObject

+ (NSBundle *)bundle;
+ (UIImage *)imageWithName:(NSString *)aName;
+ (UIImage *)imageWithName:(NSString *)aName ofType:(NSString *)aType;
@end
