//
//  UnityAdsAnalyticsUploader.m
//  UnityAdsExample
//
//  Created by Johan Halin on 13.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsAnalyticsUploader.h"
#import "UnityAdsCampaign.h"

NSString * const kUnityAdsTestAnalyticsURL = @"http://ads-dev.local/manifest.json";
NSString * const kUnityAdsAnalyticsUploaderRequestKey = @"kUnityAdsAnalyticsUploaderRequestKey";
NSString * const kUnityAdsAnalyticsUploaderConnectionKey = @"kUnityAdsAnalyticsUploaderConnectionKey";
NSString * const kUnityAdsAnalyticsSavedUploadsKey = @"kUnityAdsAnalyticsSavedUploadsKey";

@interface UnityAdsAnalyticsUploader () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSMutableArray *uploadQueue;
@property (nonatomic, strong) NSDictionary *currentUpload;
@end

@implementation UnityAdsAnalyticsUploader

@synthesize uploadQueue = _uploadQueue;
@synthesize currentUpload = _currentUpload;

#pragma mark - Private

- (void)_saveFailedUpload:(NSDictionary *)download
{
	NSMutableArray *existingFailedUploads = [[[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey] mutableCopy];
	
	if (existingFailedUploads == nil)
		existingFailedUploads = [NSMutableArray array];
	
	NSURLRequest *request = [download objectForKey:kUnityAdsAnalyticsUploaderRequestKey];
	NSString *urlString = [[request URL] absoluteString];
	[existingFailedUploads addObject:urlString];
	NSLog(@"%@", existingFailedUploads);
	[[NSUserDefaults standardUserDefaults] setObject:existingFailedUploads forKey:kUnityAdsAnalyticsSavedUploadsKey];
	[[NSUserDefaults standardUserDefaults] synchronize];
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

#pragma mark - Public

- (id)init
{
	if ((self = [super init]))
	{
		_uploadQueue = [NSMutableArray array];
	}
	
	return self;
}

- (void)_queueURL:(NSURL *)url
{
	NSURLRequest *request = [NSURLRequest requestWithURL:url];
	if (request == nil)
	{
		NSLog(@"Request could not be created.");
		return;
	}
	
	NSLog(@"queueing %@", url);
	
	NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	NSDictionary *uploadDictionary = @{ kUnityAdsAnalyticsUploaderRequestKey : request, kUnityAdsAnalyticsUploaderConnectionKey : connection };
	[self.uploadQueue addObject:uploadDictionary];
	
	if ([self.uploadQueue count] == 1)
		[self _startNextUpload];
}

- (void)sendViewReportForCampaign:(UnityAdsCampaign *)campaign positionString:(NSString *)positionString
{
	if ([NSThread isMainThread])
	{
		NSLog(@"Cannot be run on main thread.");
		return;
	}
	
	NSString *urlString = [kUnityAdsTestAnalyticsURL stringByAppendingFormat:@"?d={\"did\":\"%@\",\"c\":\"%@\",\"pos\":\"%@\"}", @"test", campaign.id, positionString];
	[self _queueURL:[NSURL URLWithString:[urlString stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]]];
}

- (void)retryFailedUploads
{
	if ([NSThread isMainThread])
	{
		NSLog(@"Cannot be run on main thread.");
		return;
	}

	NSArray *uploads = [[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey];
	if (uploads != nil)
	{
		for (NSString *url in uploads)
		{
			if ([url isKindOfClass:[NSString class]])
			{
				[self _queueURL:[NSURL URLWithString:url]];
			}
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
	NSLog(@"analytics upload finished");
	
	self.currentUpload = nil;
	
	[self _startNextUpload];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	NSLog(@"didFailWithError: %@", error);
	
	[self _saveFailedUpload:self.currentUpload];

	self.currentUpload = nil;
	
	[self _startNextUpload];
}

@end
