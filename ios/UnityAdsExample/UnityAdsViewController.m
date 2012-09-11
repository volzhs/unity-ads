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
#import "UnityAds.h"

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

- (void)viewDidLoad
{
    [super viewDidLoad];
	
	[[UnityAds sharedInstance] setDelegate:self];
	
    [buttonView addTarget:self action:@selector(nextPhase) forControlEvents:UIControlEventTouchUpInside];
}

- (void)nextPhase
{
	[[UnityAds sharedInstance] show];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationLandscapeRight);
}

#pragma mark - UnityAdsDelegate

- (void)unityAdsWillShow:(UnityAds *)unityAds
{
}

- (void)unityAdsWillHide:(UnityAds *)unityAds
{
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds
{
}

- (void)unityAdsVideoCompleted:(UnityAds *)unityAds
{
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds
{
}

- (void)unityAds:(UnityAds *)unityAds wantsToShowAdView:(UIView *)adView
{
	adView.frame = self.view.bounds;
	
	[self.view addSubview:adView];
}

@end
