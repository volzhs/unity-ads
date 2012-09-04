//
//  UnityAds.m
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"

NSString * const kUnityAdsTestBackendURL = @"http://ads-dev.local/manifest.json";
NSString * const kUnityAdsTestWebViewURL = @"http://ads-dev.local/webapp.html";

/*
 VideoPlan (for dev / testing purposes)
 The data requested from backend that contains the campaigns that should be used at this time http://ads-dev.local/manifest.json? d={"did":"DEVICE_ID","c":["ARRAY", “OF”, “CACHED”, “CAMPAIGN”, “ID S”]}
 
 ViewReport (for dev / testing purposes)
 Reporting the current position (view) of video, watch to the Backend
 http://ads-dev.local/manifest.json?
 v={"did":"DEVICE_ID","c":”VIEWED_CAMPAIGN_ID”, “pos”:”POSITION_ OPTION”}
 Position options are: start, first_quartile, mid_point, third_quartile,
 end
 */

@interface UnityAds ()
@property (nonatomic, strong) NSString *gameId;
@end

@implementation UnityAds

@synthesize gameId = _gameId;
@synthesize delegate = _delegate;

#pragma mark - Public

static UnityAds *sharedAdsInstance = nil;

+ (id)sharedInstance
{
	@synchronized(self)
	{
		if (sharedAdsInstance == nil)
			sharedAdsInstance = [[self alloc] init];
	}
	
	return sharedAdsInstance;
}

- (void)startWithGameId:(NSString *)gameId
{
	if ( ! [self respondsToSelector:@selector(autoContentAccessingProxy)]) // check if we're on at least iOS 4.0
		return;
	
	self.gameId = gameId;
}

- (BOOL)show
{
	if (self.gameId == nil)
		return NO;
	
	return YES;
}

- (BOOL)hasCampaigns
{
	if (self.gameId == nil)
		return NO;
	
	return NO;
}

- (void)stopAll
{
	if (self.gameId == nil)
		return;
}

@end
