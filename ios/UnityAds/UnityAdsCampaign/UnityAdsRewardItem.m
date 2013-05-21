//
//  UnityAdsRewardItem.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsRewardItem.h"

#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"

@implementation UnityAdsRewardItem

- (id)initWithData:(NSDictionary *)data {
  self = [super init];
  if (self) {
    self.isValidRewardItem = false;
    [self setupFromData:data];
  }
  return self;
}

- (void)setupFromData:(NSDictionary *)data {
  BOOL failedData = false;
  
  UAAssertV([data isKindOfClass:[NSDictionary class]], nil);
	  
	id keyValue = [data objectForKey:kUnityAdsRewardItemKeyKey];
  if (keyValue == nil) failedData = true;
	UAAssertV(keyValue != nil && ([keyValue isKindOfClass:[NSString class]] || [keyValue isKindOfClass:[NSNumber class]]), nil);
	NSString *key = [keyValue isKindOfClass:[NSNumber class]] ? [keyValue stringValue] : keyValue;
	UAAssertV(key != nil && [key length] > 0, nil);
  if (key == nil || [key length] == 0) failedData = true;
	self.key = key;
	
	id nameValue = [data objectForKey:kUnityAdsRewardNameKey];
  if (nameValue == nil) failedData = true;
	UAAssertV(nameValue != nil && ([nameValue isKindOfClass:[NSString class]] || [nameValue isKindOfClass:[NSNumber class]]), nil);
	NSString *name = [nameValue isKindOfClass:[NSNumber class]] ? [nameValue stringValue] : nameValue;
	UAAssertV(name != nil && [name length] > 0, nil);
  if (name == nil || [name length] == 0) failedData = true;
	self.name = name;
	
	NSString *pictureURLString = [data objectForKey:kUnityAdsRewardPictureKey];
  if (pictureURLString == nil) failedData = true;
	UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
	NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
	UAAssertV(pictureURL != nil, nil);
  if (pictureURL == nil) failedData = true;
	self.pictureURL = pictureURL;
  
  if (!failedData) {
    self.isValidRewardItem = true;
  }
}

@end
