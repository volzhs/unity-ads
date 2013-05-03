//
//  UnityAdsShowOptionsParser.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsShowOptionsParser : NSObject

@property (nonatomic, assign) BOOL openAnimated;
@property (nonatomic, assign) BOOL noOfferScreen;
@property (nonatomic, assign) NSString *gamerSID;
@property (nonatomic, assign) BOOL muteVideoSounds;
@property (nonatomic, assign) BOOL useDeviceOrientationForVideo;

+ (UnityAdsShowOptionsParser *)sharedInstance;
- (void)parseOptions:(NSDictionary *)options;
- (void)resetToDefaults;
- (NSString *)getOptionsAsJson;
@end
