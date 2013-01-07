//
//  UnityAdsCampaignManager.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCampaignManager.h"
#import "../UnityAdsSBJSON/UnityAdsSBJsonParser.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"
#import "../UnityAdsData/UnityAdsCache.h"
#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"
#import "../UnityAdsSBJSON/NSObject+UnityAdsSBJson.h"

NSString * const kCampaignEndScreenKey = @"endScreen";
NSString * const kCampaignClickURLKey = @"clickUrl";
NSString * const kCampaignPictureKey = @"picture";
NSString * const kCampaignTrailerDownloadableKey = @"trailerDownloadable";
NSString * const kCampaignTrailerStreamingKey = @"trailerStreaming";
NSString * const kCampaignGameIDKey = @"gameId";
NSString * const kCampaignGameNameKey = @"gameName";
NSString * const kCampaignIDKey = @"id";
NSString * const kCampaignTaglineKey = @"tagLine";
NSString * const kCampaignStoreIDKey = @"iTunesId";

NSString * const kRewardItemKey = @"itemKey";
NSString * const kRewardNameKey = @"name";
NSString * const kRewardPictureKey = @"picture";

NSString * const kGamerIDKey = @"gamerId";

@interface UnityAdsCampaignManager () <NSURLConnectionDelegate, UnityAdsCacheDelegate>
@property (nonatomic, strong) NSURLConnection *urlConnection;
@property (nonatomic, strong) NSMutableData *campaignDownloadData;
@property (nonatomic, strong) UnityAdsCache *cache;
@property (nonatomic, assign) dispatch_queue_t testQueue;
@end

@implementation UnityAdsCampaignManager

static UnityAdsCampaignManager *sharedUnityAdsInstanceCampaignManager = nil;

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedUnityAdsInstanceCampaignManager == nil)
      sharedUnityAdsInstanceCampaignManager = [[UnityAdsCampaignManager alloc] init];
	}
	
	return sharedUnityAdsInstanceCampaignManager;
}


#pragma mark - Private

- (void)_campaignDataReceived {
  [self _processCampaignDownloadData];
}

