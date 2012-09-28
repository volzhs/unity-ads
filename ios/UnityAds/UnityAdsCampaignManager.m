//
//  UnityAdsCampaignManager.m
//  UnityAdsExample
//
//  Created by Johan Halin on 5.9.2012.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCampaignManager.h"
#import "UnityAdsSBJSONParser.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"
#import "UnityAdsCache.h"
#import "UnityAds.h"

NSString * const kUnityAdsBackendURL = @"https://impact.applifier.com/mobile/campaigns";

NSString * const kCampaignEndScreenKey = @"endScreen";
NSString * const kCampaignClickURLKey = @"clickUrl";
NSString * const kCampaignPictureKey = @"picture";
NSString * const kCampaignTrailerDownloadableKey = @"trailerDownloadable";
NSString * const kCampaignTrailerStreamingKey = @"trailerStreaming";
NSString * const kCampaignGameIDKey = @"gameId";
NSString * const kCampaignGameNameKey = @"gameName";
NSString * const kCampaignIDKey = @"id";
NSString * const kCampaignTaglineKey = @"tagline";
NSString * const kCampaignStoreIDKey = @"iTunesId";

NSString * const kRewardItemKey = @"itemKey";
NSString * const kRewardNameKey = @"name";
NSString * const kRewardPictureKey = @"picture";

NSString * const kGamerIDKey = @"gamerId";

@interface UnityAdsCampaignManager () <NSURLConnectionDelegate, UnityAdsCacheDelegate>
@property (nonatomic, strong) NSURLConnection *urlConnection;
@property (nonatomic, strong) NSMutableData *campaignDownloadData;
@property (nonatomic, strong) UnityAdsCache *cache;
@property (nonatomic, strong) NSArray *campaigns;
@property (nonatomic, strong) UnityAdsRewardItem *rewardItem;
@property (nonatomic, strong) NSString *gamerID;
@property (nonatomic, strong) NSString *campaignJSON;
@end

@implementation UnityAdsCampaignManager

#pragma mark - Private

- (id)_JSONValueFromData:(NSData *)data
{
	UAAssertV(data != nil, nil);
	
	UnityAdsSBJsonParser *parser = [[UnityAdsSBJsonParser alloc] init];
	NSError *error = nil;
	NSString *jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
	if ([jsonString isEqualToString:self.campaignJSON])
		return nil;
	
	id repr = [parser objectWithString:jsonString error:&error];
	if (repr == nil)
	{
		UALOG_DEBUG(@"-JSONValue failed. Error is: %@", error);
		UALOG_DEBUG(@"String value: %@", jsonString);

		return nil;
	}
	
	self.campaignJSON = jsonString;
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.delegate campaignManager:self updatedJSON:jsonString];
	});
	
	return repr;
}

