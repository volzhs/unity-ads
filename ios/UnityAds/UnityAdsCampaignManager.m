//
//  UnityAdsCampaignManager.m
//  UnityAdsExample
//
//  Created by Johan Halin on 5.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCampaignManager.h"

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

@interface UnityAdsCampaignManager () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSMutableData *campaignDownloadData;
@end

@implementation UnityAdsCampaignManager

@synthesize campaignDownloadData = _campaignDownloadData;

#pragma mark - Private

- (void)_processCampaignDownloadData
{
}

#pragma mark - Public

- (void)updateCampaigns
{
	self.campaignDownloadData = [NSMutableData data];
	NSURLRequest *request = [[NSURLRequest alloc] initWithURL:[NSURL URLWithString:kUnityAdsTestBackendURL]];
	[NSURLConnection connectionWithRequest:request delegate:self];
}

#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
	[self.campaignDownloadData appendData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
	[self _processCampaignDownloadData];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	NSLog(@"didFailWithError: %@", error);
}

@end
