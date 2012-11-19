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
- (void)viewManagerStartedPlayingVideo;
- (void)viewManagerVideoEnded;
- (UIViewController *)viewControllerForPresentingViewControllersForViewManager:(UnityAdsViewManager *)viewManager;
- (void)viewManagerWillCloseAdView;
- (void)viewManagerWebViewInitialized;
@end

@interface UnityAdsViewManager : NSObject <UnityAdsVideoDelegate, UnityAdsWebAppControllerDelegate>

@property (nonatomic, assign) id<UnityAdsViewManagerDelegate> delegate;
@property (nonatomic, assign, readonly) BOOL adViewVisible;
@property (nonatomic) BOOL webViewInitialized;

+ (UnityAdsViewManager *)sharedInstance;
- (UIView *)adView;
- (void)initWebApp;
- (void)openAppStoreWithData:(NSDictionary *)data;
- (void)showPlayerAndPlaySelectedVideo;
- (void)hidePlayer;
- (void)closeAdView;

@end
