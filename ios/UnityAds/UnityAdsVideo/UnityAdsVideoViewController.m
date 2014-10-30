//
//  UnityAdsVideoViewController.m
//  UnityAds
//
//  Created by bluesun on 11/26/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "../UnityAds.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "UnityAdsVideoViewController.h"
#import "UnityAdsVideoPlayer.h"
#import "UnityAdsVideoView.h"
#import "UnityAdsProperties.h"
#import "UnityAdsVideoMuteButton.h"
#import "UnityAdsBundle.h"
#import "UnityAdsMainViewController.h"
#import "UnityAdsZoneManager.h"

@interface UnityAdsVideoViewController () {
@protected
  BOOL _routeChanged;
}
@property (nonatomic, strong) UnityAdsVideoView *videoView;
@property (nonatomic, strong) UnityAdsVideoPlayer *videoPlayer;
@property (nonatomic, weak) UnityAdsCampaign *campaignToPlay;
@property (nonatomic, strong) UILabel *bufferingLabel;
@property (nonatomic, strong) UILabel *progressLabel;
@property (nonatomic, strong) UIButton *skipLabel;
@property (nonatomic, strong) UIView *videoOverlayView;
@property (nonatomic, strong) NSURL *currentPlayingVideoUrl;
@property (nonatomic, strong) UnityAdsVideoMuteButton *muteButton;
@property (nonatomic, strong) UITapGestureRecognizer *tapGestureRecognizer;
@property (nonatomic, strong) UILabel *stagingLabel;


@end

@implementation UnityAdsVideoViewController

@synthesize muteButton = _muteButton;
@synthesize isMuted = _isMuted;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  if (self) {
    self.tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTapFrom:)];
    self.isPlaying = NO;
    self.isMuted = NO;
    _routeChanged = false;
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}


-(void)routeChanged:(id)object {
  _routeChanged = true;
}

- (void)viewDidLoad {
  [super viewDidLoad];
  
  
  NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
  [nc addObserver:self
         selector:@selector(routeChanged:)
             name:AVAudioSessionRouteChangeNotification
           object:[AVAudioSession sharedInstance]];
  
  [self.view setBackgroundColor:[UIColor blackColor]];
  self.view.clipsToBounds = true;
  
  if (self.delegate != nil) {
    [self.delegate videoPlayerReady];
  }
  self.tapGestureRecognizer.cancelsTouchesInView = NO;
  [self.view addGestureRecognizer:self.tapGestureRecognizer];
  self.tapGestureRecognizer.delegate = self;
  
  
  [self _attachVideoView];
}

- (void) handleTapFrom: (UITapGestureRecognizer *)recognizer
{
  // TODO: Show controlls
  [self showOverlay];
  UALOG_DEBUG(@"SHOW CONTROLLS");
}

- (void)viewDidDisappear:(BOOL)animated {
  [self _detachVideoPlayer];
  [self _detachVideoView];
  [self _destroyVideoPlayer];
  [self _destroyVideoView];
  
  [self destroyProgressLabel];
  [self destroyBufferingLabel];
  [self destroyVideoSkipLabel];
  if([[UnityAdsProperties sharedInstance] unityDeveloperInternalTestMode] == TRUE) {
    [self destroyStagingLabel];
  }
  [self destroyVideoOverlayView];
  
  [super viewDidDisappear:animated];
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  [self _makeOrientation];
  
  [self createVideoOverlayView];
  [self createProgressLabel];
  [self createBufferingLabel];
  [self createVideoSkipLabel];
  [self createMuteButton];
  if([[UnityAdsProperties sharedInstance] unityDeveloperInternalTestMode] == TRUE) {
    [self createStagingLabel];
  }
  
  [self.view bringSubviewToFront:self.videoOverlayView];
}

- (void)_makeOrientation {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if (![currentZone useDeviceOrientationForVideo]) {
    if (UIInterfaceOrientationIsPortrait([UIApplication sharedApplication].statusBarOrientation)) {
      double maxValue = fmax(self.view.superview.bounds.size.width, self.view.superview.bounds.size.height);
      double minValue = fmin(self.view.superview.bounds.size.width, self.view.superview.bounds.size.height);
      self.view.bounds = CGRectMake(0, 0, maxValue, minValue);
      self.view.transform = CGAffineTransformMakeRotation(M_PI / 2);
      UALOG_DEBUG(@"NEW DIMENSIONS: %f, %f", minValue, maxValue);
    }
  }
  
  [self.muteButton setFrame:CGRectMake(0.0f, self.view.bounds.size.height - self.muteButton.bounds.size.height + 16, self.muteButton.frame.size.width, self.muteButton.frame.size.height)];
  UALOG_DEBUG("Mutebutton frame: %f x %f - %f x %f",self.muteButton.frame.size.height,self.muteButton.frame.size.width,self.muteButton.frame.origin.x,self.muteButton.frame.origin.y);
  
  // Position in lower left corner.
  
  if (self.videoView != nil) {
    [self.videoView setFrame:self.view.bounds];
  }
  
}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone useDeviceOrientationForVideo]) {
    return YES;
  }
  return UIInterfaceOrientationIsLandscape(interfaceOrientation);
}

