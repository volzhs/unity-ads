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

@interface UnityAdsNoWebViewEndScreenViewController ()
  @property (nonatomic, strong) UIButton *closeButton;
  @property (nonatomic, strong) UIButton *rewatchButton;
  @property (nonatomic, strong) UIButton *downloadButton;
  @property (nonatomic, strong) UnityAdsImageView *landScapeImage;
  @property (nonatomic, strong) UnityAdsNoWebViewEndScreenBottomBar *bottomBar;
@end

@implementation UnityAdsNoWebViewEndScreenViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
      [self.view setBackgroundColor:[UIColor brownColor]];
      [self createBackgroundImage];
      [self createBottomBar];
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
}

- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning];
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
  UALOG_DEBUG(@"");
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

- (void)createBottomBar {
  if (self.bottomBar == nil) {
    int bottomBarHeight = 120;
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
      self.landScapeImage = [[UnityAdsImageView alloc] initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, self.view.bounds.size.height)];
      [self.landScapeImage loadImageFromURL:selectedCampaign.endScreenURL];
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
  
  [self.closeButton.titleLabel setFont:[UIFont systemFontOfSize:33]];
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
  
  [self.rewatchButton.titleLabel setFont:[UIFont systemFontOfSize:30]];
  [self.rewatchButton setTitle:[NSString stringWithFormat:@"\u21bb"] forState:UIControlStateNormal];
  [self.view addSubview:self.rewatchButton];
  [self.rewatchButton addTarget:self action:@selector(rewatchButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

- (void)createDownloadButton {
  UALOG_DEBUG(@"");
  
  int buttonWidth = 120;
  int buttonHeight = 40;
  
  CGFloat red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0;
  [[UIColor greenColor] getRed:&red green:&green blue:&blue alpha:&alpha];
  UIColor *myColor = [UIColor colorWithRed:red / 2 green:green / 2 blue:blue / 2 alpha:alpha];
  NSArray *gradientArray = [[NSArray alloc] initWithObjects:[UIColor greenColor], myColor, nil];
  
  self.downloadButton = [[UnityAdsNativeButton alloc] initWithFrame:CGRectMake(0, 0, buttonWidth, buttonHeight) andBaseColors:gradientArray strokeColor:[UIColor greenColor]];
  self.downloadButton.transform = CGAffineTransformMakeTranslation((self.view.bounds.size.width / 2) - (buttonWidth / 2), self.view.bounds.size.height - 50);
  self.downloadButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleLeftMargin;
  
  [self.downloadButton setTitle:@"\u21ea download" forState:UIControlStateNormal];
  [self.view addSubview:self.downloadButton];
  [self.view bringSubviewToFront:self.downloadButton];
  [self.downloadButton addTarget:self action:@selector(downloadButtonClicked) forControlEvents:UIControlEventTouchUpInside];
}

@end
