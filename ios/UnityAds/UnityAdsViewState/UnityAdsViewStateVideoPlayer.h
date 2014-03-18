//
//  UnityAdsViewStateVideoPlayer.h
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewState.h"
#import "../UnityAdsVideo/UnityAdsVideoViewController.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAds.h"

@interface UnityAdsViewStateVideoPlayer : UnityAdsViewState <UnityAdsVideoControllerDelegate>
  @property (nonatomic, strong) UnityAdsVideoViewController *videoController;
  @property (nonatomic, assign) BOOL checkIfWatched;

- (void)destroyVideoController;
- (void)createVideoController:(id)targetDelegate;
- (void)dismissVideoController;
- (BOOL)canViewSelectedCampaign;
- (void)startVideoPlayback:(BOOL)createVideoController withDelegate:(id)videoControllerDelegate;
@end
