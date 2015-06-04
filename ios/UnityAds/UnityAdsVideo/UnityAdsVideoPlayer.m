//
//  UnityAdsVideo.m
//  UnityAds
//
//  Created by bluesun on 10/22/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsVideoPlayer.h"
#import "../UnityAds.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"
#import "../UnityAdsDevice/UnityAdsDevice.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "UnityAdsCacheManager.h"

@interface UnityAdsVideoPlayer ()
  @property (nonatomic, assign) id timeObserver;
  @property (nonatomic, assign) id analyticsTimeObserver;
  @property (nonatomic, assign) NSTimer *timeOutTimer;
  @property (nonatomic) VideoAnalyticsPosition videoPosition;
  @property (nonatomic, assign) BOOL isPlaying;
  @property (nonatomic, assign) BOOL hasPlayed;
  @property (nonatomic, assign) BOOL hasMoved;
  @property (nonatomic, strong) NSTimer *videoProgressMonitor;
  @property (nonatomic, assign) CMTime lastUpdate;
@end

@implementation UnityAdsVideoPlayer

@synthesize timeOutTimer = _timeOutTimer;

- (void)preparePlayer {
  self.isPlaying = false;
  self.hasPlayed = false;
  self.hasMoved = false;
  [self _addObservers];
}

- (void)clearPlayer {
  [self pause];
  self.isPlaying = false;
  self.hasPlayed = false;
  self.hasMoved = false;
  [self _removeObservers];
}



- (void)dealloc {
  UALOG_DEBUG(@"dealloc");
}

#pragma mark Video Playback

- (void) muteVideo {
}

- (void)playSelectedVideo {
  self.videoPosition = kVideoAnalyticsPositionUnplayed;
  [[UnityAdsCampaignManager sharedInstance] selectedCampaign].videoBufferingStartTime = [[NSDate date] timeIntervalSince1970] * 1000;
}

- (void)_videoPlaybackEnded:(NSNotification *)notification {
  UALOG_DEBUG(@"");
  if ([UnityAdsDevice isSimulator]) {
    self.videoPosition = kVideoAnalyticsPositionThirdQuartile;
  }
  
  [self _logVideoAnalytics];
  [self clearVideoProgressMonitor];

  dispatch_async(dispatch_get_main_queue(), ^{
    self.hasPlayed = true;
    self.isPlaying = false;
    [self.delegate videoPlaybackEnded:FALSE];
  });
}


#pragma mark Video Observers

- (void)checkIfPlayed {
  UALOG_DEBUG(@"");
  
  if (!self.hasPlayed && !self.isPlaying) {
    UALOG_DEBUG(@"Video hasn't played and video is not playing! Seems that video is timing out.");
    [self clearTimeOutTimer];
    [self clearVideoProgressMonitor];
    [self.delegate videoPlaybackError];
  }
}

- (void)_addObservers {
  
  dispatch_async(dispatch_get_main_queue(), ^{
    [self addObserver:self forKeyPath:@"currentItem.status" options:0 context:nil];
  });
 
  __block UnityAdsVideoPlayer *blockSelf = self;
    self.timeObserver = [self addPeriodicTimeObserverForInterval:CMTimeMakeWithSeconds(1, NSEC_PER_SEC) queue:nil usingBlock:^(CMTime time) {
      [blockSelf _videoPositionChanged:time];
    }];  
  
  self.timeOutTimer = [NSTimer scheduledTimerWithTimeInterval:25 target:self selector:@selector(checkIfPlayed) userInfo:nil repeats:false];
  
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_videoPlaybackEnded:) name:AVPlayerItemDidPlayToEndTimeNotification object:self.currentItem];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_handleAudioSessionInterruption:) name:AVAudioSessionInterruptionNotification object:[AVAudioSession sharedInstance]];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_handleMediaServicesReset) name:AVAudioSessionMediaServicesWereResetNotification object:[AVAudioSession sharedInstance]];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_handleApplicationDidBecomeActive:) name:UIApplicationDidBecomeActiveNotification object:[UIApplication sharedApplication]];
}

- (void)clearTimeOutTimer {
  if (self.timeOutTimer != nil) {
    [self.timeOutTimer invalidate];
    self.timeOutTimer = nil;
  }
}

- (void)clearVideoProgressMonitor {
  if(self.videoProgressMonitor != nil) {
    [self.videoProgressMonitor invalidate];
    self.videoProgressMonitor = nil;
  }
}

