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
- (void)mainControllerWebViewInitialized;
- (void)mainControllerWillOpen;
- (void)mainControllerDidOpen;
- (void)mainControllerWillClose;
- (void)mainControllerDidClose;
- (void)mainControllerStartedPlayingVideo;
- (void)mainControllerVideoEnded;
@end

@interface UnityAdsMainViewController : UIViewController <UnityAdsVideoControllerDelegate, UnityAdsWebAppControllerDelegate>

@property (nonatomic, assign) id<UnityAdsMainViewControllerDelegate> delegate;

+ (id)sharedInstance;

- (BOOL)openAds:(BOOL)animated;
- (BOOL)closeAds:(BOOL)forceMainThread withAnimations:(BOOL)animated;
- (BOOL)mainControllerVisible;
- (void)showPlayerAndPlaySelectedVideo:(BOOL)checkIfWatched;
- (void)openAppStoreWithData:(NSDictionary *)data;

@end
