//
//  UnityAdsCampaign.m
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"

#import "UnityAdsCampaign.h"

@interface UnityAdsCampaign ()
@end

@implementation UnityAdsCampaign

- (id)initWithData:(NSDictionary *)data {
  self = [super init];
  if (self) {
    self.isValidCampaign = false;
    [self setupFromData:data];
  }
  return self;
}

- (void)setupFromData:(NSDictionary *)data {
  BOOL failedData = false;
  
  self.viewed = NO;
  self.nativeTrackingQuerySent = false;
  
  NSString *endScreenURLString = [data objectForKey:kUnityAdsCampaignEndScreenKey];
  if (endScreenURLString == nil) failedData = true;
  UAAssertV([endScreenURLString isKindOfClass:[NSString class]], nil);
  NSURL *endScreenURL = [NSURL URLWithString:endScreenURLString];
  UAAssertV(endScreenURL != nil, nil);
  self.endScreenURL = endScreenURL;
  
  NSString *endScreenPortraitURLString = [data objectForKey:kUnityAdsCampaignEndScreenPortraitKey];
  if (endScreenPortraitURLString != nil) {
    UAAssertV([endScreenPortraitURLString isKindOfClass:[NSString class]], nil);
    NSURL *endScreenPortraitURL = [NSURL URLWithString:endScreenPortraitURLString];
    UAAssertV(endScreenPortraitURL != nil, nil);
    UALOG_DEBUG(@"Found endScreenPortraitURL");
    self.endScreenPortraitURL = endScreenPortraitURL;
  }
    
  NSString *clickURLString = [data objectForKey:kUnityAdsCampaignClickURLKey];
  if (clickURLString == nil) failedData = true;
  UAAssertV([clickURLString isKindOfClass:[NSString class]], nil);
  NSURL *clickURL = [NSURL URLWithString:clickURLString];
  UAAssertV(clickURL != nil, nil);
  self.clickURL = clickURL;
  
  NSString *pictureURLString = [data objectForKey:kUnityAdsCampaignPictureKey];
  if (pictureURLString == nil) failedData = true;
  UAAssertV([pictureURLString isKindOfClass:[NSString class]], nil);
  NSURL *pictureURL = [NSURL URLWithString:pictureURLString];
  UAAssertV(pictureURL != nil, nil);
  self.pictureURL = pictureURL;
  
  NSString *trailerDownloadableURLString = [data objectForKey:kUnityAdsCampaignTrailerDownloadableKey];
  if (trailerDownloadableURLString == nil) failedData = true;
  UAAssertV([trailerDownloadableURLString isKindOfClass:[NSString class]], nil);
  NSURL *trailerDownloadableURL = [NSURL URLWithString:trailerDownloadableURLString];
  UAAssertV(trailerDownloadableURL != nil, nil);
  self.trailerDownloadableURL = trailerDownloadableURL;
  
  NSString *trailerStreamingURLString = [data objectForKey:kUnityAdsCampaignTrailerStreamingKey];
  if (trailerStreamingURLString == nil) failedData = true;
  UAAssertV([trailerStreamingURLString isKindOfClass:[NSString class]], nil);
  NSURL *trailerStreamingURL = [NSURL URLWithString:trailerStreamingURLString];
  UAAssertV(trailerStreamingURL != nil, nil);
  self.trailerStreamingURL = trailerStreamingURL;
  
  NSString *gameIconURLString = [data objectForKey:kUnityAdsCampaignGameIconKey];
  if (gameIconURLString == nil) failedData = true;
  UAAssertV([gameIconURLString isKindOfClass:[NSString class]], nil);
  NSURL *gameIconURL = [NSURL URLWithString:gameIconURLString];
  UAAssertV(gameIconURL != nil, nil);
  self.gameIconURL = gameIconURL;
  
  id gameIDValue = [data objectForKey:kUnityAdsCampaignGameIDKey];
  if (gameIDValue == nil) failedData = true;
  UAAssertV(gameIDValue != nil && ([gameIDValue isKindOfClass:[NSString class]] || [gameIDValue isKindOfClass:[NSNumber class]]), nil);
  NSString *gameID = [gameIDValue isKindOfClass:[NSNumber class]] ? [gameIDValue stringValue] : gameIDValue;
  UAAssertV(gameID != nil && [gameID length] > 0, nil);
  self.gameID = gameID;
  
  id gameNameValue = [data objectForKey:kUnityAdsCampaignGameNameKey];
  if (gameNameValue == nil) failedData = true;
  UAAssertV(gameNameValue != nil && ([gameNameValue isKindOfClass:[NSString class]] || [gameNameValue isKindOfClass:[NSNumber class]]), nil);
  NSString *gameName = [gameNameValue isKindOfClass:[NSNumber class]] ? [gameNameValue stringValue] : gameNameValue;
  UAAssertV(gameName != nil && [gameName length] > 0, nil);
  self.gameName = gameName;
  
  id idValue = [data objectForKey:kUnityAdsCampaignIDKey];
  if (idValue == nil) failedData = true;
  UAAssertV(idValue != nil && ([idValue isKindOfClass:[NSString class]] || [idValue isKindOfClass:[NSNumber class]]), nil);
  NSString *idString = [idValue isKindOfClass:[NSNumber class]] ? [idValue stringValue] : idValue;
  UAAssertV(idString != nil && [idString length] > 0, nil);
  self.id = idString;
  
  id tagLineValue = [data objectForKey:kUnityAdsCampaignTaglineKey];
  if (tagLineValue == nil) failedData = true;
  UAAssertV(tagLineValue != nil && ([tagLineValue isKindOfClass:[NSString class]] || [tagLineValue isKindOfClass:[NSNumber class]]), nil);
  NSString *tagline = [tagLineValue isKindOfClass:[NSNumber class]] ? [tagLineValue stringValue] : tagLineValue;
  UAAssertV(tagline != nil && [tagline length] > 0, nil);
  self.tagLine = tagline;
  
  id itunesIDValue = [data objectForKey:kUnityAdsCampaignStoreIDKey];
  if (itunesIDValue == nil) failedData = true;
  UAAssertV(itunesIDValue != nil && ([itunesIDValue isKindOfClass:[NSString class]] || [itunesIDValue isKindOfClass:[NSNumber class]]), nil);
  NSString *itunesID = [itunesIDValue isKindOfClass:[NSNumber class]] ? [itunesIDValue stringValue] : itunesIDValue;
  UAAssertV(itunesID != nil && [itunesID length] > 0, nil);
  self.itunesID = itunesID;
  
  self.shouldCacheVideo = NO;
  if ([data objectForKey:kUnityAdsCampaignCacheVideoKey] != nil) {
    if ([[data valueForKey:kUnityAdsCampaignCacheVideoKey] boolValue] != 0) {
      self.shouldCacheVideo = YES;
    }
  }
  
  self.bypassAppSheet = NO;
  if ([data objectForKey:kUnityAdsCampaignBypassAppSheet] != nil) {
    if ([[data valueForKey:kUnityAdsCampaignBypassAppSheet] boolValue] != 0) {
      self.bypassAppSheet = YES;
    }
  }
  
  self.expectedTrailerSize = -1;
  if ([data objectForKey:kUnityAdsCampaignExpectedFileSize] != nil) {
    if ([[data valueForKey:kUnityAdsCampaignExpectedFileSize] longLongValue] != 0) {
      self.expectedTrailerSize = [[data valueForKey:kUnityAdsCampaignExpectedFileSize] longLongValue];
    }
  }
  
  if (!failedData) {
    self.isValidCampaign = true;
  }

  /*
  NSString *customClickURLString = [data objectForKey:kUnityAdsCampaignCustomClickURLKey];
  if (customClickURLString == nil) failedData = true;
  UAAssertV([customClickURLString isKindOfClass:[NSString class]], nil);
  
  if (customClickURLString != nil && [customClickURLString length] > 4) {
    UALOG_DEBUG(@"CustomClickUrl=%@ for CampaignID=%@", customClickURLString, idString);
    NSURL *customClickURL = [NSURL URLWithString:customClickURLString];
    UAAssertV(customClickURL != nil, nil);
    self.customClickURL = customClickURL;
  }
  else {
    UALOG_DEBUG(@"Not a valid URL: %@", customClickURLString);
  }*/
  
  data = nil;
}

@end
