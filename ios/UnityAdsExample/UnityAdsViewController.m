//
//  UnityAdsViewController.m
//  UnityAdsExample
//
//  Created by bluesun on 7/30/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#import "UnityAdsViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <UnityAds/UnityAds.h>

@interface UnityAdsViewController () <UnityAdsDelegate>
@end

@implementation UnityAdsViewController

@synthesize buttonView;
@synthesize contentView;
@synthesize currentPhase;
@synthesize avPlayer;
@synthesize avPlayerLayer;
@synthesize avAsset;
@synthesize avPlayerItem;

- (void)viewDidLoad {
    [super viewDidLoad];
	
	[[UnityAds sharedInstance] setDelegate:self];
	
    [self.buttonView addTarget:self action:@selector(nextPhase) forControlEvents:UIControlEventTouchUpInside];
	[self.buttonView setImage:[UIImage imageNamed:@"unityads_waiting"] forState:UIControlStateNormal];

}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [[UnityAds sharedInstance] setTestMode:YES];
	[[UnityAds sharedInstance] startWithGameId:@"16" andViewController:self];
}

- (void)nextPhase {
	if ([[UnityAds sharedInstance] canShow]) {
        [[UnityAds sharedInstance] show];
	}
	else {
        NSLog(@"Unity Ads cannot be shown.");
    }
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    NSLog(@"Rotate");
    return (interfaceOrientation == UIInterfaceOrientationLandscapeRight);
}

- (NSUInteger) supportedInterfaceOrientations {
    NSLog(@"Rotate");
    return UIInterfaceOrientationMaskLandscape | UIInterfaceOrientationMaskPortrait;
}

#pragma mark - UnityAdsDelegate

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
	NSLog(@"unityAds:completedVideoWithRewardItem: -- key: %@", rewardItemKey);
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
	[self.buttonView setImage:[UIImage imageNamed:@"unityads_ready"] forState:UIControlStateNormal];
}

@end
