//
//  UnityAdsProperties.m
//  UnityAds
//
//  Created by bluesun on 11/2/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsProperties.h"

@implementation UnityAdsProperties

static UnityAdsProperties *sharedProperties = nil;

+ (id)sharedInstance
{
	@synchronized(self)
	{
		if (sharedProperties == nil)
      sharedProperties = [[UnityAdsProperties alloc] init];
	}
	
	return sharedProperties;
}

-(UnityAdsProperties *)init {
  if (self = [super init]) {
    [self setCampaignDataUrl:@"https://impact.applifier.com/mobile/campaigns"];
  }
  
  return self;
}

@end
