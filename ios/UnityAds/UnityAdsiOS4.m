//
//  UnityAdsiOS4.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsiOS4.h"
#import "UnityAdsCampaignManager.h"
#import "UnityAdsCampaign.h"
#import "UnityAdsRewardItem.h"

NSString * const kUnityAdsTestWebViewURL = @"http://ads-dev.local/webapp.html";

@interface UnityAdsiOS4 () <UnityAdsCampaignManagerDelegate, UIWebViewDelegate>
@property (nonatomic, strong) NSString *gameId;
@property (nonatomic, strong) NSThread *backgroundThread;
@property (nonatomic, strong) UnityAdsCampaignManager *campaignManager;
@property (nonatomic, strong) UIWindow *adsWindow;
@property (nonatomic, strong) UIWebView *webView;
@property (nonatomic, strong) NSArray *campaigns;
@property (nonatomic, strong) UnityAdsRewardItem *rewardItem;
@end

@implementation UnityAdsiOS4

@synthesize gameId = _gameId;
@synthesize backgroundThread = _backgroundThread;
@synthesize campaignManager = _campaignManager;
@synthesize adsWindow = _adsWindow;
@synthesize webView = _webView;

#pragma mark - Private

- (void)_backgroundRunLoop:(id)dummy
{
	@autoreleasepool
	{
		NSPort *port = [[NSPort alloc] init];
		[port scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
		
		while([[NSThread currentThread] isCancelled] == NO)
		{
			@autoreleasepool
			{
				[[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:3.0]];
			}
		}
	}
}

- (void)_startCampaignManager
{
	self.campaignManager = [[UnityAdsCampaignManager alloc] init];
	self.campaignManager.delegate = self;
	[self.campaignManager updateCampaigns];
}

#pragma mark - Public

- (void)startWithGameId:(NSString *)gameId
{
	if (self.campaignManager != nil)
		return;
	
	self.gameId = gameId;
	self.backgroundThread = [[NSThread alloc] initWithTarget:self selector:@selector(_backgroundRunLoop:) object:nil];
	[self.backgroundThread start];
	
	[self performSelector:@selector(_startCampaignManager) onThread:self.backgroundThread withObject:nil waitUntilDone:NO];
	
	self.adsWindow = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
	self.webView = [[UIWebView alloc] initWithFrame:self.adsWindow.bounds];
	self.webView.delegate = self;
	[self.webView loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:kUnityAdsTestWebViewURL]]];
	[self.adsWindow addSubview:self.webView];
}

- (BOOL)show
{
	return YES;
}

- (BOOL)hasCampaigns
{
	return ([self.campaigns count] > 0);
}

- (void)stopAll
{
}

- (void)dealloc
{
	self.campaignManager.delegate = nil;
}

#pragma mark - UnityAdsCampaignManagerDelegate

- (void)campaignManager:(UnityAdsCampaignManager *)campaignManager updatedWithCampaigns:(NSArray *)campaigns rewardItem:(UnityAdsRewardItem *)rewardItem
{
	if ( ! [NSThread isMainThread])
	{
		NSLog(@"Method must be run on main thread.");
		return;
	}
	
	NSLog(@"updatedWithCampaigns");
	
	self.campaigns = campaigns;
	self.rewardItem = rewardItem;
}

#pragma mark - UIWebViewDelegate

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType
{
	return YES;
}

- (void)webViewDidStartLoad:(UIWebView *)webView
{
}

- (void)webViewDidFinishLoad:(UIWebView *)webView
{
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error
{
}

@end
