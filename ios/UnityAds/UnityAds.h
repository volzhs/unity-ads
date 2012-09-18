//
//  UnityAds.h
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

#define UALOG_DEBUG_LOGGING_ENABLED 1

#define UALOG_LOG(levelName, fmt, ...) NSLog((@"%@ [T:0x%x %@] %s:%d " fmt), levelName, (unsigned int)[NSThread currentThread], ([[NSThread currentThread] isMainThread] ? @"M" : @"S"), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__)

#if UALOG_DEBUG_LOGGING_ENABLED
#define UALOG_DEBUG(fmt, ...) UALOG_LOG(@"DEBUG", fmt, ##__VA_ARGS__)
#else
#define UALOG_DEBUG(...)
#endif

#define UALOG_ERROR(fmt, ...) UALOG_LOG(@"ERROR", fmt, ##__VA_ARGS__)

//
//  All delegate methods and public methods in this header are based on the tentative iOS specification document,
//  and will probably change during development.
//

@class UnityAds;

@protocol UnityAdsDelegate <NSObject>

@optional
- (void)unityAdsWillShow:(UnityAds *)unityAds;
- (void)unityAdsWillHide:(UnityAds *)unityAds;
- (void)unityAdsVideoStarted:(UnityAds *)unityAds;
- (void)unityAdsVideoCompleted:(UnityAds *)unityAds;
- (void)unityAdsFetchCompleted:(UnityAds *)unityAds;
- (void)unityAds:(UnityAds *)unityAds wantsToShowAdView:(UIView *)adView;

@end

@interface UnityAds : NSObject

@property (nonatomic, assign) id<UnityAdsDelegate> delegate;

+ (id)sharedInstance;

- (void)startWithGameId:(NSString *)gameId;
- (BOOL)show;
- (BOOL)hasCampaigns;
- (void)stopAll;

@end
