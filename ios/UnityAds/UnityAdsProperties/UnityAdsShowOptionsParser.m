//
//  UnityAdsShowOptionsParser.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsShowOptionsParser.h"
#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsProperties.h"
#import "../UnityAdsSBJSON/UnityAdsSBJsonWriter.h"
#import "../UnityAdsSBJSON/NSObject+UnityAdsSBJson.h"

@implementation UnityAdsShowOptionsParser

static UnityAdsShowOptionsParser *sharedOptionsParser = nil;

+ (UnityAdsShowOptionsParser *)sharedInstance {
	@synchronized(self) {
		if (sharedOptionsParser == nil) {
      sharedOptionsParser = [[UnityAdsShowOptionsParser alloc] init];
      [sharedOptionsParser resetToDefaults];
		}
	}
	
	return sharedOptionsParser;
}


- (void)parseOptions:(NSDictionary *)options {
  [self resetToDefaults];
  
  if (options != NULL) {
    if ([options objectForKey:kUnityAdsOptionNoOfferscreenKey] != nil && [[options objectForKey:kUnityAdsOptionNoOfferscreenKey] boolValue] == YES) {
      self.noOfferScreen = YES;
    }
    
    if ([options objectForKey:kUnityAdsOptionOpenAnimatedKey] != nil && [[options objectForKey:kUnityAdsOptionOpenAnimatedKey] boolValue] == NO) {
      self.openAnimated = NO;
    }
    
    if ([options objectForKey:kUnityAdsOptionGamerSIDKey] != nil) {
      self.gamerSID = [options objectForKey:kUnityAdsOptionGamerSIDKey];
    }
    
    if ([options objectForKey:kUnityAdsOptionMuteVideoSounds] != nil && [[options objectForKey:kUnityAdsOptionMuteVideoSounds] boolValue] == YES) {
      self.muteVideoSounds = YES;
    }

  }
}

- (NSDictionary *)getOptionsAsJson {
  NSMutableDictionary *options = [NSMutableDictionary dictionary];
  
  [options setObject:@(self.noOfferScreen) forKey:kUnityAdsOptionNoOfferscreenKey];
  [options setObject:@(self.openAnimated) forKey:kUnityAdsOptionOpenAnimatedKey];
  [options setObject:@(self.muteVideoSounds) forKey:kUnityAdsOptionMuteVideoSounds];
  
  if (self.gamerSID != nil) {
    [options setObject:self.gamerSID forKey:kUnityAdsOptionGamerSIDKey];
  }
  
  return options;
}

- (void)resetToDefaults {
  self.noOfferScreen = NO;
  self.openAnimated = YES;
  self.gamerSID = NULL;
  self.muteVideoSounds = NO;
}

@end
