//
//  UnityAdsCampaignManager.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsCampaignManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"
#import "UnityAds.h"
#import "UnityAdsSBJsonParser.h"
#import "UnityAdsCacheManager.h"
#import "NSObject+UnityAdsSBJson.h"
#import "UnityAdsProperties.h"
#import "UnityAdsConstants.h"
#import "UnityAdsZoneParser.h"
#import "UnityAdsZoneManager.h"

@interface UnityAdsCampaignManager () <NSURLConnectionDelegate, UnityAdsCacheDelegate>
@property (nonatomic, strong) NSURLConnection *urlConnection;
@property (nonatomic, strong) NSMutableData *campaignDownloadData;
@property (nonatomic, strong) UnityAdsCacheManager *cacheManager;
@end

@implementation UnityAdsCampaignManager

@synthesize campaignDownloadData = _campaignDownloadData;

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
    if ([jsonDictionary objectForKey:kUnityAdsZonesRootKey] == nil) validData = NO;
    
    id zoneManager = [UnityAdsZoneManager sharedInstance];
    [zoneManager clearZones];
    int addedZones = [zoneManager addZones:[UnityAdsZoneParser parseZones:[jsonDictionary objectForKey:kUnityAdsZonesRootKey]]];
    if(addedZones == 0) validData = NO;
    
    self.campaigns = [self deserializeCampaigns:[jsonDictionary objectForKey:kUnityAdsCampaignsKey]];
    if (self.campaigns == nil || [self.campaigns count] == 0) validData = NO;
    
    if (validData) {
      [[UnityAdsProperties sharedInstance] setWebViewBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsWebViewUrlKey]];
      [[UnityAdsProperties sharedInstance] setAnalyticsBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsAnalyticsUrlKey]];
      [[UnityAdsProperties sharedInstance] setAdsBaseUrl:(NSString *)[jsonDictionary objectForKey:kUnityAdsUrlKey]];
      
      if ([jsonDictionary objectForKey:kUnityAdsSdkVersionKey] != nil &&
          [[jsonDictionary objectForKey:kUnityAdsSdkVersionKey] isKindOfClass:[NSString class]]) {
        [[UnityAdsProperties sharedInstance] setExpectedSdkVersion:[jsonDictionary objectForKey:kUnityAdsSdkVersionKey]];
        UALOG_DEBUG(@"Got SDK Version: %@", [[UnityAdsProperties sharedInstance] expectedSdkVersion]);
      }
      
      if ([jsonDictionary objectForKey:kUnityAdsWebViewDataParamSdkIsCurrentKey] != nil) {
        [[UnityAdsProperties sharedInstance] setSdkIsCurrent:[[jsonDictionary objectForKey:kUnityAdsWebViewDataParamSdkIsCurrentKey] boolValue]];
      }

      NSString *gamerId = [jsonDictionary objectForKey:kUnityAdsGamerIDKey];
      
      [[UnityAdsProperties sharedInstance] setGamerId:gamerId];
//      [self.cacheManager cacheCampaigns:self.campaigns];
      
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
  
  self.campaignDownloadData = nil;
}


#pragma mark - Public

- (id)init {
	UAAssertV(![NSThread isMainThread], nil);
	
	if ((self = [super init])) {
		_cacheManager = [[UnityAdsCacheManager alloc] init];
		_cacheManager.delegate = self;
	}
	
	return self;
}

- (void)updateCampaigns {
	UAAssert(![NSThread isMainThread]);
	
	NSString *urlString = [[UnityAdsProperties sharedInstance] campaignDataUrl];
	
  if ([[UnityAdsProperties sharedInstance] campaignQueryString] != nil)
		urlString = [urlString stringByAppendingString:[[UnityAdsProperties sharedInstance] campaignQueryString]];
  
  UALOG_DEBUG(@"UrlString %@", urlString);
	NSURLRequest *request = [[NSURLRequest alloc] initWithURL:[NSURL URLWithString:urlString] cachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData timeoutInterval:60];
	self.urlConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	[self.urlConnection start];
}

- (NSURL *)getVideoURLForCampaign:(UnityAdsCampaign *)campaign {
	@synchronized (self) {
		if (campaign == nil) {
			UALOG_DEBUG(@"Input is nil.");
			return nil;
		}
		
		NSURL *videoURL = [self.cacheManager localVideoURLForCampaign:campaign];
		if (videoURL == nil || [self.cacheManager campaignExistsInQueue:campaign] || ![campaign shouldCacheVideo] || ![self.cacheManager isCampaignVideoCached:campaign]) {
      UALOG_DEBUG(@"Campaign is not cached!");
      videoURL = campaign.trailerStreamingURL;
    }
    
    UALOG_DEBUG(@"%@ and %i", videoURL.absoluteString, [self.cacheManager campaignExistsInQueue:campaign]);
    
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

- (void)cancelAllDownloads {
	UAAssert(![NSThread isMainThread]);
	
	[self.urlConnection cancel];
	self.urlConnection = nil;
	
	[self.cacheManager cancelAllDownloads];
}

- (void)dealloc {
	self.cacheManager.delegate = nil;
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

static int retryCount = 0;
- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
	self.campaignDownloadData = nil;
	self.urlConnection = nil;
	
	if(retryCount < kUnityAdsWebDataMaxRetryCount) {
    ++retryCount;
    UALOG_DEBUG(@"Retrying campaign download in %d seconds.", kUnityAdsWebDataRetryInterval);
    [NSTimer scheduledTimerWithTimeInterval:kUnityAdsWebDataRetryInterval target:self selector:@selector(updateCampaigns) userInfo:nil repeats:NO];
  } else {
    UALOG_DEBUG(@"Not retrying campaign download.");
    dispatch_async(dispatch_get_main_queue(), ^{
      [self.delegate campaignManagerCampaignDataFailed];
    });
  }
}


#pragma mark - UnityAdsCacheDelegate

- (void)cache:(UnityAdsCacheManager *)cacheManager finishedCachingCampaign:(UnityAdsCampaign *)campaign {
  dispatch_async(dispatch_get_main_queue(), ^{
		[self.delegate campaignManager:self updatedWithCampaigns:self.campaigns gamerID:[[UnityAdsProperties sharedInstance] gamerId]];
	});
}

@end
