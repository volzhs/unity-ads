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
NSString * const kCampaignStoreIDKey = @"itunesID";

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
			
			NSURL *endScreenURL = [NSURL URLWithString:[campaignDictionary objectForKey:kCampaignEndScreenKey]];
			UAAssertV(endScreenURL != nil, nil);
			campaign.endScreenURL = endScreenURL;
			
			NSURL *clickURL = [NSURL URLWithString:[campaignDictionary objectForKey:kCampaignClickURLKey]];
			UAAssertV(clickURL != nil, nil);
			campaign.clickURL = clickURL;
			
			NSURL *pictureURL = [NSURL URLWithString:[campaignDictionary objectForKey:kCampaignPictureKey]];
			UAAssertV(pictureURL != nil, nil);
			campaign.pictureURL = pictureURL;
			
			NSURL *trailerDownloadableURL = [NSURL URLWithString:[campaignDictionary objectForKey:kCampaignTrailerDownloadableKey]];
			UAAssertV(trailerDownloadableURL != nil, nil);
			campaign.trailerDownloadableURL = trailerDownloadableURL;
			
			NSURL *trailerStreamingURL = [NSURL URLWithString:[campaignDictionary objectForKey:kCampaignTrailerStreamingKey]];
			UAAssertV(trailerStreamingURL != nil, nil);
			campaign.trailerStreamingURL = trailerStreamingURL;
			
			NSString *gameID = [NSString stringWithFormat:@"%@", [campaignDictionary objectForKey:kCampaignGameIDKey]];
			UAAssertV(gameID != nil && [gameID length] > 0, nil);
			campaign.gameID = gameID;
			
			NSString *gameName = [NSString stringWithFormat:@"%@", [campaignDictionary objectForKey:kCampaignGameNameKey]];
			UAAssertV(gameName != nil && [gameName length] > 0, nil);
			campaign.gameName = gameName;
			
			NSString *id = [NSString stringWithFormat:@"%@", [campaignDictionary objectForKey:kCampaignIDKey]];
			UAAssertV(id != nil && [id length] > 0, nil);
			campaign.id = id;
			
			NSString *tagline = [NSString stringWithFormat:@"%@", [campaignDictionary objectForKey:kCampaignTaglineKey]];
			UAAssertV(tagline != nil && [tagline length] > 0, nil);
			campaign.tagline = tagline;
			
			NSString *itunesID = [NSString stringWithFormat:@"%@", [campaignDictionary objectForKey:kCampaignStoreIDKey]];
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
	NSString *key = [NSString stringWithFormat:@"%@", [itemDictionary objectForKey:kRewardItemKey]];
	UAAssertV(key != nil && [key length] > 0, nil);
	item.key = key;
	
	NSString *name = [NSString stringWithFormat:@"%@", [itemDictionary objectForKey:kRewardNameKey]];
	UAAssertV(name != nil && [name length] > 0, nil);
	item.name = name;
	
	NSURL *pictureURL = [NSURL URLWithString:[itemDictionary objectForKey:kRewardPictureKey]];
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
	UALOG_DEBUG(@"didFailWithError: %@", error);
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
