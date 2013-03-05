//
//  UnityAds.h
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>

#define UALOG_LOG(levelName, fmt, ...) if ([[UnityAds sharedInstance] isDebugMode]) NSLog((@"%@ [T:0x%x %@] %s:%d " fmt), levelName, (unsigned int)[NSThread currentThread], ([[NSThread currentThread] isMainThread] ? @"M" : @"S"), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__)

#define UALOG_ERROR(fmt, ...) UALOG_LOG(@"ERROR", fmt, ##__VA_ARGS__)

#define UALOG_DEBUG(fmt, ...) UALOG_LOG(@"DEBUG", fmt, ##__VA_ARGS__)
#define UAAssert(condition) do { if ([[UnityAds sharedInstance] isDebugMode] && !(condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); abort(); } } while(0)
#define UAAssertV(condition, value) do { if ([[UnityAds sharedInstance] isDebugMode] && !(condition)) { UALOG_ERROR(@"Expected condition '%s' to be true.", #condition); abort(); } } while(0)

extern NSString * const kUnityAdsRewardItemPictureKey;
extern NSString * const kUnityAdsRewardItemNameKey;

extern NSString * const kUnityAdsOptionNoOfferscreenKey;
extern NSString * const kUnityAdsOptionOpenAnimatedKey;
extern NSString * const kUnityAdsOptionGamerSIDKey;

@class UnityAds;
@class SKStoreProductViewController;

@protocol UnityAdsDelegate <NSObject>

@required
- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey;

@optional
- (void)unityAdsWillShow:(UnityAds *)unityAds;
- (void)unityAdsDidShow:(UnityAds *)unityAds;
- (void)unityAdsWillHide:(UnityAds *)unityAds;
- (void)unityAdsDidHide:(UnityAds *)unityAds;
- (void)unityAdsWillLeaveApplication:(UnityAds *)unityAds;
- (void)unityAdsVideoStarted:(UnityAds *)unityAds;
- (void)unityAdsFetchCompleted:(UnityAds *)unityAds;
- (void)unityAdsFetchFailed:(UnityAds *)unityAds;

@end

@interface UnityAds : NSObject

@property (nonatomic, assign) id<UnityAdsDelegate> delegate;

+ (UnityAds *)sharedInstance;
+ (BOOL)isSupported;
+ (NSString *)getSDKVersion;
- (void)setDebugMode:(BOOL)debugMode;
- (BOOL)isDebugMode;
- (void)setTestMode:(BOOL)testModeEnabled;
- (BOOL)startWithGameId:(NSString *)gameId andViewController:(UIViewController *)viewController;
- (BOOL)startWithGameId:(NSString *)gameId;
- (void)setViewController:(UIViewController *)viewController showImmediatelyInNewController:(BOOL)applyAds;
- (BOOL)canShow;
- (BOOL)canShow;
- (BOOL)show:(NSDictionary *)options;
- (BOOL)show;
- (BOOL)hide;
- (void)stopAll;
- (BOOL)hasMultipleRewardItems;
- (NSArray *)getRewardItemKeys;
- (NSString *)getDefaultRewardItemKey;
- (NSString *)getCurrentRewardItemKey;
- (BOOL)setRewardItemKey:(NSString *)rewardItemKey;
- (void)setDefaultRewardItemAsRewardItem;
- (NSDictionary *)getRewardItemDetailsWithKey:(NSString *)rewardItemKey;
@end