- (NSArray *)_deserializeCampaigns:(NSArray *)campaignArray {
	if (campaignArray == nil || [campaignArray count] == 0) {
		UALOG_DEBUG(@"Input empty or nil.");
		return nil;
	}
	
	NSMutableArray *campaigns = [NSMutableArray array];
	
	for (id campaignDictionary in campaignArray) {
		if ([campaignDictionary isKindOfClass:[NSDictionary class]]) {
			UnityAdsCampaign *campaign = [[UnityAdsCampaign alloc] init];
      campaign.viewed = NO;
			
			NSString *endScreenURLString = [campaignDictionary objectForKey:kCampaignEndScreenKey];
      if (endScreenURLString == nil) continue;
      UAAssertV([endScreenURLString isKindOfClass:[NSString class]], nil);
			NSURL *endScreenURL = [NSURL URLWithString:endScreenURLString];
			UAAssertV(endScreenURL != nil, nil);
			campaign.endScreenURL = endScreenURL;
			
			NSString *clickURLString = [campaignDictionary objectForKey:kCampaignClickURLKey];
      if (clickURLString == nil) continue;
			UAAssertV([clickURLString isKindOfClass:[NSString class]], nil);
			NSURL *clickURL = [NSURL URLWithString:clickURLString];
			UAAssertV(clickURL != nil, nil);
			campaign.clickURL = clickURL;
			
			NSString *pictureURLString = [campaignDictionary objectForKey:kCampaignPictureKey];
      if (pictureURLString == nil) continue;
			UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
			NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
			UAAssertV(pictureURL != nil, nil);
			campaign.pictureURL = pictureURL;
			
			NSString *trailerDownloadableURLString = [campaignDictionary objectForKey:kCampaignTrailerDownloadableKey];
      if (trailerDownloadableURLString == nil) continue;
			UAAssertV([trailerDownloadableURLString isKindOfClass:[NSString class]], nil);
			NSURL *trailerDownloadableURL = [NSURL URLWithString:trailerDownloadableURLString];
			UAAssertV(trailerDownloadableURL != nil, nil);
			campaign.trailerDownloadableURL = trailerDownloadableURL;
			
			NSString *trailerStreamingURLString = [campaignDictionary objectForKey:kCampaignTrailerStreamingKey];
      if (trailerStreamingURLString == nil) continue;
			UAAssertV([trailerStreamingURLString isKindOfClass:[NSString class]], nil);
			NSURL *trailerStreamingURL = [NSURL URLWithString:trailerStreamingURLString];
			UAAssertV(trailerStreamingURL != nil, nil);
			campaign.trailerStreamingURL = trailerStreamingURL;
			
			id gameIDValue = [campaignDictionary objectForKey:kCampaignGameIDKey];
      if (gameIDValue == nil) continue;
			UAAssertV(gameIDValue != nil && ([gameIDValue isKindOfClass:[NSString class]] || [gameIDValue isKindOfClass:[NSNumber class]]), nil);
			NSString *gameID = [gameIDValue isKindOfClass:[NSNumber class]] ? [gameIDValue stringValue] : gameIDValue;
			UAAssertV(gameID != nil && [gameID length] > 0, nil);
			campaign.gameID = gameID;
			
			id gameNameValue = [campaignDictionary objectForKey:kCampaignGameNameKey];
      if (gameNameValue == nil) continue;
			UAAssertV(gameNameValue != nil && ([gameNameValue isKindOfClass:[NSString class]] || [gameNameValue isKindOfClass:[NSNumber class]]), nil);
			NSString *gameName = [gameNameValue isKindOfClass:[NSNumber class]] ? [gameNameValue stringValue] : gameNameValue;
			UAAssertV(gameName != nil && [gameName length] > 0, nil);
			campaign.gameName = gameName;
			
			id idValue = [campaignDictionary objectForKey:kCampaignIDKey];
      if (idValue == nil) continue;
			UAAssertV(idValue != nil && ([idValue isKindOfClass:[NSString class]] || [idValue isKindOfClass:[NSNumber class]]), nil);
			NSString *idString = [idValue isKindOfClass:[NSNumber class]] ? [idValue stringValue] : idValue;
			UAAssertV(idString != nil && [idString length] > 0, nil);
			campaign.id = idString;
			
			id tagLineValue = [campaignDictionary objectForKey:kCampaignTaglineKey];
      if (tagLineValue == nil) continue;
			UAAssertV(tagLineValue != nil && ([tagLineValue isKindOfClass:[NSString class]] || [tagLineValue isKindOfClass:[NSNumber class]]), nil);
			NSString *tagline = [tagLineValue isKindOfClass:[NSNumber class]] ? [tagLineValue stringValue] : tagLineValue;
			UAAssertV(tagline != nil && [tagline length] > 0, nil);
			campaign.tagLine = tagline;
			
			id itunesIDValue = [campaignDictionary objectForKey:kCampaignStoreIDKey];
      if (itunesIDValue == nil) continue;
			UAAssertV(itunesIDValue != nil && ([itunesIDValue isKindOfClass:[NSString class]] || [itunesIDValue isKindOfClass:[NSNumber class]]), nil);
			NSString *itunesID = [itunesIDValue isKindOfClass:[NSNumber class]] ? [itunesIDValue stringValue] : itunesIDValue;
			UAAssertV(itunesID != nil && [itunesID length] > 0, nil);
			campaign.itunesID = itunesID;
			
      campaign.shouldCacheVideo = NO;
      if ([campaignDictionary objectForKey:@"cacheVideo"] != nil) {
        if ([[campaignDictionary valueForKey:@"cacheVideo"] boolValue] != 0) {
          campaign.shouldCacheVideo = YES;
        }
      }
      
			[campaigns addObject:campaign];
		}
		else {
			UALOG_DEBUG(@"Unexpected value in campaign dictionary list. %@, %@", [campaignDictionary class], campaignDictionary);
			continue;
		}
	}
	
	return campaigns;
}

