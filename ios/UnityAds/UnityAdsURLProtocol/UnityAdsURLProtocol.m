//
//  UnityAdsURLProtocol.m
//  UnityAds
//
//  Created by bluesun on 10/10/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsURLProtocol.h"
#import "../UnityAdsWebView/UnityAdsWebAppController.h"

static const NSString *kUnityAdsURLProtocolHostname = @"nativebridge.unityads.unity3d.com";

@implementation UnityAdsURLProtocol

+ (BOOL)canInitWithRequest:(NSURLRequest *)request {
  NSURL *url = [request URL];
  
  if ([[request HTTPMethod] isEqualToString:@"POST"] || [[request HTTPMethod] isEqualToString:@"OPTIONS"]) {
    if ([[url host] isEqualToString:(NSString *)kUnityAdsURLProtocolHostname]) {
      return TRUE;
    }
  }
  
  return FALSE;
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
  return request;
}

- (void)startLoading {
  NSURLRequest *request = [self request];
  NSData *reqData = [request HTTPBody];

  if(reqData != nil) {
    [self actOnJSONResults: reqData];
  }
  
  // Create the response
  NSData *responseData = [@"{\"status\":\"ok\"}" dataUsingEncoding:NSUTF8StringEncoding];
  
  NSDictionary *headers = @{
                            @"Access-Control-Allow-Origin":@"*",
                            @"Access-Control-Allow-Headers":@"origin, content-type",
                            @"Content-Type":@"application/json",
                            @"Content-Length":[NSString stringWithFormat:@"%lu", (unsigned long)responseData.length]
                            };
  
  NSHTTPURLResponse *response = [[NSHTTPURLResponse alloc] initWithURL:[request URL] statusCode:200 HTTPVersion:@"1.1" headerFields:headers];
  
  // get a reference to the client so we can hand off the data
  id<NSURLProtocolClient> client = [self client];
  
  // turn off caching for this response data
	[client URLProtocol:self didReceiveResponse:response
   cacheStoragePolicy:NSURLCacheStorageNotAllowed];
  
  // set the data in the response to our response data
	[client URLProtocol:self didLoadData:responseData];
  
  // notify that we completed loading
	[client URLProtocolDidFinishLoading:self];
}

- (void)stopLoading {
}

- (void)actOnJSONResults:(NSData *)jsonData {
  NSError* jsonError;
  NSDictionary* jsonDict = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:&jsonError];
  
  if (!jsonDict) {
    UALOG_DEBUG(@"ERROR PARSING JSON: %@", jsonError);
    return;
  }
  
  if (![jsonDict isKindOfClass:[NSDictionary class]]) {
    UALOG_DEBUG(@"Wrong type of return value");
    return;
  }
  
  NSDictionary *results = jsonDict;
  
  if (results != nil) {
    __block NSString *type = [results objectForKey:@"type"];
    __block NSDictionary *dictData = nil;
    
    id data = [results objectForKey:@"data"];
    if ([data isKindOfClass:[NSDictionary class]]) {
      dictData = (NSDictionary *)data;
    }
    
    if (dictData != nil) {
      dispatch_async(dispatch_get_main_queue(), ^{
        [[UnityAdsWebAppController sharedInstance] handleWebEvent:type data:dictData];
      });
    }
  }
}

@end
