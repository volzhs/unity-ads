//
//  UnityAdsCache.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/6/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsCache.h"
#import "UnityAdsCampaign.h"

NSString * const kUnityAdsCacheCampaignKey = @"kUnityAdsCacheCampaignKey";
NSString * const kUnityAdsCacheConnectionKey = @"kUnityAdsCacheConnectionKey";
NSString * const kUnityAdsCacheFilePathKey = @"kUnityAdsCacheFilePathKey";
NSString * const kUnityAdsCacheURLRequestKey = @"kUnityAdsCacheURLRequestKey";
NSString * const kUnityAdsCacheIndexKey = @"kUnityAdsCacheIndexKey";
NSString * const kUnityAdsCacheResumeKey = @"kUnityAdsCacheResumeKey";

NSString * const kUnityAdsCacheDownloadResumeExpected = @"kUnityAdsCacheDownloadResumeExpected";
NSString * const kUnityAdsCacheDownloadNewDownload = @"kUnityAdsCacheDownloadNewDownload";

NSString * const kUnityAdsCacheEntryCampaignIDKey = @"kUnityAdsCacheEntryCampaignIDKey";
NSString * const kUnityAdsCacheEntryFilenameKey = @"kUnityAdsCacheEntryFilenameKey";
NSString * const kUnityAdsCacheEntryFilesizeKey = @"kUnityAdsCacheEntryFilesizeKey";

@interface UnityAdsCache () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSFileHandle *fileHandle;
@property (nonatomic, strong) NSMutableArray *downloadQueue;
@property (nonatomic, strong) NSMutableDictionary *currentDownload;
@end

@implementation UnityAdsCache

#pragma mark - Private

- (NSString *)_cachePath
{
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
	UAAssertV(paths != nil && [paths count] > 0, nil);
	
	return [[paths objectAtIndex:0] stringByAppendingPathComponent:@"unityads"];
}

- (NSString *)_videoFilenameForCampaign:(UnityAdsCampaign *)campaign
{
	return [NSString stringWithFormat:@"%@-%@", campaign.id, [campaign.trailerDownloadableURL lastPathComponent]];
}

- (NSString *)_videoPathForCampaign:(UnityAdsCampaign *)campaign
{
	return [[self _cachePath] stringByAppendingPathComponent:[self _videoFilenameForCampaign:campaign]];
}

- (long long)_cachedFilesizeForVideoFilename:(NSString *)filename
{
	NSArray *index = [[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsCacheIndexKey];
	long long size = 0;
	
	for (NSDictionary *cacheEntry in index)
	{
		NSString *indexFilename = [cacheEntry objectForKey:kUnityAdsCacheEntryFilenameKey];
		if ([filename isEqualToString:indexFilename])
		{
			size = [[cacheEntry objectForKey:kUnityAdsCacheEntryFilesizeKey] longLongValue];
			break;
		}
	}
	
	return size;
}

- (BOOL)_campaignExistsInQueue:(UnityAdsCampaign *)campaign
{
	BOOL exists = NO;
	
	for (NSDictionary *downloadDictionary in self.downloadQueue)
	{
		UnityAdsCampaign *downloadCampaign = [downloadDictionary objectForKey:kUnityAdsCacheCampaignKey];
		
		if ([downloadCampaign.id isEqualToString:campaign.id] && [downloadCampaign.trailerDownloadableURL isEqual:campaign.trailerDownloadableURL])
		{
			UALOG_DEBUG(@"Campaign '%@' exists in queue.", campaign.id);
			exists = YES;
		}
	}
	
	return exists;
}

- (BOOL)_queueCampaignDownload:(UnityAdsCampaign *)campaign
{
	if (campaign == nil)
	{
		UALOG_DEBUG(@"Campaign cannot be nil.");
		return NO;
	}
	
	NSString *filePath = [self _videoPathForCampaign:campaign];
	long long existingFilesize = [self _filesizeForPath:filePath];
	long long filesize = [self _cachedFilesizeForVideoFilename:[self _videoFilenameForCampaign:campaign]];
	
	if ( ! [self _campaignExistsInQueue:campaign] && (existingFilesize < filesize || filesize == 0))
	{
		UALOG_DEBUG(@"Queueing %@, id %@", campaign.trailerDownloadableURL, campaign.id);
		
		NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:campaign.trailerDownloadableURL];
		NSMutableDictionary *downloadDictionary = [NSMutableDictionary dictionary];
		[downloadDictionary setObject:request forKey:kUnityAdsCacheURLRequestKey];
		[downloadDictionary setObject:campaign forKey:kUnityAdsCacheCampaignKey];
		[downloadDictionary setObject:filePath forKey:kUnityAdsCacheFilePathKey];
		[downloadDictionary setObject:(existingFilesize > 0 ? kUnityAdsCacheDownloadResumeExpected : kUnityAdsCacheDownloadNewDownload) forKey:kUnityAdsCacheResumeKey];
		[self.downloadQueue addObject:downloadDictionary];
		[self _startDownload];
		
		return YES;
	}
	
	return NO;
}

