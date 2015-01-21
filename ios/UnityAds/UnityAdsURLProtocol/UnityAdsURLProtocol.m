//
//  UnityAdsURLProtocol.m
//  UnityAds
//
//  Created by bluesun on 10/10/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsURLProtocol.h"
#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsSBJSON/NSObject+UnityAdsSBJson.h"

static const NSString *kUnityAdsURLProtocolHostname = @"nativebridge.unityads.unity3d.com";

@implementation UnityAdsURLProtocol

+ (BOOL)canInitWithRequest:(NSURLRequest *)request {
  NSURL *url = [request URL];
  
  if ([[url scheme] isEqualToString:@"http"]) {
    if ([[request HTTPMethod] isEqualToString:@"POST"]) {
      if ([[url host] isEqualToString:(NSString *)kUnityAdsURLProtocolHostname]) {
        return TRUE;
      }
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

  [self actOnJSONResults: reqData];
  
  // Create the response
  NSData *responseData = [@"status: ok" dataUsingEncoding:NSUTF8StringEncoding];
	NSURLResponse *response =
  [[NSURLResponse alloc] initWithURL:[request URL]
                            MIMEType:@"application/json"
               expectedContentLength:-1
                    textEncodingName:nil];
  
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
  if (![[jsonData JSONValue] isKindOfClass:[NSDictionary class]]) {
    UALOG_DEBUG(@"Wrong type of return value");
    return;
  }
  
  NSDictionary *results = [jsonData JSONValue];
  
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
