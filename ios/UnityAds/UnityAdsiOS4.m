//
//  UnityAdsiOS4.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsiOS4.h"

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

@implementation UnityAdsiOS4

@synthesize gameId = _gameId;

#pragma mark - Public

- (void)startWithGameId:(NSString *)gameId
{
	self.gameId = gameId;
}

- (BOOL)show
{
	return YES;
}

- (BOOL)hasCampaigns
{
	return YES;
}

- (void)stopAll
{
}

@end
