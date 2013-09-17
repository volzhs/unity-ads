//
//  UnityAdsZone.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsZone.h"
#import "UnityAdsConstants.h"

@interface UnityAdsZone ()

@property (nonatomic, strong) NSDictionary *options;

@end

@implementation UnityAdsZone

- (id)initWithData:(NSDictionary *)options {
  self = [super init];
  if(self) {
    self.options = options;
  }
  return self;
}

- (NSString *)getZoneId {
  return [self.options valueForKey:kUnityAdsZoneIdKey];
}

@end
