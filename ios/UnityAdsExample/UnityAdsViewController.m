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

@interface UnityAdsViewController () <UnityAdsDelegate>
@end

@implementation UnityAdsViewController

@synthesize buttonView;
@synthesize loadingImage;
@synthesize contentView;
@synthesize currentPhase;
@synthesize avPlayer;
@synthesize avPlayerLayer;
@synthesize avAsset;
@synthesize avPlayerItem;

- (void)viewDidLoad {
    [super viewDidLoad];
	
	[[UnityAds sharedInstance] setDelegate:self];
    [self.buttonView addTarget:self action:@selector(openAds) forControlEvents:UIControlEventTouchUpInside];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    
    // TEST MODE: Do not use in production apps
    [[UnityAds sharedInstance] setTestMode:YES];
    
    // Initialize Unity Ads
	[[UnityAds sharedInstance] startWithGameId:@"11006" andViewController:self];
}

- (void)openAds {
	if ([[UnityAds sharedInstance] canShow]) {
        NSLog(@"REWARD_ITEM_KEYS: %@", [[UnityAds sharedInstance] getRewardItemKeys]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"SETTING_REWARD_ITEM (wrong): %i", [[UnityAds sharedInstance] setRewardItemKey:@"wrong_key"]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"SETTING_REWARD_ITEM (right): %i", [[UnityAds sharedInstance] setRewardItemKey:[[[UnityAds sharedInstance] getRewardItemKeys] objectAtIndex:0]]);
        NSLog(@"CURRENT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getCurrentRewardItemKey]);
        NSLog(@"DEFAULT_REWARD_ITEM: %@", [[UnityAds sharedInstance] getDefaultRewardItemKey]);

        //[[UnityAds sharedInstance] setViewController:self showImmediatelyInNewController:YES];
        [[UnityAds sharedInstance] show];
        
        NSLog(@"SETTING_REWARD_ITEM (while open): %i", [[UnityAds sharedInstance] setRewardItemKey:[[UnityAds sharedInstance] getDefaultRewardItemKey]]);

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


#pragma mark - UnityAdsDelegate

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
	NSLog(@"unityAdsFetchCompleted");
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
	[self.buttonView setEnabled:YES];
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

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
	NSLog(@"unityAds:completedVideoWithRewardItem: -- key: %@", rewardItemKey);
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_reward"]];
}

@end