- (id)_deserializeRewardItem:(NSDictionary *)itemDictionary {
	UAAssertV([itemDictionary isKindOfClass:[NSDictionary class]], nil);
	
	UnityAdsRewardItem *item = [[UnityAdsRewardItem alloc] init];
  
	id keyValue = [itemDictionary objectForKey:kRewardItemKey];
  if (keyValue == nil) return nil;
	UAAssertV(keyValue != nil && ([keyValue isKindOfClass:[NSString class]] || [keyValue isKindOfClass:[NSNumber class]]), nil);
	NSString *key = [keyValue isKindOfClass:[NSNumber class]] ? [keyValue stringValue] : keyValue;
	UAAssertV(key != nil && [key length] > 0, nil);
  if (key == nil || [key length] == 0) return nil;
	item.key = key;
	
	id nameValue = [itemDictionary objectForKey:kRewardNameKey];
  if (nameValue == nil) return nil;
	UAAssertV(nameValue != nil && ([nameValue isKindOfClass:[NSString class]] || [nameValue isKindOfClass:[NSNumber class]]), nil);
	NSString *name = [nameValue isKindOfClass:[NSNumber class]] ? [nameValue stringValue] : nameValue;
	UAAssertV(name != nil && [name length] > 0, nil);
  if (name == nil || [name length] == 0) return nil;
	item.name = name;
	
	NSString *pictureURLString = [itemDictionary objectForKey:kRewardPictureKey];
  if (pictureURLString == nil) return nil;
	UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
	NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
	UAAssertV(pictureURL != nil, nil);
  if (pictureURL == nil) return nil;
	item.pictureURL = pictureURL;
	
	return item;
}

- (void)_processCampaignDownloadData {

  NSString *jsonString = [[NSString alloc] initWithData:self.campaignDownloadData encoding:NSUTF8StringEncoding];
  _campaignData = [jsonString JSONValue];
  
  UALOG_DEBUG(@"%@", [_campaignData JSONRepresentation]);
	UAAssert([_campaignData isKindOfClass:[NSDictionary class]]);
	
  if (_campaignData != nil && [_campaignData isKindOfClass:[NSDictionary class]]) {
    NSDictionary *jsonDictionary = [(NSDictionary *)_campaignData objectForKey:@"data"];
    BOOL validData = YES;
    
    if ([jsonDictionary objectForKey:@"webViewUrl"] == nil) validData = NO;
    if ([jsonDictionary objectForKey:@"analyticsUrl"] == nil) validData = NO;
    if ([jsonDictionary objectForKey:@"impactUrl"] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kGamerIDKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:@"campaigns"] == nil) validData = NO;
    if ([jsonDictionary objectForKey:@"item"] == nil) validData = NO;
    
    self.campaigns = [self _deserializeCampaigns:[jsonDictionary objectForKey:@"campaigns"]];
    if (self.campaigns == nil || [self.campaigns count] == 0) validData = NO;
    
    self.defaultRewardItem = [self _deserializeRewardItem:[jsonDictionary objectForKey:@"item"]];
    if (self.defaultRewardItem == nil) validData = NO;
    
    if ([jsonDictionary objectForKey:@"items"] != nil) {
      NSArray *rewardItems = [jsonDictionary objectForKey:@"items"];
      UALOG_DEBUG(@"Found multiple rewards: %i", [rewardItems count]);
      NSMutableArray *deserializedRewardItems = [NSMutableArray array];
      NSMutableArray *tempRewardItemKeys = [NSMutableArray array];
      UnityAdsRewardItem *rewardItem = nil;
      
      for (NSDictionary *itemData in rewardItems) {
        UALOG_DEBUG(@"%@", itemData);
        rewardItem = [self _deserializeRewardItem:itemData];
        if (rewardItem != nil) {
          [deserializedRewardItems addObject:rewardItem];
          [tempRewardItemKeys addObject:rewardItem.key];
        }
      }
      
      self.rewardItems = [[NSMutableArray alloc] initWithArray:deserializedRewardItems];
      self.rewardItemKeys = [[NSArray alloc] initWithArray:tempRewardItemKeys];
      UALOG_DEBUG(@"Parsed total of %i reward items, with keys: %@", [self.rewardItems count], self.rewardItemKeys);
    }

    if (validData) {
      self.currentRewardItemKey = self.defaultRewardItem.key;
      
      [[UnityAdsProperties sharedInstance] setWebViewBaseUrl:(NSString *)[jsonDictionary objectForKey:@"webViewUrl"]];
      [[UnityAdsProperties sharedInstance] setAnalyticsBaseUrl:(NSString *)[jsonDictionary objectForKey:@"analyticsUrl"]];
      [[UnityAdsProperties sharedInstance] setAdsBaseUrl:(NSString *)[jsonDictionary objectForKey:@"impactUrl"]];
      
      NSString *gamerId = [jsonDictionary objectForKey:kGamerIDKey];
      
      [[UnityAdsProperties sharedInstance] setGamerId:gamerId];
      [self.cache cacheCampaigns:self.campaigns];
      
      dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self.delegate campaignManagerCampaignDataReceived];
      });
    }
    else {
      dispatch_async(dispatch_get_main_queue(), ^(void) {
        [self.delegate campaignManagerCampaignDataFailed];
      });
    }
  }
}


