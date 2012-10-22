//
//  UnityAdsViewManager.h
//  UnityAdsExample
//
//  Created by Johan Halin on 9/20/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "UnityAdsVideo/UnityAdsVideo.h"

@class UnityAdsCampaign;
@class UnityAdsViewManager;
@class SKStoreProductViewController;

@protocol UnityAdsViewManagerDelegate <NSObject>

@required
- (UnityAdsCampaign *)viewManager:(UnityAdsViewManager *)viewManager campaignWithID:(NSString *)campaignID;
- (NSURL *)viewManager:(UnityAdsViewManager *)viewManager videoURLForCampaign:(UnityAdsCampaign *)campaign;
- (void)viewManagerStartedPlayingVideo:(UnityAdsViewManager *)viewManager;
- (void)viewManagerVideoEnded:(UnityAdsViewManager *)viewManager;
- (void)viewManager:(UnityAdsViewManager *)viewManager loggedVideoPosition:(VideoAnalyticsPosition)videoPosition campaign:(UnityAdsCampaign *)campaign;
- (UIViewController *)viewControllerForPresentingViewControllersForViewManager:(UnityAdsViewManager *)viewManager;
- (void)viewManagerWillCloseAdView:(UnityAdsViewManager *)viewManager;
- (void)viewManagerWebViewInitialized:(UnityAdsViewManager *)viewManager;

@end

@interface UnityAdsViewManager : NSObject <UnityAdsVideoDelegate>

@property (nonatomic, assign) id<UnityAdsViewManagerDelegate> delegate;
@property (nonatomic, strong) NSString *machineName;
@property (nonatomic, strong) NSString *md5AdvertisingIdentifier;
@property (nonatomic, strong) NSString *md5MACAddress;
@property (nonatomic, strong) NSString *md5OpenUDID;
@property (nonatomic, strong) NSString *campaignJSON;
@property (nonatomic, strong) UnityAdsCampaign *selectedCampaign;
@property (nonatomic, assign, readonly) BOOL adViewVisible;

+ (id)sharedInstance;
- (void)handleWebEvent:(NSString *)type data:(NSDictionary *)data;
- (void)loadWebView;
- (UIView *)adView;

@end
