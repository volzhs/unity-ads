//
//  UnityAdsNoWebViewEndScreenViewController.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/11/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsNoWebViewEndScreenViewController.h"
#import "UnityAdsMainViewController.h"
#import "UnityAdsImageView.h"
#import "UnityAdsNoWebViewEndScreenBottomBar.h"
#import "UnityAdsNativeButton.h"

#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"
#import "../UnityAdsView/UnityAdsNoWebViewEndScreenBottomBarContent.h"

@interface UnityAdsNoWebViewEndScreenViewController ()
  @property (nonatomic, strong) UIButton *closeButton;
  @property (nonatomic, strong) UIButton *rewatchButton;
  @property (nonatomic, strong) UIButton *downloadButton;
  @property (nonatomic, strong) UnityAdsImageView *landScapeImage;
  @property (nonatomic, strong) UnityAdsNoWebViewEndScreenBottomBarContent *bottomBarContent;
  @property (nonatomic, strong) UnityAdsNoWebViewEndScreenBottomBar *bottomBar;
@end

@implementation UnityAdsNoWebViewEndScreenViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
      [self.view setBackgroundColor:[UIColor blackColor]];
      [self createBackgroundImage];
      [self createBottomBar];
      [self createCloseButton];
      [self createRewatchButton];
      [self createBottomBarContent];
    }
    return self;
}

- (void)initController {

}

- (void)viewDidLoad {
  [super viewDidLoad];

}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
}


#pragma mark - Button actions

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

- (void)createBottomBarContent {
  if (self.bottomBarContent == nil && self.bottomBar != nil) {
    int bottomBarContentHeight = 109;
    CGRect refRect = [[UnityAdsMainViewController sharedInstance] view].window.frame;
    //self.bottomBarContent = [[UnityAdsNoWebViewEndScreenBottomBarContent alloc] initWithFrame:CGRectMake(0, 0, [[UnityAdsMainViewController sharedInstance] view].window.frame.size.width, bottomBarContentHeight)];
    //self.bottomBarContent.transform = CGAffineTransformMakeTranslation(160, [[UnityAdsMainViewController sharedInstance] view].window.frame.size.height - bottomBarContentHeight);
    self.bottomBarContent = [[UnityAdsNoWebViewEndScreenBottomBarContent alloc] initWithFrame:CGRectMake((self.view.frame.size.width / 2) - (refRect.size.width / 2), self.view.frame.size.height - bottomBarContentHeight, refRect.size.width, bottomBarContentHeight)];

    self.bottomBarContent.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin;
    [self.view addSubview:self.bottomBarContent];
  }
}

- (void)createBottomBar {
  if (self.bottomBar == nil) {
    int bottomBarHeight = 100;
    int bottomBarWidth = self.view.frame.size.height;
    if (self.view.frame.size.width > bottomBarWidth)
      bottomBarWidth = self.view.frame.size.width;
    
    self.bottomBar = [[UnityAdsNoWebViewEndScreenBottomBar alloc] initWithFrame:CGRectMake((self.view.frame.size.width / 2) - (bottomBarWidth / 2),
                                                                                                  self.view.frame.size.height - bottomBarHeight,
                                                                                                  bottomBarWidth,
                                                                                                  bottomBarHeight)];
    self.bottomBar.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin;
    [self.view addSubview:self.bottomBar];
  }
}

- (void)createBackgroundImage {
  if ([[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil) {
    UnityAdsCampaign *selectedCampaign = [[UnityAdsCampaignManager sharedInstance] selectedCampaign];
    
    if (self.landScapeImage == nil) {
      self.landScapeImage = [[UnityAdsImageView alloc] initWithFrame:CGRectMake(0, 0, self.view.window.frame.size.width, self.view.window.frame.size.height)];
      [self.landScapeImage loadImageFromURL:selectedCampaign.endScreenURL applyScaling:true];
      [self.view addSubview:self.landScapeImage];
    }
  }
}

- (void)createCloseButton {
  UALOG_DEBUG(@"");
  
  int buttonWidth = 50;
  int buttonHeight = 50;
  
  UIColor *myColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0];
  NSArray *gradientArray = [[NSArray alloc] initWithObjects:myColor, myColor, nil];
  
  self.closeButton = [[UnityAdsNativeButton alloc] initWithFrame:CGRectMake(0, 0, buttonWidth, buttonHeight) andBaseColors:gradientArray strokeColor:[UIColor whiteColor] withCorner:UIRectCornerBottomLeft withCornerRadius:23];
  
  self.closeButton.transform = CGAffineTransformMakeTranslation(0, 0);
  self.closeButton.transform = CGAffineTransformMakeTranslation(self.view.bounds.size.width - 47, -3);
  self.closeButton.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleBottomMargin;
  
  [self.closeButton.titleLabel setFont:[UIFont boldSystemFontOfSize:33]];
  [self.closeButton setTitle:[NSString stringWithFormat:@"\u00d7"] forState:UIControlStateNormal];
  [self.view addSubview:self.closeButton];
  [self.closeButton addTarget:self action:@selector(closeButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

- (void)createRewatchButton {
  UALOG_DEBUG(@"");
  
  int buttonWidth = 50;
  int buttonHeight = 50;
  
  UIColor *myColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0];
  NSArray *gradientArray = [[NSArray alloc] initWithObjects:myColor, myColor, nil];
  
  self.rewatchButton = [[UnityAdsNativeButton alloc] initWithFrame:CGRectMake(0, 0, buttonWidth, buttonHeight) andBaseColors:gradientArray strokeColor:[UIColor whiteColor] withCorner:UIRectCornerBottomRight withCornerRadius:23];
  
  self.rewatchButton.transform = CGAffineTransformMakeTranslation(0, 0);
  self.rewatchButton.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin;
  self.rewatchButton.transform = CGAffineTransformMakeTranslation(-3, -3);
  
  [self.rewatchButton.titleLabel setFont:[UIFont boldSystemFontOfSize:30]];
  [self.rewatchButton setTitle:[NSString stringWithFormat:@"\u21bb"] forState:UIControlStateNormal];
  [self.view addSubview:self.rewatchButton];
  [self.rewatchButton addTarget:self action:@selector(rewatchButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

@end
