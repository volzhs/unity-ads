//
//  UnityAdsCache.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/6/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCache.h"
#import "UnityAdsCampaign.h"

NSString const * kUnityAdsCacheCampaignKey = @"kUnityAdsCacheCampaignKey";
NSString const * kUnityAdsCacheConnectionKey = @"kUnityAdsCacheConnectionKey";
NSString const * kUnityAdsCacheFilePathKey = @"kUnityAdsCacheFilePathKey";

@interface UnityAdsCache () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSFileHandle *fileHandle;
@property (nonatomic, strong) NSMutableArray *downloadQueue;
@property (nonatomic, strong) NSDictionary *currentDownload;
@end

@implementation UnityAdsCache

@synthesize delegate = _delegate;
@synthesize fileHandle = _fileHandle;
@synthesize downloadQueue = _downloadQueue;
@synthesize currentDownload = _currentDownload;

#pragma mark - Private

- (NSString *)_cachePath
{
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
	if (paths == nil || [paths count] == 0)
		return nil;
	
	return [[paths objectAtIndex:0] stringByAppendingString:@"/applifier/"];
}

- (void)_queueCampaignDownload:(UnityAdsCampaign *)campaign;
{
	if (campaign == nil)
	{
		NSLog(@"Campaign cannot be nil.");
		return;
	}
	
	NSLog(@"Queueing %@, id %@", campaign.trailerDownloadableURL, campaign.id);
	
	NSString *filePath = [[self _cachePath] stringByAppendingString:[NSString stringWithFormat:@"%@.mp4", campaign.id]];		
	NSURLRequest *request = [[NSURLRequest alloc] initWithURL:campaign.trailerDownloadableURL];
	NSURLConnection *urlConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	NSDictionary *downloadDictionary = @{ kUnityAdsCacheCampaignKey : campaign, kUnityAdsCacheConnectionKey : urlConnection, kUnityAdsCacheFilePathKey : filePath };
	[self.downloadQueue addObject:downloadDictionary];
	[self _startDownload];
}

- (BOOL)_startNextDownloadInQueue
{
	if (self.currentDownload != nil)
		return NO;
	
	if ([self.downloadQueue count] > 0)
	{
		self.currentDownload = [self.downloadQueue objectAtIndex:0];
		
		NSString *filePath = [self.currentDownload objectForKey:kUnityAdsCacheFilePathKey];
		if ([[NSFileManager defaultManager] fileExistsAtPath:filePath])
		{
			NSLog(@"TODO: file exists"); // e.g., resume or what
		}
		else
		{
			if ( ! [[NSFileManager defaultManager] createFileAtPath:filePath contents:nil attributes:nil])
			{
				NSLog(@"Unable to create file at %@", filePath);
				self.currentDownload = nil;
				return NO;
			}
		}
		
		self.fileHandle = [NSFileHandle fileHandleForWritingAtPath:filePath];

		NSURLConnection *connection = [self.currentDownload objectForKey:kUnityAdsCacheConnectionKey];
		[connection start];

		[self.downloadQueue removeObjectAtIndex:0];
	}
	else
		return NO;
	
	NSLog(@"starting download %@", self.currentDownload);

	return YES;
}

- (void)_startDownload
{
	BOOL downloadStarted = [self _startNextDownloadInQueue];
	if ( ! downloadStarted && self.currentDownload == nil && [self.downloadQueue count] > 0)
		[self performSelector:@selector(_startDownload) withObject:self afterDelay:3.0];
}

- (void)_downloadFinishedWithFailure:(BOOL)failure
{
	NSLog(@"download finished with failure: %@", failure ? @"yes" : @"no");
	
	[self.fileHandle closeFile];
	self.fileHandle = nil;
	
	if (failure)
	{
		UnityAdsCampaign *campaign = [self.currentDownload objectForKey:kUnityAdsCacheCampaignKey];
		[self _queueCampaignDownload:campaign];
	}
	else
	{
		if ([self.delegate respondsToSelector:@selector(cache:finishedCachingCampaign:)])
			[self.delegate cache:self finishedCachingCampaign:[self.currentDownload objectForKey:kUnityAdsCacheCampaignKey]];
	}
	
	self.currentDownload = nil;
	
	if ([self.downloadQueue count] == 0)
	{
		if ([self.delegate respondsToSelector:@selector(cacheFinishedCachingCampaigns:)])
			[self.delegate cacheFinishedCachingCampaigns:self];
	}
	
	[self _startDownload];
}

- (void)_compareCampaigns:(NSArray *)campaigns
{
	if (campaigns == nil || [campaigns count] == 0)
	{
		NSLog(@"No new campaigns.");
		return;
	}
	
	NSString *cachePath = [self _cachePath];
	NSString *campaignIndexPath = [cachePath stringByAppendingString:@"index.plist"];
	NSArray *oldIndex = [NSArray arrayWithContentsOfFile:campaignIndexPath];
	
	NSMutableArray *index = [NSMutableArray array];
	for (UnityAdsCampaign *campaign in campaigns)
	{
		if (campaign.id != nil)
			[index addObject:[campaign.id stringByAppendingString:@".mp4"]];
	}
	
	if ( ! [index writeToFile:campaignIndexPath atomically:YES])
		NSLog(@"Saving campaign index failed.");
	
	for (NSString *oldFile in oldIndex)
	{
		if ( ! [index containsObject:oldFile])
		{
			NSString *filePath = [cachePath stringByAppendingString:oldFile];
			NSError *error = nil;
			if ( ! [[NSFileManager defaultManager] removeItemAtPath:filePath error:&error])
			{
				NSLog(@"Unable to remove file. %@", error);
			}
		}
	}
}

#pragma mark - Public

- (id)init
{
	if ((self = [super init]))
	{
		_downloadQueue = [NSMutableArray array];
	}
	
	return self;
}

- (void)cacheCampaigns:(NSArray *)campaigns
{
	NSError *error = nil;
	NSString *cachePath = [self _cachePath];
	if ( ! [[NSFileManager defaultManager] createDirectoryAtPath:cachePath withIntermediateDirectories:YES attributes:nil error:&error])
	{
		NSLog(@"Couldn't create cache path. Error: %@", error);
		return;
	}
	
	// TODO: check queue for existing downloads that should be cancelled
	
	for (UnityAdsCampaign *campaign in campaigns)
	{
		[self _queueCampaignDownload:campaign];
	}
	
	[self _compareCampaigns:campaigns];
}

#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
	NSLog(@"didReceiveResponse: %@", response);
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
	[self.fileHandle writeData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
	[self _downloadFinishedWithFailure:NO];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	NSLog(@"didFailWithError: %@", error);
	
	[self _downloadFinishedWithFailure:YES];
}

@end
