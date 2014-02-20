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

- (id)initWithData:(NSDictionary *)data {
  self = [super init];
  if (self) {
    @try {
      [self setupFromData:data];
    } @catch (NSException *exception) {
      return nil;
    }
  }
  return self;
}

- (void)setupFromData:(NSDictionary *)data {
	id keyValue = [data objectForKey:kUnityAdsRewardItemKeyKey];
	NSString *key = [keyValue isKindOfClass:[NSNumber class]] ? [keyValue stringValue] : keyValue;
  if (key == nil || [key length] == 0) {
    [NSException raise:@"itemKeyException" format:@"Item key is invalid"];
  }
	self.key = key;
	
	id nameValue = [data objectForKey:kUnityAdsRewardNameKey];
	NSString *name = [nameValue isKindOfClass:[NSNumber class]] ? [nameValue stringValue] : nameValue;
  if (name == nil || [name length] == 0) {
    [NSException raise:@"itemNameException" format:@"Item name is invalid"];
  }
	self.name = name;
	
	NSString *pictureURLString = [data objectForKey:kUnityAdsRewardPictureKey];
	NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
  if (pictureURL == nil) {
    [NSException raise:@"itemPictureException" format:@"Item picture is invalid"];
  }
	self.pictureURL = pictureURL;
}

- (NSDictionary *)getDetails {
  return @{kUnityAdsRewardItemNameKey:self.name, kUnityAdsRewardItemPictureKey:self.pictureURL};
}

@end
