//
//  UnityAdsCampaignManager.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCampaignManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"
#import "../UnityAds.h"
#import "../UnityAdsSBJSON/UnityAdsSBJsonParser.h"
#import "../UnityAdsData/UnityAdsCache.h"
#import "../UnityAdsSBJSON/NSObject+UnityAdsSBJson.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"

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

- (NSArray *)deserializeCampaigns:(NSArray *)campaignArray {
	if (campaignArray == nil || [campaignArray count] == 0) {
		UALOG_DEBUG(@"Input empty or nil.");
		return nil;
	}
	
	NSMutableArray *campaigns = [NSMutableArray array];
	
	for (id campaignDictionary in campaignArray) {
		if ([campaignDictionary isKindOfClass:[NSDictionary class]]) {
			UnityAdsCampaign *campaign = [[UnityAdsCampaign alloc] initWithData:campaignDictionary];
      
      if (campaign.isValidCampaign) {
        [campaigns addObject:campaign];
      }
		}
		else {
			UALOG_DEBUG(@"Unexpected value in campaign dictionary list. %@, %@", [campaignDictionary class], campaignDictionary);
			continue;
		}
	}
	
	return campaigns;
}

- (NSArray *)deserializeRewardItems:(NSArray *)rewardItemsArray {
  if (rewardItemsArray == nil || [rewardItemsArray count] == 0) {
		UALOG_DEBUG(@"Input empty or nil.");
		return nil;
	}
  
  NSMutableArray *deserializedRewardItems = [NSMutableArray array];
  UnityAdsRewardItem *rewardItem = nil;
  
  for (NSDictionary *rewardItemData in rewardItemsArray) {
    rewardItem = [self deserializeRewardItem:rewardItemData];
    if (rewardItem != nil) {
      [deserializedRewardItems addObject:rewardItem];
    }
  }
  
  if (deserializedRewardItems != nil && [deserializedRewardItems count] > 0) {
    return [[NSArray alloc] initWithArray:deserializedRewardItems];
  }
  
  return nil;
}

- (id)deserializeRewardItem:(NSDictionary *)itemDictionary {
	UAAssertV([itemDictionary isKindOfClass:[NSDictionary class]], nil);
	
	UnityAdsRewardItem *item = [[UnityAdsRewardItem alloc] initWithData:itemDictionary];
  
  if (item.isValidRewardItem) {
    return item;
  }
  
  return nil;
}

- (NSArray *)createRewardItemKeyMap:(NSArray *)rewardItemsArray {
  if (self.rewardItems != nil && [self.rewardItems count] > 0) {
    NSMutableArray *tempRewardItemKeys = [NSMutableArray array];
    
    for (UnityAdsRewardItem *rewardItem in rewardItemsArray) {
      if (rewardItem.isValidRewardItem) {
        [tempRewardItemKeys addObject:rewardItem.key];
      }
    }
    
    if (tempRewardItemKeys != nil && [tempRewardItemKeys count] > 0) {
      return [[NSArray alloc] initWithArray:tempRewardItemKeys];
    }
  }
  
  return nil;
}

