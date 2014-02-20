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

@interface UnityAdsViewController () <UnityAdsDelegate, UITextFieldDelegate>
@end

@implementation UnityAdsViewController

@synthesize startButton;
@synthesize openButton;
@synthesize optionsButton;
@synthesize optionsView;
@synthesize developerId;
@synthesize optionsId;
@synthesize loadingImage;
@synthesize contentView;
@synthesize webviewSwitch;


- (void)viewDidLoad {
    [super viewDidLoad];
	
	[[UnityAds sharedInstance] setDelegate:self];
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
    [[UnityAds sharedInstance] setDebugMode:YES];
    //[[UnityAds sharedInstance] setTestMode:YES];
    
    if (self.developerId.text != nil) {
        UALOG_DEBUG(@"Setting developerId");
        // TEST STUFF, DO NOT USE IN PRODUCTION APPS
        [[UnityAds sharedInstance] setTestDeveloperId:self.developerId.text];
    }
    
    if (self.optionsId.text != nil) {
        UALOG_DEBUG(@"Setting optionsId");
        // TEST STUFF, DO NOT USE IN PRODUCTION APPS
        [[UnityAds sharedInstance] setTestOptionsId:self.optionsId.text];
    }
    
    if (!self.webviewSwitch.isOn) {
        [[UnityAds sharedInstance] setAdsMode:kUnityAdsModeNoWebView];
    }
    
    // Initialize Unity Ads
	[[UnityAds sharedInstance] startWithGameId:@"16" andViewController:self];
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
	NSLog(@"canShow: %i",[[UnityAds sharedInstance] canShow]);
    if ([[UnityAds sharedInstance] canShow]) {
        /*
        NSLog(@"REWARD_ITEM_KEYS: %@", [[UnityAds sharedInstance] getRewardItemKeys]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"SETTING_REWARD_ITEM (wrong): %i", [[UnityAds sharedInstance] setRewardItemKey:@"wrong_key"]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"SETTING_REWARD_ITEM (right): %i", [[UnityAds sharedInstance] setRewardItemKey:[[[UnityAds sharedInstance] getRewardItemKeys] objectAtIndex:0]]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"DEFAULT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getDefaultRewardItemKey]); */

        //[[UnityAds sharedInstance] setViewController:self showImmediatelyInNewController:YES];
        
        NSLog(@"show: %i", [[UnityAds sharedInstance] show:@{
          kUnityAdsOptionNoOfferscreenKey:@false,
          kUnityAdsOptionOpenAnimatedKey:@true,
          kUnityAdsOptionGamerSIDKey:@"gom",
          kUnityAdsOptionMuteVideoSounds:@false,
          kUnityAdsOptionVideoUsesDeviceOrientation:@true
        }]);
        
        /*
        NSLog(@"SETTING_REWARD_ITEM (while open): %i", [[UnityAds sharedInstance] setRewardItemKey:[[UnityAds sharedInstance] getDefaultRewardItemKey]]);
        NSLog(@"GETTING_REWARD_ITEM_DETAILS: %@", [[UnityAds sharedInstance] getRewardItemDetailsWithKey:[[UnityAds sharedInstance] getCurrentRewardItemKey]]); */
	}
	else {
        NSLog(@"Unity Ads cannot be shown.");
    }
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
    UALOG_DEBUG(@"");
    [textField resignFirstResponder];
    return YES;
}


#pragma mark - UnityAdsDelegate

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
	NSLog(@"unityAdsFetchCompleted");
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
	[self.openButton setEnabled:YES];
    [self.instructionsText setText:@"Press \"Open\" to show Unity Ads"];
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
	NSLog(@"unityAdsWillShow");
}

- (void)unityAdsDidShow:(UnityAds *)unityAds {
	NSLog(@"unityAdsDidShow");
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
	NSLog(@"unityAdsWillHide");
}

- (void)unityAdsDidHide:(UnityAds *)unityAds {
	NSLog(@"unityAdsDidHide");
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
	NSLog(@"unityAdsVideoStarted");
}

- (void)unityAdsVideoCompleted:(UnityAds *)unityAds rewardItemKey:(NSString *)rewardItemKey skipped:(BOOL)skipped {
	NSLog(@"unityAds:completedVideoWithRewardItem: -- key: %@ -- skipped: %@", rewardItemKey, skipped ? @"true" : @"false");
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_reward"]];
}

@end
