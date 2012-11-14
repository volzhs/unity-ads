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
#import "UnityAdsWebView/UnityAdsWebAppController.h"

@class UnityAdsCampaign;
@class UnityAdsViewManager;
@class SKStoreProductViewController;

@protocol UnityAdsViewManagerDelegate <NSObject>

@required
//- (UnityAdsCampaign *)viewManager:(UnityAdsViewManager *)viewManager campaignWithID:(NSString *)campaignID;
//- (NSURL *)viewManager:(UnityAdsViewManager *)viewManager videoURLForCampaign:(UnityAdsCampaign *)campaign;
- (void)viewManagerStartedPlayingVideo;
- (void)viewManagerVideoEnded;
- (void)viewManager:(UnityAdsViewManager *)viewManager loggedVideoPosition:(VideoAnalyticsPosition)videoPosition campaign:(UnityAdsCampaign *)campaign;
- (UIViewController *)viewControllerForPresentingViewControllersForViewManager:(UnityAdsViewManager *)viewManager;
- (void)viewManagerWillCloseAdView;
- (void)viewManagerWebViewInitialized;
@end

@interface UnityAdsViewManager : NSObject <UnityAdsVideoDelegate, UnityAdsWebAppControllerDelegate>

@property (nonatomic, assign) id<UnityAdsViewManagerDelegate> delegate;
@property (nonatomic, assign, readonly) BOOL adViewVisible;

+ (id)sharedInstance;
- (UIView *)adView;
- (void)loadWebView;
- (void)initWebApp;
- (void)openAppStoreWithGameId:(NSString *)gameId;
- (void)showPlayerAndPlaySelectedVideo;
- (void)closeAdView;

@end