- (NSUInteger)supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone useDeviceOrientationForVideo]) {
    return YES;
  }
  return NO;
}

- (BOOL)prefersStatusBarHidden {
  return YES;
}

#pragma mark - Public

- (void)playCampaign:(UnityAdsCampaign *)campaignToPlay {
  UALOG_DEBUG(@"");
  NSURL *videoURL = [[UnityAdsCampaignManager sharedInstance] getVideoURLForCampaign:campaignToPlay];
  
  if (videoURL == nil) {
    UALOG_DEBUG(@"Video not found!");
    return;
  }
  
  self.campaignToPlay = campaignToPlay;
  self.currentPlayingVideoUrl = videoURL;
  
  AVURLAsset *asset = [AVURLAsset URLAssetWithURL:self.currentPlayingVideoUrl options:nil];
  AVMutableAudioMix *audioZeroMix = nil;
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone muteVideoSounds]) {
    NSArray *audioTracks = [asset tracksWithMediaType:AVMediaTypeAudio];
    NSMutableArray *allAudioParams = [NSMutableArray array];
    
    for (AVAssetTrack *track in audioTracks) {
      AVMutableAudioMixInputParameters *audioInputParams = [AVMutableAudioMixInputParameters audioMixInputParameters];
      [audioInputParams setVolume:0.0 atTime:kCMTimeZero];
      [audioInputParams setTrackID:[track trackID]];
      [allAudioParams addObject:audioInputParams];
    }
    
    audioZeroMix = [AVMutableAudioMix audioMix];
    [audioZeroMix setInputParameters:allAudioParams];
  }
  
  AVPlayerItem *item = [AVPlayerItem playerItemWithAsset:asset];
  
  if ([currentZone muteVideoSounds]) {
    [item setAudioMix:audioZeroMix];
  }
  
  [self _createVideoView];
  [self _createVideoPlayer];
  [self _attachVideoPlayer];
  [self.videoPlayer preparePlayer];
  
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.videoPlayer replaceCurrentItemWithPlayerItem:item];
    [self.videoPlayer playSelectedVideo];
  });
}


#pragma mark - Video View

- (void)_createVideoView {
  if (self.videoView == nil) {
    self.videoView = [[UnityAdsVideoView alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [self.videoView setVideoFillMode:AVLayerVideoGravityResizeAspect];
    self.videoView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
  }
}

- (void)_attachVideoView {
  if (self.videoView != nil && ![self.videoView.superview isEqual:self.view]) {
    [self.view addSubview:self.videoView];
  }
}

- (void)_detachVideoView {
  if (self.videoView != nil && self.videoView.superview != nil) {
    [self.videoView removeFromSuperview];
  }
}

- (void)_destroyVideoView {
  if (self.videoView != nil) {
    [self _detachVideoView];
    self.videoView = nil;
  }
}


#pragma mark - Video player

- (void)forceStopVideoPlayer {
  UALOG_DEBUG(@"");
  // FIX: Wrong order?
  [self _detachVideoPlayer];
  [self _destroyVideoPlayer];
}

- (void)_createVideoPlayer {
  if (self.videoPlayer == nil) {
    UALOG_DEBUG(@"");
    self.videoPlayer = [[UnityAdsVideoPlayer alloc] initWithPlayerItem:nil];
    self.videoPlayer.delegate = self;
  }
}

- (void)_attachVideoPlayer {
  if (self.videoView != nil) {
    [self.videoView setPlayer:self.videoPlayer];
  }
}

- (void)_destroyVideoPlayer {
  if (self.videoPlayer != nil) {
    UALOG_DEBUG(@"");
    self.currentPlayingVideoUrl = nil;
    [self.videoPlayer clearPlayer];
    self.videoPlayer.delegate = nil;
    self.videoPlayer = nil;
  }
}

- (void)_detachVideoPlayer {
  [self.videoView setPlayer:nil];
}

- (void)videoPositionChanged:(CMTime)time {
  [self updateLabelsWithCMTime:time];
}

- (void)videoStartedPlaying {
  UALOG_DEBUG(@"");
  self.isPlaying = YES;
  self.bufferingLabel.hidden = YES;
  [self.delegate videoPlayerStartedPlaying];
  [self showMuteButton];
}

- (void)videoPlaybackEnded:(BOOL)skipped {
  UALOG_DEBUG(@"");
  [self.delegate videoPlayerPlaybackEnded:skipped];
  self.isPlaying = NO;
  self.campaignToPlay = nil;
}

- (void)videoPlaybackError {
  UALOG_DEBUG(@"");
  [self.delegate videoPlayerEncounteredError];
  self.isPlaying = NO;
}

- (void)videoPlaybackStarted {
  UALOG_DEBUG(@"");
  self.bufferingLabel.hidden = YES;
  UnityAdsZone * currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone allowVideoSkipInSeconds] == 0) {
    self.skipLabel.hidden = YES;
  }
  self.stagingLabel.hidden = NO;
  [self hideOverlayAfter:3.0f];
}

