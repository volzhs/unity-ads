//
//  UnityAdsAnalyticsUploader.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsAnalyticsUploader.h"
#import "../UnityAds.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"

#import "../UnityAdsZone/UnityAdsZoneManager.h"
#import "../UnityAdsZone/UnityAdsIncentivizedZone.h"

@interface UnityAdsAnalyticsUploader () <NSURLConnectionDelegate>
@property (nonatomic, strong) NSMutableArray *uploadQueue;
@property (nonatomic, strong) NSDictionary *currentUpload;
@property (nonatomic, assign) dispatch_queue_t analyticsQueue;
@property (nonatomic, strong) NSThread *backgroundThread;
@end

@implementation UnityAdsAnalyticsUploader


#pragma mark - Private

- (void)_backgroundRunLoop:(id)dummy {
	@autoreleasepool {
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while([[NSThread currentThread] isCancelled] == NO) {
			@autoreleasepool {
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:3.0]];
			}
		}
	}
}


#pragma mark - Upload queing

- (BOOL)_startNextUpload {
	if (self.currentUpload != nil || [self.uploadQueue count] == 0)
		return NO;
	
	self.currentUpload = [self.uploadQueue objectAtIndex:0];
	
	NSURLConnection *connection = [self.currentUpload objectForKey:kUnityAdsAnalyticsUploaderConnectionKey];
	[connection start];
	
	[self.uploadQueue removeObjectAtIndex:0];
	
	return YES;
}

- (void)_queueURL:(NSURL *)url body:(NSData *)body httpMethod:(NSString *)httpMethod retries:(NSNumber *)retryCount {
	if (url == nil) {
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
	
	NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:url];

	if (request == nil) {
		UALOG_ERROR(@"Could not create request with url '%@'.", url);
		return;
	}
	
	[request setHTTPMethod:httpMethod];
	if (body != nil)
		[request setHTTPBody:body];
	
	NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
	NSDictionary *uploadDictionary = @{kUnityAdsAnalyticsUploaderRequestKey:request, kUnityAdsAnalyticsUploaderConnectionKey:connection, kUnityAdsAnalyticsUploaderRetriesKey:retryCount};
	[self.uploadQueue addObject:uploadDictionary];
	  
	if ([self.uploadQueue count] == 1)
		[self _startNextUpload];
}

- (void)_queueWithURLString:(NSString *)urlString queryString:(NSString *)queryString httpMethod:(NSString *)httpMethod retries:(NSNumber *)retryCount {
	NSURL *url = [NSURL URLWithString:urlString];
	NSData *body = nil;
  if (queryString != nil) {
    NSString * queryStringWithIFV = [queryString stringByAppendingFormat:@"&%@=%@",
                                     kUnityAdsInitQueryParamIdentifierForVendor,
                                     [UnityAdsDevice identifierForVendor], nil];
		body = [queryStringWithIFV dataUsingEncoding:NSUTF8StringEncoding];
  }
  
	[self _queueURL:url body:body httpMethod:httpMethod retries:retryCount];
}


#pragma mark - Public

static UnityAdsAnalyticsUploader *sharedUnityAdsInstanceAnalyticsUploader = nil;

+ (id)sharedInstance {
	@synchronized(self) {
		if (sharedUnityAdsInstanceAnalyticsUploader == nil)
      sharedUnityAdsInstanceAnalyticsUploader = [[UnityAdsAnalyticsUploader alloc] init];
	}
	
	return sharedUnityAdsInstanceAnalyticsUploader;
}

- (id)init {
	if ((self = [super init])) {
		_uploadQueue = [NSMutableArray array];
    self.analyticsQueue = dispatch_queue_create("com.unity3d.ads.analytics", NULL);
    self.backgroundThread = [[NSThread alloc] initWithTarget:self selector:@selector(_backgroundRunLoop:) object:nil];
		[self.backgroundThread start];
	}
	
	return self;
}

- (void)dealloc {
  dispatch_release(self.analyticsQueue);
}

#pragma mark - Click track

- (void)sendOpenAppStoreRequest:(UnityAdsCampaign *)campaign {
  if (campaign != nil) {
    NSString *query = [NSString stringWithFormat:@"%@=%@&%@=%@&%@=%@&%@=%@", kUnityAdsAnalyticsQueryParamGameIdKey, [[UnityAdsProperties sharedInstance] adsGameId], kUnityAdsAnalyticsQueryParamEventTypeKey, kUnityAdsAnalyticsEventTypeOpenAppStore, kUnityAdsAnalyticsQueryParamTrackingIdKey, [[UnityAdsProperties sharedInstance] gamerId], kUnityAdsAnalyticsQueryParamProviderIdKey, campaign.id];
    
    id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
    query = [NSString stringWithFormat:@"%@&%@=%@", query, kUnityAdsAnalyticsQueryParamZoneIdKey, [currentZone getZoneId]];
    
    if([currentZone isIncentivized]) {
      id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
      query = [NSString stringWithFormat:@"%@&%@=%@", query, kUnityAdsAnalyticsQueryParamRewardItemKey, [itemManager getCurrentItem].key];
    }
    
    [self performSelector:@selector(sendAnalyticsRequestWithQueryString:) onThread:self.backgroundThread withObject:query waitUntilDone:NO];
  }
}


