//
//  UnityAdsViewController.m
//  UnityAdsExample
//
//  Created by bluesun on 7/30/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <UnityAds/UnityAds.h>

#define TEST_LEGACY_IMPACT_API 0

#if !TEST_LEGACY_IMPACT_API

@interface UnityAdsViewController () <UnityAdsDelegate, UITextFieldDelegate>
@end

#else

@interface UnityAdsViewController () <ApplifierImpactDelegate, UITextFieldDelegate>
@end

#endif

@implementation UnityAdsViewController

@synthesize startButton;
@synthesize openButton;
@synthesize optionsButton;
@synthesize optionsView;
@synthesize developerId;
@synthesize optionsId;
@synthesize loadingImage;
@synthesize contentView;

- (void)viewDidLoad {
  [super viewDidLoad];

#if !TEST_LEGACY_IMPACT_API
	[[UnityAds sharedInstance] setDelegate:self];
#else
	[[ApplifierImpact sharedInstance] setDelegate:self];
#endif

  [self.openButton addTarget:self action:@selector(openAds) forControlEvents:UIControlEventTouchUpInside];
  [self.startButton addTarget:self action:@selector(startAds) forControlEvents:UIControlEventTouchUpInside];
  [self.optionsButton addTarget:self action:@selector(openOptions) forControlEvents:UIControlEventTouchUpInside];

  [self.developerId setDelegate:self];
  [self.optionsId setDelegate:self];
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
}

- (void)startAds {
  self.optionsButton.enabled = false;
  [self.optionsButton setAlpha:0.3f];
  self.startButton.enabled = false;
  self.startButton.hidden = true;
  self.openButton.hidden = false;
  self.loadingImage.hidden = false;
  self.optionsView.hidden = true;

  // TEST MODE: Do not use in production apps
#if !TEST_LEGACY_IMPACT_API
  [[UnityAds sharedInstance] setDebugMode:YES];
  [[UnityAds sharedInstance] setTestMode:NO];
#else
  [[ApplifierImpact sharedInstance] setDebugMode:YES];
  [[ApplifierImpact sharedInstance] setTestMode:NO];
#endif

  if (self.developerId.text != nil) {
    NSLog(@"Setting developerId");
    // TEST STUFF, DO NOT USE IN PRODUCTION APPS
#if !TEST_LEGACY_IMPACT_API
    if( [self.developerId.text length] > 0){
        [[UnityAds sharedInstance] setTestMode:YES];
    }
    [[UnityAds sharedInstance] setTestDeveloperId:self.developerId.text];
#else
    if([self.developerId.text length] > 0){
      [[ApplifierImpact sharedInstance] setTestMode:YES];
    }
    [[ApplifierImpact sharedInstance] setTestDeveloperId:self.developerId.text];
#endif
  }

  if (self.optionsId.text != nil) {
    NSLog(@"Setting optionsId");
    // TEST STUFF, DO NOT USE IN PRODUCTION APPS
#if !TEST_LEGACY_IMPACT_API
    [[UnityAds sharedInstance] setTestOptionsId:self.optionsId.text];
#else
    [[ApplifierImpact sharedInstance] setTestOptionsId:self.optionsId.text];
#endif
  }

  // Initialize Unity Ads
#if !TEST_LEGACY_IMPACT_API
	[[UnityAds sharedInstance] startWithGameId:@"16" andViewController:self];
#else
	[[ApplifierImpact sharedInstance] startWithGameId:@"16" andViewController:self];
#endif
}

- (void)openOptions {
  if (self.optionsView.hidden) {
    self.optionsView.hidden = false;
  }
  else {
    self.optionsView.hidden = true;
  }
}

