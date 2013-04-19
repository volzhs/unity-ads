//
//  UnityAdsNoWebViewEndScreenBottomBarContent.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/18/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsNoWebViewEndScreenBottomBarContent.h"
#import "UnityAdsMainViewController.h"
#import "UnityAdsImageView.h"
#import "UnityAdsImageViewRoundedCorners.h"
#import "UnityAdsNativeButton.h"

#import "../UnityAds.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsCampaign/UnityAdsCampaign.h"


@interface UnityAdsNoWebViewEndScreenBottomBarContent ()
  @property (nonatomic, strong) UnityAdsImageViewRoundedCorners *gameIcon;
  @property (nonatomic, strong) UIButton *downloadButton;
  @property (nonatomic, strong) UILabel *gameName;
@end

@implementation UnityAdsNoWebViewEndScreenBottomBarContent

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
      [self createGameIcon];
      [self createGameNameLabel];
      [self createDownloadButton];
      
      [self updateViewData];
    }
    return self;
}

#pragma mark - Data update

- (void)updateViewData {
  UALOG_DEBUG(@"");
  
  UnityAdsCampaign *selectedCampaign = [[UnityAdsCampaignManager sharedInstance] selectedCampaign];
  
  if (self.gameIcon != nil && selectedCampaign != nil) {
    [self.gameIcon loadImageFromURL:selectedCampaign.gameIconURL];
  }
  if (self.gameName != nil && selectedCampaign != nil) {
    [self.gameName setText:selectedCampaign.gameName];
  }
}

#pragma mark - View creation

- (void)createGameNameLabel {
  if (self.gameName == nil && [[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil) {
    self.gameName = [[UILabel alloc] initWithFrame:CGRectMake(0, 10, 200, 25)];
    self.gameName.transform = CGAffineTransformMakeTranslation((self.bounds.size.width / 2) - (150 / 2) + 38, 11);
    self.gameName.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleLeftMargin;
    [self.gameName setBackgroundColor:[UIColor clearColor]];
    [self.gameName setTextColor:[UIColor whiteColor]];
    UIColor *myShadowColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [self.gameName setShadowColor:myShadowColor];
    [self.gameName setShadowOffset:CGSizeMake(0, 2)];
    [self.gameName setFont:[UIFont boldSystemFontOfSize:20]];
    
    [self addSubview:self.gameName];
  }
}

- (void)createGameIcon {
  if (self.gameIcon == nil && [[UnityAdsCampaignManager sharedInstance] selectedCampaign] != nil) {
    int gameIconSize = 65;
    UnityAdsCampaign *selectedCampaign = [[UnityAdsCampaignManager sharedInstance] selectedCampaign];
    
    if (selectedCampaign.gameIconURL != nil) {
      self.gameIcon = [[UnityAdsImageViewRoundedCorners alloc] initWithFrame:CGRectMake(0, 0, gameIconSize, gameIconSize)];
      self.gameIcon.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin;
      self.gameIcon.transform = CGAffineTransformMakeTranslation((self.frame.size.width / 2) - (gameIconSize / 2) - 85, 0);
      
      [self addSubview:self.gameIcon];
    }
  }
}

- (void)createDownloadButton {
  UALOG_DEBUG(@"");
  
  int buttonWidth = 150;
  int buttonHeight = 40;
  
  CGFloat red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0;
  [[UIColor greenColor] getRed:&red green:&green blue:&blue alpha:&alpha];
  UIColor *myColor = [UIColor colorWithRed:red / 2 green:green / 2 blue:blue / 2 alpha:alpha];
  NSArray *gradientArray = [[NSArray alloc] initWithObjects:[UIColor greenColor], myColor, nil];
  
  self.downloadButton = [[UnityAdsNativeButton alloc] initWithFrame:CGRectMake(0, 0, buttonWidth, buttonHeight) andBaseColors:gradientArray strokeColor:[UIColor clearColor]];
  self.downloadButton.transform = CGAffineTransformMakeTranslation((self.bounds.size.width / 2) - (buttonWidth / 2) + 33, self.bounds.size.height - 53);
  self.downloadButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleLeftMargin;
  
  UIColor *myShadowColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
  [self.downloadButton.titleLabel setShadowColor:myShadowColor];
  [self.downloadButton.titleLabel setShadowOffset:CGSizeMake(0, -1)];
  
  [self.downloadButton setTitle:@"\u21ea download" forState:UIControlStateNormal];
  [self.downloadButton setUserInteractionEnabled:true];
  [self.downloadButton addTarget:self action:@selector(downloadButtonClicked) forControlEvents:UIControlEventTouchUpInside];
  
  [self addSubview:self.downloadButton];
  [self bringSubviewToFront:self.downloadButton];
}

- (void)downloadButtonClicked {
  UALOG_DEBUG(@"");
  NSDictionary *data = @{kUnityAdsCampaignStoreIDKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].itunesID,
                         kUnityAdsWebViewEventDataClickUrlKey:[[[UnityAdsCampaignManager sharedInstance] selectedCampaign].clickURL absoluteString],
                         kUnityAdsCampaignIDKey:[[UnityAdsCampaignManager sharedInstance] selectedCampaign].id};
  
  [[UnityAdsMainViewController sharedInstance] applyOptionsToCurrentState:data];
}


@end