- (void)_removeObservers {
  UALOG_DEBUG(@"");
  UAAssert([NSThread isMainThread]);
  [[NSNotificationCenter defaultCenter] removeObserver:self name:AVPlayerItemDidPlayToEndTimeNotification object:nil];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionInterruptionNotification object:nil];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionMediaServicesWereResetNotification object:nil];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
  
  if (self.timeObserver != nil) {
    [self removeTimeObserver:self.timeObserver];
    self.timeObserver = nil;
  }
	
  if (self.analyticsTimeObserver != nil) {
    [self removeTimeObserver:self.analyticsTimeObserver];
    self.analyticsTimeObserver = nil;
  }
  
  [self clearTimeOutTimer];
  [self clearVideoProgressMonitor];

  dispatch_async(dispatch_get_main_queue(), ^{
    [self removeObserver:self forKeyPath:@"currentItem.status"];
  });
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
  if ([keyPath isEqual:@"currentItem.status"]) {
    UALOG_DEBUG(@"VIDEOPLAYERITEM_STATUS: %li", (long)self.currentItem.status);
    
    AVPlayerItemStatus playerItemStatus = self.currentItem.status;
    if (playerItemStatus == AVPlayerItemStatusReadyToPlay) {
      UALOG_DEBUG(@"videostartedplaying");
      __block UnityAdsVideoPlayer *blockSelf = self;
      
      self.lastUpdate = kCMTimeInvalid;
      [self clearVideoProgressMonitor];
      self.videoProgressMonitor = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(_videoProgressMonitor:) userInfo:nil repeats:YES];
      
      Float64 duration = [self _currentVideoDuration];
      NSMutableArray *analyticsTimeValues = [NSMutableArray array];
      [analyticsTimeValues addObject:[self _valueWithDuration:duration * .25]];
      [analyticsTimeValues addObject:[self _valueWithDuration:duration * .5]];
      [analyticsTimeValues addObject:[self _valueWithDuration:duration * .75]];
      
      if (![UnityAdsDevice isSimulator]) {
        self.analyticsTimeObserver = [self addBoundaryTimeObserverForTimes:analyticsTimeValues queue:nil usingBlock:^{
          UALOG_DEBUG(@"Log position");
          [blockSelf _logVideoAnalytics];
        }];
      }
      
      dispatch_async(dispatch_get_main_queue(), ^{
        self.hasPlayed = false;
        self.isPlaying = false;
        [self.delegate videoStartedPlaying];
        [self _logVideoAnalytics];
      });
      
      [self play];
      
      [[UnityAdsCampaignManager sharedInstance] selectedCampaign].videoBufferingEndTime = [[NSDate date] timeIntervalSince1970] * 1000;
      long long bufferingCompleted = [[UnityAdsCampaignManager sharedInstance] selectedCampaign].videoBufferingEndTime - [[UnityAdsCampaignManager sharedInstance] selectedCampaign].videoBufferingStartTime;
      
    }
    else if (playerItemStatus == AVPlayerItemStatusFailed) {
      UALOG_DEBUG(@"Player failed");
      dispatch_async(dispatch_get_main_queue(), ^{
        self.hasPlayed = false;
        self.isPlaying = false;
        [self.delegate videoPlaybackError];
        [self clearTimeOutTimer];
        [self clearVideoProgressMonitor];
      });
    }
    else if (playerItemStatus == AVPlayerItemStatusUnknown) {
      UALOG_DEBUG(@"Player in unknown state");
    }
  } else {
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
  }
}

#pragma mark Audio notifications

- (void)_handleAudioSessionInterruption:(NSNotification *)notification {
  NSNumber *interruptionType = [[notification userInfo] objectForKey:AVAudioSessionInterruptionTypeKey];
  NSNumber *interruptionOption = [[notification userInfo] objectForKey:AVAudioSessionInterruptionOptionKey];
  
  switch (interruptionType.unsignedIntegerValue) {
    case AVAudioSessionInterruptionTypeBegan:
      UALOG_DEBUG(@"Audio session interruption began");
      break;
      
    case AVAudioSessionInterruptionTypeEnded:
      if (interruptionOption.unsignedIntegerValue == AVAudioSessionInterruptionOptionShouldResume) {
        UALOG_DEBUG(@"Resuming video playback after audio session interrupt");
        [self play];
      } else {
        UALOG_DEBUG(@"Ending video playback after audio session interrupt");
        [self _videoPlaybackEnded:nil];
      }
      break;
      
    default:
      break;
  }
}

- (void)_handleMediaServicesReset {
  UALOG_DEBUG(@"Media services reset");
  [self _videoPlaybackEnded:nil];
}

#pragma mark Application lifecycle notifications

- (void)_handleApplicationDidBecomeActive:(UIApplication*)application {
  if(self.isPlaying) {
    [self play];
  }
}

#pragma mark Video progress

- (void)_videoProgressMonitor:(NSTimer *)timer {
  if(CMTIME_IS_INVALID(self.lastUpdate)) {
    self.lastUpdate = self.currentTime;
  } else {
    Float64 difference = CMTimeGetSeconds(CMTimeSubtract(self.currentTime, self.lastUpdate));
    UALOG_DEBUG(@"VIDEO MOVED: %f", difference);
    if(difference <= 0.001) {
      UALOG_DEBUG(@"VIDEO STALLED!");
      [self.delegate videoPlaybackStalled];
    } else {
      if(!self.hasMoved) {
        [self clearTimeOutTimer];
        self.hasMoved = true;
        self.isPlaying = true;
      }
      [self.delegate videoPlaybackStarted];
    }
    self.lastUpdate = self.currentTime;
  }
}

#pragma mark Video Duration

- (void)_videoPositionChanged:(CMTime)time {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.delegate videoPositionChanged:time];
  });
}

- (Float64)_currentVideoDuration {
	CMTime durationTime = self.currentItem.asset.duration;
	Float64 duration = CMTimeGetSeconds(durationTime);
	
	return duration;
}

- (NSValue *)_valueWithDuration:(Float64)duration {
	CMTime time = CMTimeMakeWithSeconds(duration, NSEC_PER_SEC);
	return [NSValue valueWithCMTime:time];
}


#pragma mark Analytics

- (void)_logVideoAnalytics {
  UALOG_DEBUG(@"_logVideoAnalytics");
	self.videoPosition++;
  UnityAdsCampaign *campaign = [[UnityAdsCampaignManager sharedInstance] selectedCampaign];
  BOOL cached = [[UnityAdsCacheManager sharedInstance] is:ResourceTypeTrailerVideo cachedForCampaign:campaign];
  [[UnityAdsAnalyticsUploader sharedInstance] logVideoAnalyticsWithPosition:self.videoPosition campaignId:campaign.id viewed:campaign.viewed cached:cached];
}

@end