#pragma mark - Video analytics

- (void)logVideoAnalyticsWithPosition:(VideoAnalyticsPosition)videoPosition campaignId:(NSString *)campaignId viewed:(BOOL)viewed cached:(BOOL)cached {
  UALOG_DEBUG(@"");
	if (campaignId == nil) {
		UALOG_DEBUG(@"Campaign is nil.");
		return;
	}
	
	dispatch_async(self.analyticsQueue, ^{
		NSString *positionString = nil;
    
		if (videoPosition == kVideoAnalyticsPositionStart)
			positionString = kUnityAdsAnalyticsEventTypeVideoStart;
		else if (videoPosition == kVideoAnalyticsPositionFirstQuartile)
			positionString = kUnityAdsAnalyticsEventTypeVideoFirstQuartile;
		else if (videoPosition == kVideoAnalyticsPositionMidPoint)
			positionString = kUnityAdsAnalyticsEventTypeVideoMidPoint;
		else if (videoPosition == kVideoAnalyticsPositionThirdQuartile)
			positionString = kUnityAdsAnalyticsEventTypeVideoThirdQuartile;
		else if (videoPosition == kVideoAnalyticsPositionEnd)
			positionString = kUnityAdsAnalyticsEventTypeVideoEnd;

    if (positionString != nil) {
      NSString *trackingQuery = [NSString stringWithFormat:@"%@/video/%@/%@/%@", [[UnityAdsProperties sharedInstance] gamerId], positionString, campaignId, [[UnityAdsProperties sharedInstance] adsGameId]];

      id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
      trackingQuery = [NSString stringWithFormat:@"%@?%@=%@", trackingQuery, kUnityAdsAnalyticsQueryParamZoneIdKey, [currentZone getZoneId]];
      
      if ([UnityAdsDevice getIOSMajorVersion] < 7) {
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamMacAddressKey, [UnityAdsDevice md5MACAddressString]];
      }
      
      id advertisingIdentifierString = [UnityAdsDevice advertisingIdentifier];
      id md5AdvertisingIdentifierString = [UnityAdsDevice md5AdvertisingIdentifierString];
      
      // Add advertisingTrackingId info if identifier is available
      if (advertisingIdentifierString != nil) {
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamRawAdvertisingTrackingIdKey, advertisingIdentifierString];
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamAdvertisingTrackingIdKey, md5AdvertisingIdentifierString];
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%i", trackingQuery, kUnityAdsInitQueryParamTrackingEnabledKey, [UnityAdsDevice canUseTracking]];
      }
      
      trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamSoftwareVersionKey, [UnityAdsDevice softwareVersion]];
      trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamDeviceTypeKey, [UnityAdsDevice analyticsMachineName]];
      trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamConnectionTypeKey, [UnityAdsDevice currentConnectionType]];
      
      id networkType = [UnityAdsDevice getNetworkType];
      if(networkType != nil) {
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsInitQueryParamNetworkTypeKey, networkType];
      }
      
      trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsAnalyticsQueryParamCachedPlaybackKey, cached ? @"true" : @"false"];
      
      if([currentZone isIncentivized]) {
        id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsAnalyticsQueryParamRewardItemKey, [itemManager getCurrentItem].key];
      }
      
      if ([currentZone getGamerSid] != nil) {
        trackingQuery = [NSString stringWithFormat:@"%@&%@=%@", trackingQuery, kUnityAdsAnalyticsQueryParamGamerSIDKey, [currentZone getGamerSid]];
      }
      
      if (!viewed) {
        [self performSelector:@selector(sendTrackingCallWithQueryString:) onThread:self.backgroundThread withObject:trackingQuery waitUntilDone:YES];
      }
    }
	});
}

- (void)sendTrackingCallWithQueryString:(NSString *)queryString {
  UALOG_DEBUG(@"");
  NSArray *queryStringComponents = [queryString componentsSeparatedByString:@"?"];
  NSString *trackingPath = [queryStringComponents objectAtIndex:0];
  if([queryStringComponents count] > 1) {
    queryString = [queryStringComponents objectAtIndex:1];
  } else {
    queryString = nil;
  }
  
  UALOG_DEBUG(@"Tracking report: %@%@%@ : %@", [[UnityAdsProperties sharedInstance] adsBaseUrl], kUnityAdsAnalyticsTrackingPath, trackingPath, queryString);
  
  [self _queueWithURLString:[NSString stringWithFormat:@"%@%@%@", [[UnityAdsProperties sharedInstance] adsBaseUrl], kUnityAdsAnalyticsTrackingPath,trackingPath] queryString:queryString httpMethod:@"POST" retries:[NSNumber numberWithInt:0]];
}