- (NSArray *)_deserializeCampaigns:(NSArray *)campaignArray
{
	if (campaignArray == nil || [campaignArray count] == 0)
	{
		UALOG_DEBUG(@"Input empty or nil.");
		return nil;
	}
	
	NSMutableArray *campaigns = [NSMutableArray array];
	
	for (id campaignDictionary in campaignArray)
	{
		if ([campaignDictionary isKindOfClass:[NSDictionary class]])
		{
			UnityAdsCampaign *campaign = [[UnityAdsCampaign alloc] init];
			
			NSString *endScreenURLString = [campaignDictionary objectForKey:kCampaignEndScreenKey];
			UAAssertV([endScreenURLString isKindOfClass:[NSString class]], nil);
			NSURL *endScreenURL = [NSURL URLWithString:endScreenURLString];
			UAAssertV(endScreenURL != nil, nil);
			campaign.endScreenURL = endScreenURL;
			
			NSString *clickURLString = [campaignDictionary objectForKey:kCampaignClickURLKey];
			UAAssertV([clickURLString isKindOfClass:[NSString class]], nil);
			NSURL *clickURL = [NSURL URLWithString:clickURLString];
			UAAssertV(clickURL != nil, nil);
			campaign.clickURL = clickURL;
			
			NSString *pictureURLString = [campaignDictionary objectForKey:kCampaignPictureKey];
			UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
			NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
			UAAssertV(pictureURL != nil, nil);
			campaign.pictureURL = pictureURL;
			
			NSString *trailerDownloadableURLString = [campaignDictionary objectForKey:kCampaignTrailerDownloadableKey];
			UAAssertV([trailerDownloadableURLString isKindOfClass:[NSString class]], nil);
			NSURL *trailerDownloadableURL = [NSURL URLWithString:trailerDownloadableURLString];
			UAAssertV(trailerDownloadableURL != nil, nil);
			campaign.trailerDownloadableURL = trailerDownloadableURL;
			
			NSString *trailerStreamingURLString = [campaignDictionary objectForKey:kCampaignTrailerStreamingKey];
			UAAssertV([trailerStreamingURLString isKindOfClass:[NSString class]], nil);
			NSURL *trailerStreamingURL = [NSURL URLWithString:trailerStreamingURLString];
			UAAssertV(trailerStreamingURL != nil, nil);
			campaign.trailerStreamingURL = trailerStreamingURL;
			
			id gameIDValue = [campaignDictionary objectForKey:kCampaignGameIDKey];
			UAAssertV(gameIDValue != nil && ([gameIDValue isKindOfClass:[NSString class]] || [gameIDValue isKindOfClass:[NSNumber class]]), nil);
			NSString *gameID = [gameIDValue isKindOfClass:[NSNumber class]] ? [gameIDValue stringValue] : gameIDValue;
			UAAssertV(gameID != nil && [gameID length] > 0, nil);
			campaign.gameID = gameID;
			
			id gameNameValue = [campaignDictionary objectForKey:kCampaignGameNameKey];
			UAAssertV(gameNameValue != nil && ([gameNameValue isKindOfClass:[NSString class]] || [gameNameValue isKindOfClass:[NSNumber class]]), nil);
			NSString *gameName = [gameNameValue isKindOfClass:[NSNumber class]] ? [gameNameValue stringValue] : gameNameValue;
			UAAssertV(gameName != nil && [gameName length] > 0, nil);
			campaign.gameName = gameName;
			
			id idValue = [campaignDictionary objectForKey:kCampaignIDKey];
			UAAssertV(idValue != nil && ([idValue isKindOfClass:[NSString class]] || [idValue isKindOfClass:[NSNumber class]]), nil);
			NSString *idString = [idValue isKindOfClass:[NSNumber class]] ? [idValue stringValue] : idValue;
			UAAssertV(idString != nil && [idString length] > 0, nil);
			campaign.id = idString;
			
			id taglineValue = [campaignDictionary objectForKey:kCampaignTaglineKey];
			UAAssertV(taglineValue != nil && ([taglineValue isKindOfClass:[NSString class]] || [taglineValue isKindOfClass:[NSNumber class]]), nil);
			NSString *tagline = [taglineValue isKindOfClass:[NSNumber class]] ? [taglineValue stringValue] : taglineValue;
			UAAssertV(tagline != nil && [tagline length] > 0, nil);
			campaign.tagline = tagline;
			
			id itunesIDValue = [campaignDictionary objectForKey:kCampaignStoreIDKey];
			UAAssertV(itunesIDValue != nil && ([itunesIDValue isKindOfClass:[NSString class]] || [itunesIDValue isKindOfClass:[NSNumber class]]), nil);
			NSString *itunesID = [itunesIDValue isKindOfClass:[NSNumber class]] ? [itunesIDValue stringValue] : itunesIDValue;
			UAAssertV(itunesID != nil && [itunesID length] > 0, nil);
			campaign.itunesID = itunesID;
			
			[campaigns addObject:campaign];
		}
		else
		{
			UALOG_DEBUG(@"Unexpected value in campaign dictionary list. %@, %@", [campaignDictionary class], campaignDictionary);
			
			continue;
		}
	}
	
	return campaigns;
}

