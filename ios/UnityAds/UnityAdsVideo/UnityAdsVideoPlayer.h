//
//  UnityAdsVideo.h
//  UnityAds
//
//  Created by bluesun on 10/22/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>

typedef enum
{
	kVideoAnalyticsPositionUnplayed = -1,
	kVideoAnalyticsPositionStart = 0,
	kVideoAnalyticsPositionFirstQuartile = 1,
	kVideoAnalyticsPositionMidPoint = 2,
	kVideoAnalyticsPositionThirdQuartile = 3,
	kVideoAnalyticsPositionEnd = 4,
} VideoAnalyticsPosition;

@protocol UnityAdsVideoPlayerDelegate <NSObject>

@required
- (void)videoStartedPlaying;
- (void)videoPlaybackEnded:(BOOL)skipped;
- (void)videoPlaybackError;
- (void)videoPlaybackStarted;
- (void)videoPlaybackStalled;
- (void)videoPositionChanged:(CMTime)time;
@end

@interface UnityAdsVideoPlayer : AVPlayer
@property (nonatomic, assign) id<UnityAdsVideoPlayerDelegate> delegate;
@property (nonatomic, strong) AVPlayerLayer *playerLayer;
- (void)playSelectedVideo;
- (void)preparePlayer;
- (void)clearPlayer;
@end
