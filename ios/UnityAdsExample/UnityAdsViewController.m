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

@interface UnityAdsViewController () <UnityAdsDelegate, SKStoreProductViewControllerDelegate>
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
	
    [self.buttonView addTarget:self action:@selector(nextPhase) forControlEvents:UIControlEventTouchUpInside];
	[self.buttonView setImage:[UIImage imageNamed:@"unityads_waiting"] forState:UIControlStateNormal];
}

- (void)nextPhase
{
	if ([[UnityAds sharedInstance] canShow])
	{
		NSLog(@"Showing Unity Ads.");
		UIView *adView = [[UnityAds sharedInstance] Unity AdsAdView];
		adView.frame = self.view.bounds;
		[self.view addSubview:adView];
	}
	else
		NSLog(@"Unity Ads cannot be shown.");
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationLandscapeRight);
}

#pragma mark - UnityAdsDelegate

- (void)unityAdsWillShow:(UnityAds *)unityAds
{
	NSLog(@"unityAdsWillShow");
}

- (void)unityAdsWillHide:(UnityAds *)unityAds
{
	NSLog(@"unityAdsWillHide");
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds
{
	NSLog(@"unityAdsVideoStarted");
}

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey
{
	NSLog(@"unityAds:completedVideoWithRewardItem: -- key: %@", rewardItemKey);
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds
{
	NSLog(@"unityAdsFetchCompleted");

	[self.buttonView setImage:[UIImage imageNamed:@"unityads_ready"] forState:UIControlStateNormal];
}

- (void)unityAds:(UnityAds *)unityAds wantsToPresentProductViewController:(SKStoreProductViewController *)productViewController
{
	productViewController.delegate = self;
	[self presentViewController:productViewController animated:YES completion:nil];
}

#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController
{
	[self dismissViewControllerAnimated:YES completion:nil];
}

@end
