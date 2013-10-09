//
//  UnityAdsZone.m
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "../UnityAds.h"
#import "UnityAdsZone.h"
#import "UnityAdsConstants.h"

@interface UnityAdsZone ()

@property (nonatomic, strong) NSMutableDictionary *_options;
@property (nonatomic, strong) NSString * _gamerSid;
@property (nonatomic) BOOL _isDefault;

@end

@implementation UnityAdsZone

- (id)initWithData:(NSDictionary *)options {
  self = [super init];
  if(self) {
    self._options = [NSMutableDictionary dictionaryWithDictionary:options];
    self._gamerSid = nil;
    self._isDefault = [[options valueForKey:kUnityAdsZoneDefaultKey] boolValue];
  }
  return self;
}

- (BOOL)isIncentivized {
  return FALSE;
}

- (BOOL)isDefault {
  return self._isDefault;
}

- (NSString *)getZoneId {
  return [self._options valueForKey:kUnityAdsZoneIdKey];
}

- (NSDictionary *)getZoneOptions {
  return self._options;
}

- (BOOL)noWebView {
  return [[self._options valueForKey:kUnityAdsZoneNoWebViewKey] boolValue];
}

- (BOOL)noOfferScreen {
  return [[self._options valueForKey:kUnityAdsZoneNoOfferScreenKey] boolValue];
}

- (BOOL)openAnimated {
  return [[self._options valueForKey:kUnityAdsZoneOpenAnimatedKey] boolValue];
}

- (BOOL)muteVideoSounds {
  return [[self._options valueForKey:kUnityAdsZoneMuteVideoSoundsKey] boolValue];
}

- (BOOL)useDeviceOrientationForVideo {
  return [[self._options valueForKey:kUnityAdsZoneUseDeviceOrientationForVideoKey] boolValue];
}

- (NSString *)getGamerSid {
  return self._gamerSid;
}

- (void)setGamerSid:(NSString *)gamerSid {
  self._gamerSid = gamerSid;
}

- (void)setNoOfferScreen:(BOOL)noOfferScreen {
  NSString *stringValue = noOfferScreen ? @"1" : @"0";
  [self._options setObject:stringValue forKey:kUnityAdsZoneNoOfferScreenKey];
}

- (int)allowVideoSkipInSeconds {
  return [[self._options valueForKey:kUnityAdsZoneAllowVideoSkipInSecondsKey] integerValue];
}

- (BOOL)allowsOverride:(NSString *)option {
  id allowOverrides = [self._options objectForKey:kUnityAdsZoneAllowOverrides];
  return [allowOverrides indexOfObject:option] != NSNotFound;
}

- (void)mergeOptions:(NSDictionary *)options {
  [options enumerateKeysAndObjectsUsingBlock:^(id optionKey, id optionValue, BOOL *stop) {
    if([self allowsOverride:optionKey]) {
      [self._options setObject:optionValue forKey:optionKey];
    }
  }];
  NSString * gamerSid = [options valueForKey:kUnityAdsOptionGamerSIDKey];
  if(gamerSid != nil) {
    [self setGamerSid:gamerSid];
  }
}

@end