- (void)videoPlaybackStalled {
  UALOG_DEBUG(@"");
  if (_routeChanged) {
    [self.videoPlayer play];
    if (!self.isMuted)
    [self muteVideoButtonPressed:nil];
    _routeChanged = false;
  }
  self.bufferingLabel.hidden = NO;
  [self showVideoSkipLabel];
  [self showOverlay];
}

#pragma mark - Video Overlay View

- (void)createVideoOverlayView {
  if (self.videoOverlayView == nil) {
    self.videoOverlayView = [[UIView alloc] initWithFrame:self.view.bounds];
    [self.videoOverlayView setBackgroundColor:[UIColor clearColor]];
    self.videoOverlayView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    
    [self.view addSubview:self.videoOverlayView];
    [self.view bringSubviewToFront:self.videoOverlayView];
  }
}

- (void)destroyVideoOverlayView {
  if (self.videoOverlayView != nil) {
    [self.videoOverlayView removeFromSuperview];
    self.videoOverlayView = nil;
  }
}


#pragma mark - Video Skip Label

- (void)createVideoSkipLabel {
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if (self.skipLabel == nil && self.videoOverlayView != nil && [currentZone allowVideoSkipInSeconds] > 0) {
    UALOG_DEBUG(@"Create video skip label");
    self.skipLabel = [[UIButton alloc] initWithFrame:CGRectMake(3, 0, 300, 20)];
    self.skipLabel.backgroundColor = [UIColor clearColor];
    self.skipLabel.titleLabel.textColor = [UIColor whiteColor];
    self.skipLabel.titleLabel.font = [UIFont systemFontOfSize:12.0];
    self.skipLabel.titleLabel.textAlignment = UITextAlignmentLeft;
    [self.skipLabel setTitleShadowColor:[UIColor blackColor] forState:UIControlStateNormal];
    [self.skipLabel setContentHorizontalAlignment:UIControlContentHorizontalAlignmentLeft];
    self.skipLabel.titleLabel.shadowColor = [UIColor blackColor];
    self.skipLabel.titleLabel.shadowOffset = CGSizeMake(0, 1.0);
    //self.skipLabel.transform = CGAffineTransformMakeTranslation(self.view.bounds.size.width - 303, self.view.bounds.size.height - 23);
    
    [self.skipLabel addTarget:self action:@selector(skipButtonPressed) forControlEvents:UIControlEventTouchUpInside];
    self.skipLabel.enabled = NO;
    self.skipLabel.hidden = YES;
    
    [self.videoOverlayView addSubview:self.skipLabel];
    [self.videoOverlayView bringSubviewToFront:self.skipLabel];
    self.videoOverlayView.hidden = NO;
  }
}

- (void)showVideoSkipLabel {
  [self.skipLabel setTitle:@"Skip Video" forState:UIControlStateNormal];
  self.skipLabel.enabled = YES;
  self.skipLabel.hidden = NO;
}

