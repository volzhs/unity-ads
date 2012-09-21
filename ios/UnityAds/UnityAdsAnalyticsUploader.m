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

NSString * const kUnityAdsAnalyticsURL = @"https://log.applifier.com/videoads-tracking";
NSString * const kUnityAdsTrackingURL = @"https://impact.applifier.com/gamers/";
NSString * const kUnityAdsInstallTrackingURL = @"https://impact.applifier.com/games/";
NSString * const kUnityAdsAnalyticsUploaderRequestKey = @"kUnityAdsAnalyticsUploaderRequestKey";
NSString * const kUnityAdsAnalyticsUploaderConnectionKey = @"kUnityAdsAnalyticsUploaderConnectionKey";
NSString * const kUnityAdsAnalyticsSavedUploadsKey = @"kUnityAdsAnalyticsSavedUploadsKey";
NSString * const kUnityAdsAnalyticsSavedUploadURLKey = @"kUnityAdsAnalyticsSavedUploadURLKey";
NSString * const kUnityAdsAnalyticsSavedUploadBodyKey = @"kUnityAdsAnalyticsSavedUploadBodyKey";
NSString * const kUnityAdsAnalyticsSavedUploadHTTPMethodKey = @"kUnityAdsAnalyticsSavedUploadHTTPMethodKey";
NSString * const kUnityAdsQueryDictionaryQueryKey = @"kUnityAdsQueryDictionaryQueryKey";
NSString * const kUnityAdsQueryDictionaryBodyKey = @"kUnityAdsQueryDictionaryBodyKey";

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
	if ([request URL] != nil)
	{
		[failedUpload setObject:[[request URL] absoluteString] forKey:kUnityAdsAnalyticsSavedUploadURLKey];
		
		if ([request HTTPBody] != nil)
		{
			NSString *bodyString = [[NSString alloc] initWithData:[request HTTPBody] encoding:NSUTF8StringEncoding];
			[failedUpload setObject:bodyString forKey:kUnityAdsAnalyticsSavedUploadBodyKey];
		}
		
		[failedUpload setObject:[request HTTPMethod] forKey:kUnityAdsAnalyticsSavedUploadHTTPMethodKey];
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

- (void)_queueURL:(NSURL *)url body:(NSData *)body httpMethod:(NSString *)httpMethod
{
	if (url == nil)
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
	
	[request setHTTPMethod:httpMethod];
	if (body != nil)
		[request setHTTPBody:body];
	
	UALOG_DEBUG(@"queueing %@", [request URL]);
	
	NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	NSDictionary *uploadDictionary = @{ kUnityAdsAnalyticsUploaderRequestKey : request, kUnityAdsAnalyticsUploaderConnectionKey : connection };
	[self.uploadQueue addObject:uploadDictionary];
	
	if ([self.uploadQueue count] == 1)
		[self _startNextUpload];
}

- (void)_queueWithURLString:(NSString *)urlString queryString:(NSString *)queryString httpMethod:(NSString *)httpMethod
{
	NSURL *url = [NSURL URLWithString:urlString];
	NSData *body = nil;
	if (queryString != nil)
		body = [queryString dataUsingEncoding:NSUTF8StringEncoding];

	[self _queueURL:url body:body httpMethod:httpMethod];
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
	
	[self _queueWithURLString:kUnityAdsAnalyticsURL queryString:queryString httpMethod:@"POST"];
}

- (void)sendTrackingCallWithQueryString:(NSString *)queryString
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
	
	[self _queueWithURLString:[kUnityAdsTrackingURL stringByAppendingString:queryString] queryString:nil httpMethod:@"GET"];
}

- (void)sendInstallTrackingCallWithQueryDictionary:(NSDictionary *)queryDictionary
{
	if ([NSThread isMainThread])
	{
		UALOG_ERROR(@"Cannot be run on main thread.");
		return;
	}
	
	if (queryDictionary == nil)
	{
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
	
	NSString *query = [queryDictionary objectForKey:kUnityAdsQueryDictionaryQueryKey];
	NSString *body = [queryDictionary objectForKey:kUnityAdsQueryDictionaryBodyKey];
	
	if (query == nil || [query length] == 0 || body == nil || [body length] == 0)
	{
		UALOG_DEBUG(@"Invalid parameters in query dictionary.");
		return;
	}
	
	[self _queueWithURLString:[kUnityAdsInstallTrackingURL stringByAppendingString:query] queryString:body httpMethod:@"POST"];
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
			NSString *httpMethod = [upload objectForKey:kUnityAdsAnalyticsSavedUploadHTTPMethodKey];
			[self _queueURL:[NSURL URLWithString:url] body:[body dataUsingEncoding:NSUTF8StringEncoding] httpMethod:httpMethod];
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