#pragma mark - Public

- (id)init {
	UAAssertV(![NSThread isMainThread], nil);
	
	if ((self = [super init])) {
		_cache = [[UnityAdsCache alloc] init];
		_cache.delegate = self;
	}
	
	return self;
}

- (void)updateCampaigns {
	UAAssert(![NSThread isMainThread]);
	
	NSString *urlString = [[UnityAdsProperties sharedInstance] campaignDataUrl];
	
  if ([[UnityAdsProperties sharedInstance] campaignQueryString] != nil)
		urlString = [urlString stringByAppendingString:[[UnityAdsProperties sharedInstance] campaignQueryString]];
  
  UALOG_DEBUG(@"UrlString %@", urlString);
	NSURLRequest *request = [[NSURLRequest alloc] initWithURL:[NSURL URLWithString:urlString]];
	self.urlConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	[self.urlConnection start];
}

- (NSURL *)getVideoURLForCampaign:(UnityAdsCampaign *)campaign {
	@synchronized (self) {
		if (campaign == nil) {
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
		
		NSURL *videoURL = [self.cache localVideoURLForCampaign:campaign];
		if (videoURL == nil || [self.cache campaignExistsInQueue:campaign] || ![campaign shouldCacheVideo]) {
      UALOG_DEBUG(@"Campaign is not cached!");
      videoURL = campaign.trailerStreamingURL;
    }
    
    UALOG_DEBUG(@"%@ and %i", videoURL.absoluteString, [self.cache campaignExistsInQueue:campaign]);
    
		return videoURL;
	}
}

- (UnityAdsCampaign *)getCampaignWithId:(NSString *)campaignId {
	UALOG_DEBUG(@"");
	UAAssertV([NSThread isMainThread], nil);
	UnityAdsCampaign *foundCampaign = nil;
	
	for (UnityAdsCampaign *campaign in self.campaigns) {
		if ([campaign.id isEqualToString:campaignId]) {
			foundCampaign = campaign;
			break;
		}
	}
	
	return foundCampaign;
}

- (NSArray *)getViewableCampaigns {
	UALOG_DEBUG(@"");
  NSMutableArray *retAr = [[NSMutableArray alloc] init];
  
  if (self.campaigns != nil) {
    for (UnityAdsCampaign* campaign in self.campaigns) {
      if (!campaign.viewed) {
        [retAr addObject:campaign];
      }
    }
  }
  
  return retAr;
}

- (BOOL)setSelectedRewardItemKey:(NSString *)rewardItemKey {
  if (self.rewardItems != nil && [self.rewardItems count] > 0) {
    for (UnityAdsRewardItem *rewardItem in self.rewardItems) {
      if ([rewardItem.key isEqualToString:rewardItemKey]) {
        self.currentRewardItemKey = rewardItemKey;
        return YES;
      }
    }
  }
  
  return NO;
}

- (UnityAdsRewardItem *)getCurrentRewardItem {
  if (self.currentRewardItemKey != nil) {
    for (UnityAdsRewardItem *rewardItem in self.rewardItems) {
      if ([rewardItem.key isEqualToString:self.currentRewardItemKey]) {
        return rewardItem;
      }
    }
  }
  
  return nil;
}

- (void)cancelAllDownloads {
	UAAssert(![NSThread isMainThread]);
	
	[self.urlConnection cancel];
	self.urlConnection = nil;
	
	[self.cache cancelAllDownloads];
}

- (void)dealloc {
	self.cache.delegate = nil;
}


#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
  self.campaignDownloadData = [NSMutableData data];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
	[self.campaignDownloadData appendData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
  [self _campaignDataReceived];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
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
	else {
		UALOG_DEBUG(@"Not retrying campaign download.");
    [self.delegate campaignManagerCampaignDataFailed];
  }
}


#pragma mark - UnityAdsCacheDelegate

- (void)cache:(UnityAdsCache *)cache finishedCachingCampaign:(UnityAdsCampaign *)campaign {
}

- (void)cacheFinishedCachingCampaigns:(UnityAdsCache *)cache {
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.delegate campaignManager:self updatedWithCampaigns:self.campaigns rewardItem:self.defaultRewardItem gamerID:[[UnityAdsProperties sharedInstance] gamerId]];
	});
}

@end
