//
//  UnityAdsAnalyticsUploader.m
//  UnityAdsExample
//
//  Created by Johan Halin on 13.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsAnalyticsUploader.h"
#import "UnityAdsCampaign.h"
#import "UnityAds.h"

NSString * const kUnityAdsAnalyticsURL = @"http://log.applifier.com/videoads-tracking";
NSString * const kUnityAdsAnalyticsUploaderRequestKey = @"kUnityAdsAnalyticsUploaderRequestKey";
NSString * const kUnityAdsAnalyticsUploaderConnectionKey = @"kUnityAdsAnalyticsUploaderConnectionKey";
NSString * const kUnityAdsAnalyticsSavedUploadsKey = @"kUnityAdsAnalyticsSavedUploadsKey";
NSString * const kUnityAdsAnalyticsSavedUploadURLKey = @"kUnityAdsAnalyticsSavedUploadURLKey";
NSString * const kUnityAdsAnalyticsSavedUploadBodyKey = @"kUnityAdsAnalyticsSavedUploadBodyKey";

@interface UnityAdsAnalyticsUploader () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSMutableArray *uploadQueue;
@property (nonatomic, strong) NSDictionary *currentUpload;
@end

@implementation UnityAdsAnalyticsUploader

#pragma mark - Private

- (void)_saveFailedUpload:(NSDictionary *)upload
{
	if (upload == nil)
	{
		UALOG_DEBUG(@"Input is nil.");
		return;
	}
	
	NSMutableArray *existingFailedUploads = [[[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey] mutableCopy];
	
	if (existingFailedUploads == nil)
		existingFailedUploads = [NSMutableArray array];
	
	NSURLRequest *request = [upload objectForKey:kUnityAdsAnalyticsUploaderRequestKey];
	NSMutableDictionary *failedUpload = [NSMutableDictionary dictionary];
	if ([request URL] != nil && [request HTTPBody] != nil)
	{
		[failedUpload setObject:[[request URL] absoluteString] forKey:kUnityAdsAnalyticsSavedUploadURLKey];
		NSString *bodyString = [[NSString alloc] initWithData:[request HTTPBody] encoding:NSUTF8StringEncoding];
		[failedUpload setObject:bodyString forKey:kUnityAdsAnalyticsSavedUploadBodyKey];
		[existingFailedUploads addObject:failedUpload];
		
		UALOG_DEBUG(@"%@", existingFailedUploads);
		[[NSUserDefaults standardUserDefaults] setObject:existingFailedUploads forKey:kUnityAdsAnalyticsSavedUploadsKey];
		[[NSUserDefaults standardUserDefaults] synchronize];
	}
}

- (BOOL)_startNextUpload
{
	if (self.currentUpload != nil)
		return NO;
	
	if ([self.uploadQueue count] > 0)
	{
		self.currentUpload = [self.uploadQueue objectAtIndex:0];
		
		NSURLConnection *connection = [self.currentUpload objectForKey:kUnityAdsAnalyticsUploaderConnectionKey];
		[connection start];
		
		[self.uploadQueue removeObjectAtIndex:0];
	}
	else
		return NO;
	
	return YES;
}

- (void)_queueURL:(NSURL *)url body:(NSData *)body
{
	if (url == nil || body == nil)
	{
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
	
	NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:url];

	if (request == nil)
	{
		UALOG_DEBUG(@"Could not create request.");
		return;
	}
	
	[request setHTTPMethod:@"POST"];
	[request setHTTPBody:body];
	
	UALOG_DEBUG(@"queueing %@", [request URL]);
	
	NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	NSDictionary *uploadDictionary = @{ kUnityAdsAnalyticsUploaderRequestKey : request, kUnityAdsAnalyticsUploaderConnectionKey : connection };
	[self.uploadQueue addObject:uploadDictionary];
	
	if ([self.uploadQueue count] == 1)
		[self _startNextUpload];
}

#pragma mark - Public

- (id)init
{
	if ((self = [super init]))
	{
		_uploadQueue = [NSMutableArray array];
	}
	
	return self;
}

- (void)sendViewReportWithQueryString:(NSString *)queryString
{
	if ([NSThread isMainThread])
	{
		UALOG_ERROR(@"Cannot be run on main thread.");
		return;
	}
	
	if (queryString == nil || [queryString length] == 0)
	{
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
	
	NSURL *url = [NSURL URLWithString:kUnityAdsAnalyticsURL];
	[self _queueURL:url body:[queryString dataUsingEncoding:NSUTF8StringEncoding]];
}

- (void)retryFailedUploads
{
	if ([NSThread isMainThread])
	{
		UALOG_ERROR(@"Cannot be run on main thread.");
		return;
	}

	NSArray *uploads = [[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey];
	if (uploads != nil)
	{
		for (NSDictionary *upload in uploads)
		{
			NSString *url = [upload objectForKey:kUnityAdsAnalyticsSavedUploadURLKey];
			NSString *body = [upload objectForKey:kUnityAdsAnalyticsSavedUploadBodyKey];
			[self _queueURL:[NSURL URLWithString:url] body:[body dataUsingEncoding:NSUTF8StringEncoding]];
		}
		
		[[NSUserDefaults standardUserDefaults] removeObjectForKey:kUnityAdsAnalyticsSavedUploadsKey];
		[[NSUserDefaults standardUserDefaults] synchronize];
	}
}

#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
	UALOG_DEBUG(@"analytics upload finished");
	
	self.currentUpload = nil;
	
	[self _startNextUpload];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	UALOG_DEBUG(@"%@", error);
	
	[self _saveFailedUpload:self.currentUpload];

	self.currentUpload = nil;
	
	[self _startNextUpload];
}

@end