- (void)_processCampaignDownloadData {

  if (self.campaignDownloadData == nil) {
    dispatch_async(dispatch_get_main_queue(), ^(void) {
      [self.delegate campaignManagerCampaignDataFailed];
    });
    UALOG_DEBUG(@"Campaign download data is NULL!");
    return;
  }
  
  NSString *jsonString = [[NSString alloc] initWithData:self.campaignDownloadData encoding:NSUTF8StringEncoding];
  _campaignData = [jsonString JSONValue];
  
  if (_campaignData == nil) {
    dispatch_async(dispatch_get_main_queue(), ^(void) {
      [self.delegate campaignManagerCampaignDataFailed];
    });
    UALOG_DEBUG(@"Campaigndata is NULL!");
    return;
  }
  
  UALOG_DEBUG(@"%@", [_campaignData JSONRepresentation]);
	UAAssert([_campaignData isKindOfClass:[NSDictionary class]]);
	
  if (_campaignData != nil && [_campaignData isKindOfClass:[NSDictionary class]]) {
    NSDictionary *jsonDictionary = [(NSDictionary *)_campaignData objectForKey:kUnityAdsJsonDataRootKey];
    BOOL validData = YES;
    
    if ([jsonDictionary objectForKey:kUnityAdsWebViewUrlKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kUnityAdsAnalyticsUrlKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kUnityAdsUrlKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kUnityAdsGamerIDKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kUnityAdsCampaignsKey] == nil) validData = NO;
    if ([jsonDictionary objectForKey:kUnityAdsRewardItemKey] == nil) validData = NO;
    
    if ([jsonDictionary objectForKey:kUnityAdsCampaignAllowVideoSkipKey] != nil) {
      [[UnityAdsProperties sharedInstance] setAllowVideoSkipInSeconds:[[jsonDictionary objectForKey:kUnityAdsCampaignAllowVideoSkipKey] intValue]];
      UALOG_DEBUG(@"ALLOW_VIDEO_SKIP: %i", [UnityAdsProperties sharedInstance].allowVideoSkipInSeconds);
    }
    
    self.campaigns = [self deserializeCampaigns:[jsonDictionary objectForKey:kUnityAdsCampaignsKey]];
    if (self.campaigns == nil || [self.campaigns count] == 0) validData = NO;
    
    self.defaultRewardItem = [self deserializeRewardItem:[jsonDictionary objectForKey:kUnityAdsRewardItemKey]];
    if (self.defaultRewardItem == nil) validData = NO;
    
    if ([jsonDictionary objectForKey:kUnityAdsRewardItemsKey] != nil) {
      NSArray *rewardItems = [jsonDictionary objectForKey:kUnityAdsRewardItemsKey];
      NSArray *deserializedRewardItems = [self deserializeRewardItems:rewardItems];
      
      if (deserializedRewardItems != nil) {
        self.rewardItems = [[NSMutableArray alloc] initWithArray:deserializedRewardItems];
      }
      
      if (self.rewardItems != nil && [self.rewardItems count] > 0) {
        self.rewardItemKeys = [self createRewardItemKeyMap:self.rewardItems];
      }

      UALOG_DEBUG(@"Parsed total of %i reward items, with keys: %@", [self.rewardItems count], self.rewardItemKeys);
    }

    if (validData) {
      self.currentRewardItemKey = self.defaultRewardItem.key;
      
      [[UnityAdsProperties sharedInstance] setWebViewBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsWebViewUrlKey]];
      [[UnityAdsProperties sharedInstance] setAnalyticsBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsAnalyticsUrlKey]];
      [[UnityAdsProperties sharedInstance] setAdsBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsUrlKey]];
      
      if ([jsonDictionary objectForKey:kUnityAdsSdkVersionKey] != nil) {
        [[UnityAdsProperties sharedInstance] setExpectedSdkVersion:[jsonDictionary objectForKey:kUnityAdsSdkVersionKey]];
        UALOG_DEBUG(@"Got SDK Version: %@", [[UnityAdsProperties sharedInstance] expectedSdkVersion]);
      }
      
      NSString *gamerId = [jsonDictionary objectForKey:kUnityAdsGamerIDKey];
      
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
		if (videoURL == nil || [self.cache campaignExistsInQueue:campaign] || ![campaign shouldCacheVideo] || ![self.cache isCampaignVideoCached:campaign]) {
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

- (UnityAdsCampaign *)getCampaignWithITunesId:(NSString *)iTunesId {
	UALOG_DEBUG(@"");
	UAAssertV([NSThread isMainThread], nil);
	UnityAdsCampaign *foundCampaign = nil;
	
	for (UnityAdsCampaign *campaign in self.campaigns) {
		if ([campaign.itunesID isEqualToString:iTunesId]) {
			foundCampaign = campaign;
			break;
		}
	}
	
	return foundCampaign;
}

- (UnityAdsCampaign *)getCampaignWithClickUrl:(NSString *)clickUrl {
	UALOG_DEBUG(@"");
	UAAssertV([NSThread isMainThread], nil);
	UnityAdsCampaign *foundCampaign = nil;
	
	for (UnityAdsCampaign *campaign in self.campaigns) {
		if ([[campaign.clickURL absoluteString] isEqualToString:clickUrl]) {
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
    if (self.rewardItems != nil) {
      for (UnityAdsRewardItem *rewardItem in self.rewardItems) {
        if ([rewardItem.key isEqualToString:self.currentRewardItemKey]) {
          return rewardItem;
        }
      }
    }
    else {
      return self.defaultRewardItem;
    }
  }
  
  return nil;
}

- (NSDictionary *)getPublicRewardItemDetails:(NSString *)rewardItemKey {
  if (rewardItemKey != nil) {
    for (UnityAdsRewardItem *rewardItem in self.rewardItems) {
      if ([rewardItem.key isEqualToString:rewardItemKey]) {
        NSDictionary *retDict = @{kUnityAdsRewardItemNameKey:rewardItem.name, kUnityAdsRewardItemPictureKey:rewardItem.pictureURL};
        return retDict;
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
    dispatch_async(dispatch_get_main_queue(), ^{
      [self.delegate campaignManagerCampaignDataFailed];
    });
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
