//
//  UnityAds.h
//  UnityAds
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>

#define UNITY_ADS_DEBUG_MODE_ENABLED 1

#define UALOG_LOG(levelName, fmt, ...) NSLog((@"%@ [T:0x%x %@] %s:%d " fmt), levelName, (unsigned int)[NSThread currentThread], ([[NSThread currentThread] isMainThread] ? @"M" : @"S"), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__)

#define UALOG_ERROR(fmt, ...) UALOG_LOG(@"ERROR", fmt, ##__VA_ARGS__)

#if UNITY_ADS_DEBUG_MODE_ENABLED
#define UALOG_DEBUG(fmt, ...) UALOG_LOG(@"DEBUG", fmt, ##__VA_ARGS__)
#define UAAssert(condition) do { if ( ! (condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); abort(); } } while(0)
#define UAAssertV(condition, value) do { if ( ! (condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); abort(); } } while(0)
#else
#define UALOG_DEBUG(...)
#define UAAssert(condition) do { if ( ! (condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); return; } } while(0)
#define UAAssertV(condition, value) do { if ( ! (condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); return value; } } while(0)
#endif


//
//  All delegate methods and public methods in this header are based on the tentative iOS specification document,
//  and will probably change during development.
//

@class UnityAds;
@class SKStoreProductViewController;

@protocol UnityAdsDelegate <NSObject>

@required
- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey;
- (UIViewController *)viewControllerForPresentingViewControllersForAds:(UnityAds *)unityAds;

@optional
- (void)unityAdsWillShow:(UnityAds *)unityAds;
- (void)unityAdsWillHide:(UnityAds *)unityAds;
- (void)unityAdsVideoStarted:(UnityAds *)unityAds;
- (void)unityAdsFetchCompleted:(UnityAds *)unityAds;

@end

@interface UnityAds : NSObject

@property (nonatomic, assign) id<UnityAdsDelegate> delegate;

+ (id)sharedInstance;
- (void)startWithGameId:(NSString *)gameId;
- (UIView *)adsView;
- (BOOL)canShow;
- (void)stopAll;
- (void)trackInstall;
- (void)refresh;

@end