- (void)createMuteButton {
  self.muteButton = [[UnityAdsVideoMuteButton alloc] initWithIcon:[UnityAdsBundle imageWithName:@"audio_on" ofType:@"png"] title:@""];
  [self.muteButton setImage:[UnityAdsBundle imageWithName:@"audio_mute" ofType:@"png"] forState:UIControlStateSelected];
  [self.muteButton addTarget:self action:@selector(muteVideoButtonPressed:) forControlEvents:UIControlEventTouchDown];
  [self.muteButton setFrame:CGRectMake(0.0f, self.view.bounds.size.height - self.muteButton.bounds.size.height + 16, self.muteButton.frame.size.width, self.muteButton.frame.size.height)];
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone muteVideoSounds]) {
    self.isMuted = true;
    self.muteButton.selected = self.isMuted;
  }
}

- (void)showMuteButton {
  [self.muteButton setFrame:CGRectMake(0.0f, self.view.bounds.size.height - self.muteButton.bounds.size.height + 16, self.muteButton.frame.size.width, self.muteButton.frame.size.height)];
  [self.videoOverlayView addSubview:self.muteButton];
  [self.videoOverlayView bringSubviewToFront:self.muteButton];
}

- (void)muteVideoButtonPressed:(id)sender {
  AVPlayerItem *item = [self.videoPlayer currentItem];
  AVMutableAudioMix *audioZeroMix = nil;
  NSArray *audioTracks = [item.asset tracksWithMediaType:AVMediaTypeAudio];
  NSMutableArray *allAudioParams = [NSMutableArray array];
  
  for (AVAssetTrack *track in audioTracks) {
    AVMutableAudioMixInputParameters *audioInputParams = [AVMutableAudioMixInputParameters audioMixInputParameters];
    [audioInputParams setVolume:!self.isMuted ? 0.0f : 1.0f atTime:kCMTimeZero];
    [audioInputParams setTrackID:[track trackID]];
    [allAudioParams addObject:audioInputParams];
  }
  
  audioZeroMix = [AVMutableAudioMix audioMix];
  [audioZeroMix setInputParameters:allAudioParams];
  [item setAudioMix:audioZeroMix];
  self.isMuted = !self.isMuted;
  self.muteButton.selected = self.isMuted;
}

- (void)destroyVideoSkipLabel {
  if (self.skipLabel != nil) {
    [self.skipLabel removeFromSuperview];
    self.skipLabel = nil;
  }
}



- (void)skipButtonPressed {
  UALOG_DEBUG(@"");
  [self videoPlaybackEnded:TRUE];
  [[UnityAdsMainViewController sharedInstance] applyOptionsToCurrentState:@{@"sendAbortInstrumentation":@true, @"type":kUnityAdsGoogleAnalyticsEventVideoAbortSkip}];
}

#pragma mark - Video Buffering Label

- (void)createBufferingLabel {
  UALOG_DEBUG(@"");
  if(self.bufferingLabel == nil && self.videoOverlayView != nil) {
    self.bufferingLabel = [[UILabel alloc] initWithFrame:CGRectMake(self.view.bounds.size.width - 303, 0, 300, 20)];
    self.bufferingLabel.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin;
    self.bufferingLabel.backgroundColor = [UIColor clearColor];
    self.bufferingLabel.textColor = [UIColor whiteColor];
    self.bufferingLabel.font = [UIFont systemFontOfSize:12.0];
    self.bufferingLabel.textAlignment = UITextAlignmentRight;
    self.bufferingLabel.shadowColor = [UIColor blackColor];
    self.bufferingLabel.shadowOffset = CGSizeMake(0, 1.0);
    self.bufferingLabel.text = @"Buffering...";
    self.bufferingLabel.hidden = YES;
    
    [self.videoOverlayView addSubview:self.bufferingLabel];
    [self.videoOverlayView bringSubviewToFront:self.bufferingLabel];
    
    self.videoOverlayView.hidden = NO;
  }
}

- (void)destroyBufferingLabel {
  if(self.bufferingLabel != nil) {
    [self.bufferingLabel removeFromSuperview];
    self.bufferingLabel = nil;
  }
}

#pragma mark - Staging label

- (void)createStagingLabel {
  if(self.stagingLabel == nil && self.videoOverlayView != nil) {
    self.stagingLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, self.view.bounds.size.height)];
    self.stagingLabel.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin;
    self.stagingLabel.backgroundColor = [UIColor clearColor];
    self.stagingLabel.textColor = [UIColor redColor];
    self.stagingLabel.font = [UIFont systemFontOfSize:12.0];
    self.stagingLabel.textAlignment = UITextAlignmentCenter;
    self.stagingLabel.numberOfLines = 2;
    self.stagingLabel.text = @"INTERNAL UNITY TEST BUILD\nDO NOT USE IN PRODUCTION";
    self.stagingLabel.hidden = YES;
    
    [self.videoOverlayView addSubview:self.stagingLabel];
    [self.videoOverlayView bringSubviewToFront:self.stagingLabel];
    
    self.videoOverlayView.hidden = NO;
  }
}

