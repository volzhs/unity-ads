//
//  UnityAdsiOS4.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsiOS4.h"
#import "UnityAdsCampaignManager.h"

@interface UnityAds ()
@property (nonatomic, strong) NSString *gameId;
@property (nonatomic, strong) UnityAdsCampaignManager *campaignManager;
@property (nonatomic, strong) UIWindow *adsWindow;
@property (nonatomic, strong) UIWebView *webView;
@end

@implementation UnityAdsiOS4

@synthesize gameId = _gameId;
@synthesize campaignManager = _campaignManager;
@synthesize adsWindow = _adsWindow;
@synthesize webView = _webView;

#pragma mark - Public

- (void)startWithGameId:(NSString *)gameId
{
	if (self.campaignManager != nil)
		return;
	
	self.gameId = gameId;
	self.campaignManager = [[UnityAdsCampaignManager alloc] init];

	[self.campaignManager updateCampaigns];
}

- (BOOL)show
{
	return YES;
}

- (BOOL)hasCampaigns
{
	return YES;
}

- (void)stopAll
{
}

@end