- (void)openAds {
#if !TEST_LEGACY_IMPACT_API
	NSLog(@"canShow: %i",[[UnityAds sharedInstance] canShow]);
  if ([[UnityAds sharedInstance] canShow]) {
#if 0
    NSLog(@"REWARD_ITEM_KEYS: %@", [[UnityAds sharedInstance] getRewardItemKeys]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"SETTING_REWARD_ITEM (wrong): %i", [[UnityAds sharedInstance] setRewardItemKey:@"wrong_key"]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"SETTING_REWARD_ITEM (right): %i", [[UnityAds sharedInstance] setRewardItemKey:[[[UnityAds sharedInstance] getRewardItemKeys] objectAtIndex:0]]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"DEFAULT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getDefaultRewardItemKey]);
#endif
    
     
    NSLog(@"show: %i", [[UnityAds sharedInstance] show:@{
                                                         kUnityAdsOptionNoOfferscreenKey:@true,
                                                         kUnityAdsOptionOpenAnimatedKey:@true,
                                                         kUnityAdsOptionGamerSIDKey:@"gom",
                                                         kUnityAdsOptionMuteVideoSounds:@false,
                                                         kUnityAdsOptionVideoUsesDeviceOrientation:@true
                                                         }]);

#if 0
    NSLog(@"SETTING_REWARD_ITEM (while open): %i", [[UnityAds sharedInstance] setRewardItemKey:[[UnityAds sharedInstance] getDefaultRewardItemKey]]);
    NSLog(@"GETTING_REWARD_ITEM_DETAILS: %@", [[UnityAds sharedInstance] getRewardItemDetailsWithKey:[[UnityAds sharedInstance] getCurrentRewardItemKey]]);
#endif
	}
	else {
    NSLog(@"Unity Ads cannot be shown.");
  }
#else
  NSLog(@"canShowImpact: %i",[[ApplifierImpact sharedInstance] canShowImpact]);
  if ([[ApplifierImpact sharedInstance] canShowImpact]) {
#if 0
    NSLog(@"REWARD_ITEM_KEYS: %@", [[ApplifierImpact sharedInstance] getRewardItemKeys]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[ApplifierImpact sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"SETTING_REWARD_ITEM (wrong): %i", [[ApplifierImpact sharedInstance] setRewardItemKey:@"wrong_key"]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[ApplifierImpact sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"SETTING_REWARD_ITEM (right): %i", [[ApplifierImpact sharedInstance] setRewardItemKey:[[[UnityAds sharedInstance] getRewardItemKeys] objectAtIndex:0]]);
    NSLog(@"CURRENT_REWARD_ITEM: %@", [[ApplifierImpact sharedInstance] getCurrentRewardItemKey]);
    NSLog(@"DEFAULT_REWARD_ITEM: %@", [[ApplifierImpact sharedInstance] getDefaultRewardItemKey]);
#endif

    NSLog(@"showAds: %i", [[ApplifierImpact sharedInstance] showImpact:@{
                                                                         kApplifierImpactOptionNoOfferscreenKey:@true,
                                                                         kApplifierImpactOptionOpenAnimatedKey:@true,
                                                                         kApplifierImpactOptionGamerSIDKey:@"gom",
                                                                         kApplifierImpactOptionMuteVideoSounds:@false,
                                                                         kApplifierImpactOptionVideoUsesDeviceOrientation:@true
                                                                         }]);

#if 0
    NSLog(@"SETTING_REWARD_ITEM (while open): %i", [[ApplifierImpact sharedInstance] setRewardItemKey:[[UnityAds sharedInstance] getDefaultRewardItemKey]]);
     NSLog(@"GETTING_REWARD_ITEM_DETAILS: %@", [[ApplifierImpact sharedInstance] getRewardItemDetailsWithKey:[[UnityAds sharedInstance] getCurrentRewardItemKey]]);
#endif
	}
	else {
    NSLog(@"Unity Ads cannot be shown.");
  }
#endif
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return YES;
}

- (NSUInteger) supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  return YES;
}


#pragma mark - UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
  NSLog(@"textFieldShouldReturn");
  [textField resignFirstResponder];
  return YES;
}

#if !TEST_LEGACY_IMPACT_API

#pragma mark - UnityAdsDelegate

- (void)unityAdsFetchCompleted {
	NSLog(@"unityAdsFetchCompleted");
  [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
	[self.openButton setEnabled:YES];
  [self.instructionsText setText:@"Press \"Open\" to show Unity Ads"];
}

- (void)unityAdsWillShow {
	NSLog(@"unityAdsWillShow");
}

- (void)unityAdsDidShow {
	NSLog(@"unityAdsDidShow");
}

- (void)unityAdsWillHide {
	NSLog(@"unityAdsWillHide");
}

- (void)unityAdsDidHide {
	NSLog(@"unityAdsDidHide");
}

- (void)unityAdsVideoStarted {
	NSLog(@"unityAdsVideoStarted");
}

- (void)unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped {
	NSLog(@"unityAdsVideoCompleted:rewardItemKey:skipped -- key: %@ -- skipped: %@", rewardItemKey, skipped ? @"true" : @"false");
}

#else

#pragma mark - ApplifierImpactDelegate

- (void)applifierImpactCampaignsAreAvailable:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactCampaignsAreAvailable");
  [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
  [self.openButton setEnabled:YES];
  [self.instructionsText setText:@"Press \"Open\" to show Unity Ads"];
}

- (void)applifierImpactWillOpen:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactWillOpen");
}

- (void)applifierImpactDidOpen:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactDidOpen");
}

- (void)applifierImpactWillClose:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactWillClose");
}

- (void)applifierImpactDidClose:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactDidClose");
}

- (void)applifierImpactVideoStarted:(ApplifierImpact *)applifierImpact {
  NSLog(@"applifierImpactVideoStarted");
}

- (void)applifierImpact:(ApplifierImpact *)applifierImpact completedVideoWithRewardItemKey:(NSString *)rewardItemKey videoWasSkipped:(BOOL)skipped {
  NSLog(@"applifierImpact:completedVideoWithRewardItem:videoWasSkipped: -- key: %@ -- skipped: %@", rewardItemKey, skipped ? @"true" : @"false");
  [self.loadingImage setImage:[UIImage imageNamed:@"unityads_reward"]];
}

#endif

@end
