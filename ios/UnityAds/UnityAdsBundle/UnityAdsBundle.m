//
//  UnityAdsBundle.m
//  UnityAds
//
//  Created by Matti Savolainen on 5/3/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsBundle.h"

@implementation UnityAdsBundle

+ (NSBundle *)bundle {
  static NSBundle *resourceBundle;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    resourceBundle = [[NSBundle alloc] initWithPath:[[NSBundle mainBundle] pathForResource:@"UnityAds" ofType:@"bundle"]];
    NSAssert(resourceBundle, @"Please move the UnityAds.bundle into the Resource Directory of your Application!");
  });
  return resourceBundle;
}

+ (UIImage *)imageWithName:(NSString *)aName {
  NSBundle *bundle = [self bundle];
  NSString *path = [bundle pathForResource:aName ofType:@"png"];
  return [UIImage imageWithContentsOfFile:path];
}

+ (UIImage *)imageWithName:(NSString *)aName ofType:(NSString *)aType {
  NSBundle *bundle = [self bundle];
  NSString *path = [bundle pathForResource:aName ofType:aType];
  return [UIImage imageWithContentsOfFile:path];
}

@end