- (long long)_filesizeForPath:(NSString *)path
{
	long long size = 0;
	
	if ([[NSFileManager defaultManager] fileExistsAtPath:path])
	{
		NSDictionary *attributes = [[NSFileManager defaultManager] attributesOfItemAtPath:path error:nil];
		size = [attributes fileSize];
	}
	
	return size;
}

- (BOOL)_startNextDownloadInQueue
{
	if (self.currentDownload != nil)
		return NO;
	
	if ([self.downloadQueue count] > 0)
	{
		self.currentDownload = [self.downloadQueue objectAtIndex:0];
		
		NSMutableURLRequest *request = [self.currentDownload objectForKey:kUnityAdsCacheURLRequestKey];
		NSString *filePath = [self.currentDownload objectForKey:kUnityAdsCacheFilePathKey];
		
		if ( ! [[NSFileManager defaultManager] fileExistsAtPath:filePath])
		{
			if ( ! [[NSFileManager defaultManager] createFileAtPath:filePath contents:nil attributes:nil])
			{
				UALOG_DEBUG(@"Unable to create file at %@", filePath);
				self.currentDownload = nil;
				return NO;
			}
		}
		
		long long rangeStart = [self _filesizeForPath:filePath];
		self.fileHandle = [NSFileHandle fileHandleForWritingAtPath:filePath];
		if (rangeStart > 0)
		{
			[self.fileHandle seekToEndOfFile];
			[request setValue:[NSString stringWithFormat:@"bytes=%qi-", rangeStart] forHTTPHeaderField:@"Range"];
		}
		
		NSURLConnection *urlConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
		[self.currentDownload setObject:urlConnection forKey:kUnityAdsCacheConnectionKey];
		[urlConnection start];

		[self.downloadQueue removeObjectAtIndex:0];
	}
	else
		return NO;
	
	UALOG_DEBUG(@"starting download %@", self.currentDownload);

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
	UALOG_DEBUG(@"download finished with failure: %@", failure ? @"yes" : @"no");
	
	[self.fileHandle closeFile];
	self.fileHandle = nil;
	
	if (failure)
	{
		UnityAdsCampaign *campaign = [self.currentDownload objectForKey:kUnityAdsCacheCampaignKey];
		[self _queueCampaignDownload:campaign];
	}
	else
		[self.delegate cache:self finishedCachingCampaign:[self.currentDownload objectForKey:kUnityAdsCacheCampaignKey]];
	
	self.currentDownload = nil;
	
	if ([self.downloadQueue count] == 0)
		[self.delegate cacheFinishedCachingCampaigns:self];
	
	[self _startDownload];
}

