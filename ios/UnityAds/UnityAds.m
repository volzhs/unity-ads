//
//  UnityAds.m
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsiOS4.h"

@implementation UnityAds

@synthesize delegate = _delegate;

#pragma mark - Private

- (id)initAdsInstance
{
	if ((self = [super init]))
	{
	}
	
	return self;
}

#pragma mark - Public

static UnityAds *sharedAdsInstance = nil;

- (id)init
{
	[self doesNotRecognizeSelector:_cmd];
	
	return nil;
}

+ (id)sharedInstance
{
	@synchronized(self)
	{
		if (sharedAdsInstance == nil)
		{
			if ([self respondsToSelector:@selector(autoContentAccessingProxy)]) // check if we're on at least iOS 4.0
				sharedAdsInstance = [[UnityAdsiOS4 alloc] initAdsInstance];
			else
				sharedAdsInstance = [[self alloc] initAdsInstance];
		}
	}
	
	return sharedAdsInstance;
}

- (void)startWithGameId:(NSString *)gameId
{
	// do nothing
}

- (UIView *)adsView
{
	return nil;
}

- (BOOL)canShow
{
	return NO;
}

- (void)stopAll
{
	// do nothing
}

@end
