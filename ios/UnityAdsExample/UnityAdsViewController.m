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
    //[[UnityAds sharedInstance] setTestMode:YES];
	[[UnityAds sharedInstance] startWithGameId:@"16" andViewController:self];
}

- (void)openAds {
	if ([[UnityAds sharedInstance] canShow]) {
        //[[UnityAds sharedInstance] setViewController:self showImmediatelyInNewController:YES];
        [[UnityAds sharedInstance] show];
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

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
	NSLog(@"unityAds:completedVideoWithRewardItem: -- key: %@", rewardItemKey);
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_reward"]];
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
	NSLog(@"unityAdsWillShow");
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
	NSLog(@"unityAdsWillHide");
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
	NSLog(@"unityAdsVideoStarted");
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
	NSLog(@"unityAdsFetchCompleted");
    [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
	[self.buttonView setEnabled:YES];
}

@end