- (void)sendAnalyticsRequestWithQueryString:(NSString *)queryString {
	if (queryString == nil || [queryString length] == 0) {
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
  
  UALOG_DEBUG(@"View report: %@?%@", [[UnityAdsProperties sharedInstance] analyticsBaseUrl], queryString);
	[self _queueWithURLString:[[UnityAdsProperties sharedInstance] analyticsBaseUrl] queryString:queryString httpMethod:@"POST" retries:[NSNumber numberWithInt:0]];
}

#pragma mark - Install tracking

- (void)sendInstallTrackingCallWithQueryDictionary:(NSDictionary *)queryDictionary {	
	if (queryDictionary == nil) {
		UALOG_DEBUG(@"Invalid input.");
		return;
	}
	
	NSString *query = [queryDictionary objectForKey:kUnityAdsAnalyticsQueryDictionaryQueryKey];
	NSString *body = [queryDictionary objectForKey:kUnityAdsAnalyticsQueryDictionaryBodyKey];
	
	if (query == nil || [query length] == 0 || body == nil || [body length] == 0) {
		UALOG_DEBUG(@"Invalid parameters in query dictionary.");
		return;
	}
	
  [self _queueWithURLString:[NSString stringWithFormat:@"%@%@", [[UnityAdsProperties sharedInstance] adsBaseUrl], kUnityAdsAnalyticsInstallTrackingPath] queryString:nil httpMethod:@"GET" retries:[NSNumber numberWithInt:0]];
}


#pragma mark - Error handling

- (void)retryFailedUploads {
	NSArray *uploads = [[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey];
	if (uploads != nil) {
		for (NSDictionary *upload in uploads) {
			NSString *url = [upload objectForKey:kUnityAdsAnalyticsSavedUploadURLKey];
			NSString *body = [upload objectForKey:kUnityAdsAnalyticsSavedUploadBodyKey];
			NSString *httpMethod = [upload objectForKey:kUnityAdsAnalyticsSavedUploadHTTPMethodKey];
      NSNumber *retries = @(0);
      
      if ([upload objectForKey:kUnityAdsAnalyticsUploaderRetriesKey] != nil) {
        retries = [upload objectForKey:kUnityAdsAnalyticsUploaderRetriesKey];
        retries = [NSNumber numberWithInt:[retries intValue] + 1];
      }
      
      // Check if too many retries
      if ([retries intValue] > [[UnityAdsProperties sharedInstance] maxNumberOfAnalyticsRetries]) {
        continue;
      }
      
      [self _queueURL:[NSURL URLWithString:url] body:[body dataUsingEncoding:NSUTF8StringEncoding] httpMethod:httpMethod retries:retries];
		}
		
		[[NSUserDefaults standardUserDefaults] removeObjectForKey:kUnityAdsAnalyticsSavedUploadsKey];
		[[NSUserDefaults standardUserDefaults] synchronize];
	}
}

- (void)_saveFailedUpload:(NSDictionary *)upload {
	if (upload == nil) {
		UALOG_DEBUG(@"Input is nil.");
		return;
	}
	
	NSMutableArray *existingFailedUploads = [[[NSUserDefaults standardUserDefaults] arrayForKey:kUnityAdsAnalyticsSavedUploadsKey] mutableCopy];
	
	if (existingFailedUploads == nil) {
        existingFailedUploads = [NSMutableArray array];
    }
    
	NSURLRequest *request = [upload objectForKey:kUnityAdsAnalyticsUploaderRequestKey];
	NSMutableDictionary *failedUpload = [NSMutableDictionary dictionary];
	
    if ([request URL] != nil) {
		[failedUpload setObject:[[request URL] absoluteString] forKey:kUnityAdsAnalyticsSavedUploadURLKey];
        
        NSNumber *retries = @(0);
        if ([upload objectForKey:kUnityAdsAnalyticsUploaderRetriesKey] != nil)
        {
            retries = [upload objectForKey:kUnityAdsAnalyticsUploaderRetriesKey];
        }
        
		[failedUpload setObject:retries forKey:kUnityAdsAnalyticsUploaderRetriesKey];
        
		if ([request HTTPBody] != nil) {
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


#pragma mark - NSURLConnectionDelegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
	UALOG_DEBUG(@"");
  NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
  
  if ([httpResponse statusCode] >= 400) {
    UALOG_DEBUG(@"ERROR FECTHING URL: %li", (long)[httpResponse statusCode]);
    [self _saveFailedUpload:self.currentUpload];
  }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
	UALOG_DEBUG(@"");
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
	UALOG_DEBUG(@"");
	self.currentUpload = nil;	
	[self _startNextUpload];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
	UALOG_DEBUG(@"Analytics upload connection error: %@", error);
	
	[self _saveFailedUpload:self.currentUpload];
	self.currentUpload = nil;
	[self _startNextUpload];
}

@end