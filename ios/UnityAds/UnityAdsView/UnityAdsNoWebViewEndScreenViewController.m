//
//  UnityAdsNoWebViewEndScreenViewController.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsNoWebViewEndScreenViewController.h"
#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"

@interface UnityAdsNoWebViewEndScreenViewController ()
  @property (nonatomic, strong) UIButton *closeButton;
  @property (nonatomic, strong) UIButton *rewatchButton;
  @property (nonatomic, strong) UIButton *downloadButton;
@end

@implementation UnityAdsNoWebViewEndScreenViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
      [self.view setBackgroundColor:[UIColor brownColor]];
      [self createCloseButton];
      [self createRewatchButton];
      [self createDownloadButton];
    }
    return self;
}

- (void)initController {

}

- (void)viewDidLoad {
  [super viewDidLoad];
	// Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


#pragma mark - Button actions

- (void)downloadButtonClicked {
  NSDictionary *data = @{kUnityAdsCampaignStoreIDKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].itunesID,
                         kUnityAdsWebViewEventDataClickUrlKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].clickURL,
                         kUnityAdsCampaignIDKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
  
  [[UnityAdsMainViewController sharedInstance] applyOptionsToCurrentState:data];
  UALOG_DEBUG(@"");
}

- (void)rewatchButtonClicked {
  UALOG_DEBUG(@"");
  
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil) {
    NSDictionary *data = @{kUnityAdsWebViewEventDataRewatchKey:@true,
                           kUnityAdsWebViewEventDataCampaignIdKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
    [[UnityAdsMainViewController sharedInstance] changeState:kUnityAdsViewStateTypeVideoPlayer withOptions:data];
  }
}

- (void)closeButtonClicked {
  UALOG_DEBUG(@"");
  [[UnityAdsMainViewController sharedInstance] closeAds:YES withAnimations:YES withOptions:nil];
}


#pragma mark - View creation

- (void)createBackgroundImage {
  
}

- (void)createCloseButton {
  UALOG_DEBUG(@"");
  self.closeButton = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 50, 50)];
  [self.closeButton setBackgroundColor:[UIColor blackColor]];
  [self.closeButton setTitle:@"close" forState:UIControlStateNormal];
  [self.view addSubview:self.closeButton];
  self.closeButton.transform = CGAffineTransformMakeTranslation(self.view.bounds.size.width - 50, 0);
  self.closeButton.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleBottomMargin;
  [self.view bringSubviewToFront:self.closeButton];
  
  [self.closeButton addTarget:self action:@selector(closeButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

- (void)createRewatchButton {
  UALOG_DEBUG(@"");
  self.rewatchButton = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 50, 50)];
  [self.rewatchButton setBackgroundColor:[UIColor blackColor]];
  [self.rewatchButton setTitle:@"rewatch" forState:UIControlStateNormal];
  [self.view addSubview:self.rewatchButton];
  self.rewatchButton.transform = CGAffineTransformMakeTranslation(0, 0);
  self.rewatchButton.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin;
  [self.view bringSubviewToFront:self.rewatchButton];
  
  [self.rewatchButton addTarget:self action:@selector(rewatchButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

- (void)createDownloadButton {
  UALOG_DEBUG(@"");
  self.downloadButton = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 120, 40)];
  [self.downloadButton setBackgroundColor:[UIColor greenColor]];
  [self.downloadButton setTitle:@"download" forState:UIControlStateNormal];
  [self.view addSubview:self.downloadButton];
  self.downloadButton.transform = CGAffineTransformMakeTranslation((self.view.bounds.size.width / 2) - (120 / 2), self.view.bounds.size.height - 100);
  self.downloadButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleLeftMargin;
  [self.view bringSubviewToFront:self.downloadButton];
  
  [self.downloadButton addTarget:self action:@selector(downloadButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

@end