- (void)destroyStagingLabel {
  if(self.stagingLabel != nil) {
    [self.stagingLabel removeFromSuperview];
    self.stagingLabel = nil;
  }
}

#pragma mark - Video Progress Label

- (void)createProgressLabel {
  UALOG_DEBUG(@"");
  
  if (self.progressLabel == nil && self.videoOverlayView != nil) {
    self.progressLabel = [[UILabel alloc] initWithFrame:CGRectMake(self.view.bounds.size.width - 303, self.view.bounds.size.height - 23, 300, 20)];
    self.progressLabel.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin;
    self.progressLabel.backgroundColor = [UIColor clearColor];
    self.progressLabel.textColor = [UIColor whiteColor];
    self.progressLabel.font = [UIFont systemFontOfSize:12.0];
    self.progressLabel.textAlignment = UITextAlignmentRight;
    self.progressLabel.shadowColor = [UIColor blackColor];
    self.progressLabel.shadowOffset = CGSizeMake(0, 1.0);
    
    [self.videoOverlayView addSubview:self.progressLabel];
    [self.videoOverlayView bringSubviewToFront:self.progressLabel];
    
    self.videoOverlayView.hidden = NO;
  }
}

- (void)destroyProgressLabel {
  if (self.progressLabel != nil) {
    [self.progressLabel removeFromSuperview];
    self.progressLabel = nil;
  }
}

- (void)updateLabelsWithCMTime:(CMTime)currentTime {
  Float64 duration = [self _currentVideoDuration];
  Float64 current = CMTimeGetSeconds(currentTime);
  Float64 timeLeft = duration - current;
  Float64 timeUntilSkip = -1;
  
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if ([currentZone allowVideoSkipInSeconds] > 0) {
    timeUntilSkip = [currentZone allowVideoSkipInSeconds] - current;
  }
  
  if (timeLeft < 0)
    timeLeft = 0;
  
  if (timeUntilSkip > -1) {
    if (timeUntilSkip < 0)
      timeUntilSkip = 0;
    
    NSString *skipText = [NSString stringWithFormat:NSLocalizedString(@"You can skip this video in %.0f seconds.", nil), timeUntilSkip];
    self.skipLabel.enabled = NO;
    self.skipLabel.hidden = NO;
    
    if (timeUntilSkip == 0) {
      skipText = [NSString stringWithFormat:@"Skip Video"];
      [self hideOverlayAfter:3.0f];
      self.skipLabel.enabled = YES;
    }
    
    if (self.skipLabel != nil) {
      [self.skipLabel setTitle:skipText forState:UIControlStateNormal];
      [self.skipLabel setTitle:skipText forState:UIControlStateDisabled];
    }
  } else {
    [self hideOverlayAfter:3.0f];
  }
  
  NSString *descriptionText = [NSString stringWithFormat:NSLocalizedString(@"This video ends in %.0f seconds.", nil), timeLeft];
  self.progressLabel.text = descriptionText;
}

- (Float64)_currentVideoDuration {
  CMTime durationTime = self.videoPlayer.currentItem.asset.duration;
  Float64 duration = CMTimeGetSeconds(durationTime);
  
  return duration;
}

- (NSValue *)_valueWithDuration:(Float64)duration {
  CMTime time = CMTimeMakeWithSeconds(duration, NSEC_PER_SEC);
  return [NSValue valueWithCMTime:time];
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch {
  if ([touch.view isKindOfClass:[UIControl class]]) {
    return NO; // ignore the touch
  }
  return YES; // handle the touch
}


- (void) showOverlay {
  [UIView beginAnimations:nil context:nil];
  [UIView setAnimationDuration:1.0];
  [self.videoOverlayView setAlpha:1.0f];
  [UIView commitAnimations];
}

- (void) hideOverlay {
  [UIView beginAnimations:nil context:nil];
  [UIView setAnimationDuration:1.0];
  [self.videoOverlayView setAlpha:0.0f];
  [UIView commitAnimations];
}

- (void) hideOverlayAfter:(CGFloat)seconds {
  // do not double fire.
  if(self.videoOverlayView.alpha == 1.0f) {
    self.videoOverlayView.alpha = 0.99999f;
    [self performSelector:@selector(hideOverlay) withObject:nil afterDelay:seconds];
  }
}


@end