- (id)_deserializeRewardItem:(NSDictionary *)itemDictionary
{
	UAAssertV([itemDictionary isKindOfClass:[NSDictionary class]], nil);
	
	UnityAdsRewardItem *item = [[UnityAdsRewardItem alloc] init];

	id keyValue = [itemDictionary objectForKey:kRewardItemKey];
	UAAssertV(keyValue != nil && ([keyValue isKindOfClass:[NSString class]] || [keyValue isKindOfClass:[NSNumber class]]), nil);
	NSString *key = [keyValue isKindOfClass:[NSNumber class]] ? [keyValue stringValue] : keyValue;
	UAAssertV(key != nil && [key length] > 0, nil);
	item.key = key;
	
	id nameValue = [itemDictionary objectForKey:kRewardNameKey];
	UAAssertV(nameValue != nil && ([nameValue isKindOfClass:[NSString class]] || [nameValue isKindOfClass:[NSNumber class]]), nil);
	NSString *name = [nameValue isKindOfClass:[NSNumber class]] ? [nameValue stringValue] : nameValue;
	UAAssertV(name != nil && [name length] > 0, nil);
	item.name = name;
	
	NSString *pictureURLString = [itemDictionary objectForKey:kRewardPictureKey];
	UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
	NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
	UAAssertV(pictureURL != nil, nil);
	item.pictureURL = pictureURL;
	
	return item;
}

- (void)_processCampaignDownloadData
{
	id json = [self _JSONValueFromData:self.campaignDownloadData];

	UAAssert([json isKindOfClass:[NSDictionary class]]);
	
	NSDictionary *jsonDictionary = [(NSDictionary *)json objectForKey:@"data"];
	self.campaigns = [self _deserializeCampaigns:[jsonDictionary objectForKey:@"campaigns"]];
	self.rewardItem = [self _deserializeRewardItem:[jsonDictionary objectForKey:@"item"]];
	
	NSString *gamerID = [jsonDictionary objectForKey:kGamerIDKey];
	UAAssert(gamerID != nil);
	self.gamerID = gamerID;
	
	[self.cache cacheCampaigns:self.campaigns];
}

#pragma mark - Public

- (id)init
{
	UAAssertV( ! [NSThread isMainThread], nil);
	
	if ((self = [super init]))
	{
		_cache = [[UnityAdsCache alloc] init];
		_cache.delegate = self;
	}
	
	return self;
}

- (void)updateCampaigns
{
	UAAssert( ! [NSThread isMainThread]);
	
	NSString *urlString = kUnityAdsBackendURL;
	if (self.queryString != nil)
		urlString = [urlString stringByAppendingString:self.queryString];
	
	NSURLRequest *request = [[NSURLRequest alloc] initWithURL:[NSURL URLWithString:urlString]];
	self.urlConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	[self.urlConnection start];
}

- (NSURL *)videoURLForCampaign:(UnityAdsCampaign *)campaign
{
	@synchronized (self)
	{
		if (campaign == nil)
		{
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
		
		NSURL *videoURL = [self.cache localVideoURLForCampaign:campaign];
		if (videoURL == nil)
			videoURL = campaign.trailerStreamingURL;

		return videoURL;
	}
}

- (void)cancelAllDownloads
{
	UAAssert( ! [NSThread isMainThread]);
	
	[self.urlConnection cancel];
	self.urlConnection = nil;
	
	[self.cache cancelAllDownloads];
}

- (void)dealloc
{
	self.cache.delegate = nil;
}

#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
	self.campaignDownloadData = [NSMutableData data];
}

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
	self.campaignDownloadData = nil;
	self.urlConnection = nil;
	
	NSInteger errorCode = [error code];
	if (errorCode != NSURLErrorNotConnectedToInternet &&
		errorCode != NSURLErrorCannotFindHost &&
		errorCode != NSURLErrorCannotConnectToHost &&
		errorCode != NSURLErrorResourceUnavailable &&
		errorCode != NSURLErrorFileDoesNotExist &&
		errorCode != NSURLErrorNoPermissionsToReadFile)
	{
		UALOG_DEBUG(@"Retrying campaign download.");
		[self updateCampaigns];
	}
	else
		UALOG_DEBUG(@"Not retrying campaign download.");
}

#pragma mark - UnityAdsCacheDelegate

- (void)cache:(UnityAdsCache *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign
{
}

- (void)cacheFinishedCachingCampaigns:(UnityAdsCache *)cache
{
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.delegate campaignManager:self updatedWithCampaigns:self.campaigns rewardItem:self.rewardItem gamerID:self.gamerID];
	});
}

@end
