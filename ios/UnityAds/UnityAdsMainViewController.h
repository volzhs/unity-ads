//
//  UnityAdsAdViewController.h
//  UnityAds
//
//  Created by bluesun on 11/21/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "UnityAdsWebView/UnityAdsWebAppController.h"
#import "UnityAdsVideo/UnityAdsVideoViewController.h"

@protocol UnityAdsMainViewControllerDelegate <NSObject>

@required
- (void)mainControllerStartedPlayingVideo;
- (void)mainControllerVideoEnded;
- (void)mainControllerWillCloseAdView;
- (void)mainControllerWebViewInitialized;
@end

@interface UnityAdsMainViewController : UIViewController <UnityAdsVideoControllerDelegate, UnityAdsWebAppControllerDelegate>

@property (nonatomic, assign) id<UnityAdsMainViewControllerDelegate> delegate;
//@property (nonatomic) BOOL webViewInitialized;

+ (id)sharedInstance;

- (BOOL)openAds;
- (BOOL)closeAds;
- (BOOL)mainControllerVisible;
- (void)showPlayerAndPlaySelectedVideo:(BOOL)checkIfWatched;
- (void)openAppStoreWithData:(NSDictionary *)data;

@end
