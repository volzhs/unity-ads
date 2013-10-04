//
//  UnityAdsZone.h
//  UnityAds
//
//  Created by Ville Orkas on 9/17/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsZone : NSObject

- (id)initWithData:(NSDictionary *)options;

- (NSString *)getZoneId;

- (BOOL)isIncentivized;

- (BOOL)noWebView;
- (BOOL)noOfferScreen;
- (BOOL)openAnimated;
- (BOOL)muteVideoSounds;
- (BOOL)useDeviceOrientationForVideo;

- (int)allowVideoSkipInSeconds;

- (BOOL)allowsOverride:(NSString *)option;
- (void)mergeOptions:(NSDictionary *)options;

@end