- (void)_cleanUpIndexWithCampaigns:(NSArray *)campaigns
{
	// FIXME: what to do with old campaigns?
	if (campaigns == nil || [campaigns count] == 0)
	{
		UALOG_DEBUG(@"No new campaigns.");
		return;
	}
	
	NSString *cachePath = [self _cachePath];
	NSMutableArray *oldIndex = [[[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsCacheIndexKey] mutableCopy];
	NSMutableArray *entriesToRemove = [NSMutableArray array];
	
	for (NSDictionary *oldEntry in oldIndex)
	{
		NSString *oldFilename = [oldEntry objectForKey:kUnityAdsCacheEntryFilenameKey];
		NSString *oldCampaignID = [oldEntry objectForKey:kUnityAdsCacheEntryCampaignIDKey];
		BOOL found = NO;
		
		for (UnityAdsCampaign *campaign in campaigns)
		{
			NSString *filename = [self _videoFilenameForCampaign:campaign];
			if ([oldFilename isEqualToString:filename] && [oldCampaignID isEqualToString:campaign.id])
			{
				found = YES;
				break;
			}
		}
		
		if ( ! found)
		{
			NSString *filePath = [cachePath stringByAppendingPathComponent:oldFilename];
			NSError *error = nil;
			if ([[NSFileManager defaultManager] removeItemAtPath:filePath error:&error])
			{
				UALOG_DEBUG(@"Deleted file '%@'", filePath);
				[entriesToRemove addObject:oldEntry];
			}
			else
				UALOG_DEBUG(@"Unable to remove file. %@", error);
		}
	}
	
	if ([entriesToRemove count] > 0)
	{
		UALOG_DEBUG(@"Removing entries from index: %@", entriesToRemove);
		[oldIndex removeObjectsInArray:entriesToRemove];
		[[NSUserDefaults standardUserDefaults] setObject:oldIndex forKey:kUnityAdsCacheIndexKey];
		[[NSUserDefaults standardUserDefaults] synchronize];
	}
	else
		UALOG_DEBUG(@"No cache index entries to remove.");
}

- (void)_saveCurrentlyDownloadingCampaignToIndexWithFilesize:(long long)filesize
{
	NSMutableArray *index = [[[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsCacheIndexKey] mutableCopy];
	if (index == nil)
		index = [NSMutableArray array];
	
	UnityAdsCampaign *campaign = [self.currentDownload objectForKey:kUnityAdsCacheCampaignKey];

	BOOL found = NO;
	if (campaign != nil)
	{
		for (NSDictionary *cacheEntry in index)
		{
			NSString *campaignID = [cacheEntry objectForKey:kUnityAdsCacheEntryCampaignIDKey];
			if ([campaignID isEqualToString:campaign.id])
			{
				NSString *filename = [self _videoFilenameForCampaign:campaign];
				NSString *oldFilename = [cacheEntry objectForKey:kUnityAdsCacheEntryFilenameKey];
				
				if ([filename isEqualToString:oldFilename])
				{
					found = YES;
					break;
				}
			}
		}

		if ( ! found)
		{
			UALOG_DEBUG(@"Adding campaign '%@' to index.", campaign.id);
			NSMutableDictionary *cacheEntry = [NSMutableDictionary dictionary];
			[cacheEntry setObject:campaign.id forKey:kUnityAdsCacheEntryCampaignIDKey];
			[cacheEntry setObject:[self _videoFilenameForCampaign:campaign] forKey:kUnityAdsCacheEntryFilenameKey];
			[cacheEntry setObject:[NSNumber numberWithLongLong:filesize] forKey:kUnityAdsCacheEntryFilesizeKey];
			[index addObject:cacheEntry];
			[[NSUserDefaults standardUserDefaults] setObject:index forKey:kUnityAdsCacheIndexKey];
			[[NSUserDefaults standardUserDefaults] synchronize];
		}
		else
			UALOG_DEBUG(@"Campaign '%@' already exists in index.", campaign.id);
	}
}

- (void)_removeInvalidDownloadsWithCampaigns:(NSArray *)campaigns
{
	if ([self.downloadQueue count] == 0)
	{
		UALOG_DEBUG(@"No downloads queued.");
		return;
	}
	
	NSMutableArray *downloadsToRemove = [NSMutableArray array];
	
	for (NSDictionary *downloadDictionary in self.downloadQueue)
	{
		UnityAdsCampaign *downloadCampaign = [downloadDictionary objectForKey:kUnityAdsCacheCampaignKey];
		BOOL found = NO;
		
		for (UnityAdsCampaign *campaign in campaigns)
		{
			if ([campaign.id isEqualToString:downloadCampaign.id] && [campaign.trailerDownloadableURL isEqual:downloadCampaign.trailerDownloadableURL])
			{
				found = YES;
				break;
			}
		}
		
		if ( ! found)
			[downloadsToRemove addObject:downloadDictionary];
	}
	
	if ([downloadsToRemove count] > 0)
	{
		UALOG_DEBUG(@"Removing downloads from queue: %@", downloadsToRemove);
		[self.downloadQueue removeObjectsInArray:downloadsToRemove];
	}
	else
		UALOG_DEBUG(@"Not removing any downloads from the queue.");
}

#pragma mark - Public

- (id)init
{
	UAAssertV( ! [NSThread isMainThread], nil);
	
	if ((self = [super init]))
	{
		_downloadQueue = [NSMutableArray array];
	}
	
	return self;
}

- (void)cacheCampaigns:(NSArray *)campaigns
{
	UAAssert( ! [NSThread isMainThread]);
	
	if (campaigns == nil)
	{
		UALOG_DEBUG(@"Input is nil.");
		return;
	}
	
	NSError *error = nil;
	NSString *cachePath = [self _cachePath];
	if ( ! [[NSFileManager defaultManager] createDirectoryAtPath:cachePath withIntermediateDirectories:YES attributes:nil error:&error])
	{
		UALOG_DEBUG(@"Couldn't create cache path. Error: %@", error);
		return;
	}
	
	[self _removeInvalidDownloadsWithCampaigns:campaigns];
	[self _cleanUpIndexWithCampaigns:campaigns];
	
	BOOL downloadsQueued = NO;
	for (UnityAdsCampaign *campaign in campaigns)
	{
		if ([self _queueCampaignDownload:campaign])
			downloadsQueued = YES;
	}
	
	if ( ! downloadsQueued)
	{
		UALOG_DEBUG(@"No new or partial videos to download.");
		[self.delegate cacheFinishedCachingCampaigns:self];
	}
}

- (NSURL *)localVideoURLForCampaign:(UnityAdsCampaign *)campaign
{
	@synchronized (self)
	{
		if (campaign == nil)
		{
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
		
		NSString *path = [self _videoPathForCampaign:campaign];
		
		return [NSURL fileURLWithPath:path];
	}
}

- (void)cancelAllDownloads
{
	UAAssert( ! [NSThread isMainThread]);
	
	if (self.currentDownload != nil)
	{
		NSURLConnection *connection = [self.currentDownload objectForKey:kUnityAdsCacheConnectionKey];
		[connection cancel];
		[self.fileHandle closeFile];
		self.fileHandle = nil;
		self.currentDownload = nil;
	}
	
	[self.downloadQueue removeAllObjects];
}

#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
	NSHTTPURLResponse *httpResponse = nil;
	if ([response isKindOfClass:[NSHTTPURLResponse class]])
		httpResponse = (NSHTTPURLResponse *)response;
	
	NSString *resumeStatus = [self.currentDownload objectForKey:kUnityAdsCacheResumeKey];
	BOOL resumeExpected = [resumeStatus isEqualToString:kUnityAdsCacheDownloadResumeExpected];
	if (resumeExpected && [httpResponse statusCode] == 200)
	{
		UALOG_DEBUG(@"Resume expected but got status code 200, restarting download.");
		
		[self.fileHandle truncateFileAtOffset:0];
	}
	else if ([httpResponse statusCode] == 206)
	{
		UALOG_DEBUG(@"Resuming download.");
	}
	
	NSNumber *contentLength = [[httpResponse allHeaderFields] objectForKey:@"Content-Length"];
	if (contentLength != nil)
	{
		long long size = [contentLength longLongValue];
		[self _saveCurrentlyDownloadingCampaignToIndexWithFilesize:size];
		
		NSDictionary *fsAttributes = [[NSFileManager defaultManager] attributesOfFileSystemForPath:[self _cachePath] error:nil];
		if (fsAttributes != nil)
		{
			long long freeSpace = [[fsAttributes objectForKey:NSFileSystemFreeSize] longLongValue];
			if (size > freeSpace)
			{
				UALOG_DEBUG(@"Not enough space, canceling download. (%lld needed, %lld free)", size, freeSpace);
				[connection cancel];
				[self _downloadFinishedWithFailure:YES];
			}
		}
	}
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
	UALOG_DEBUG(@"%@", error);
	
	[self _downloadFinishedWithFailure:YES];
}

@end
