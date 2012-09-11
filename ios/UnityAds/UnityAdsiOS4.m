//
//  UnityAdsiOS4.m
//  UnityAdsExample
//
//  Created by Johan Halin on 9/4/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>
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
@property (nonatomic, strong) UIView *adView;
@property (nonatomic, strong) UnityAdsCampaign *selectedCampaign;
@property (nonatomic, strong) AVPlayer *player;
@property (nonatomic, strong) AVPlayerLayer *playerLayer;
@end

@implementation UnityAdsiOS4

@synthesize gameId = _gameId;
@synthesize backgroundThread = _backgroundThread;
@synthesize campaignManager = _campaignManager;
@synthesize adsWindow = _adsWindow;
@synthesize webView = _webView;
@synthesize campaigns = _campaigns;
@synthesize rewardItem = _rewardItem;
@synthesize adView = _adView;
@synthesize selectedCampaign = _selectedCampaign;
@synthesize player = _player;
@synthesize playerLayer = _playerLayer;

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

- (void)_selectCampaign:(UnityAdsCampaign *)campaign
{
	if (campaign == nil)
		return;
	
	self.selectedCampaign = campaign;
	
	NSString *js = [NSString stringWithFormat:@"selectCampaign(%@);", campaign.id];
	
	[self.webView stringByEvaluatingJavaScriptFromString:js];
}

- (void)_configureWebView
{
	self.adsWindow = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
	self.webView = [[UIWebView alloc] initWithFrame:self.adsWindow.bounds];
	self.webView.delegate = self;
	self.webView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
	[self.webView loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:kUnityAdsTestWebViewURL]]];
	[self.adsWindow addSubview:self.webView];
}

- (UIView *)_adView
{
	if (self.adView == nil)
	{
		self.adView = [[UIView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
		self.webView.bounds = self.adView.bounds;
		[self.adView addSubview:self.webView];
	}
	
	return self.adView;
}

- (void)_playVideo
{
	NSURL *videoURL = [self.campaignManager videoURLForCampaign:self.selectedCampaign];
	if (videoURL == nil)
	{
		NSLog(@"Video not found!");
		return;
	}
	
	AVPlayerItem *item = [AVPlayerItem playerItemWithURL:videoURL];
	self.player = [AVPlayer playerWithPlayerItem:item];
	self.playerLayer = [AVPlayerLayer playerLayerWithPlayer:self.player];
	self.playerLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
	self.playerLayer.frame = self.adView.bounds;
	[self.adView.layer addSublayer:self.playerLayer];
	[self.player play];

	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_videoPlaybackEnded:) name:AVPlayerItemDidPlayToEndTimeNotification object:item];
	
	if ([self.delegate respondsToSelector:@selector(unityAdsVideoStarted:)])
		[self.delegate unityAdsVideoStarted:self];
}

- (void)_videoPlaybackEnded:(NSNotification *)notification
{
	if ([self.delegate respondsToSelector:@selector(unityAdsVideoCompleted:)])
		[self.delegate unityAdsVideoCompleted:self];
	
	[self.playerLayer removeFromSuperlayer];
	// FIXME: use the actual API
	[self.webView stringByEvaluatingJavaScriptFromString:@"document.getElementById('videoStart').style.display = 'none';"];
	[self.webView stringByEvaluatingJavaScriptFromString:@"document.getElementById('videoCompleted').style.display = 'block';"];
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

	[self _configureWebView];
}

- (BOOL)show
{
	// FIXME: probably not the best way to accomplish this
	
	if ([self.campaigns count] > 0)
	{
		// merge the following two delegate methods?
		if ([self.delegate respondsToSelector:@selector(unityAdsWillShow:)])
			[self.delegate unityAdsWillShow:self];
		
		if ([self.delegate respondsToSelector:@selector(unityAds:wantsToShowAdView:)])
			[self.delegate unityAds:self wantsToShowAdView:[self _adView]];
		
		return YES;
	}
	
	return NO;
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
	
	self.campaigns = campaigns;
	self.rewardItem = rewardItem;
	
	if ([self.delegate respondsToSelector:@selector(unityAdsFetchCompleted:)])
		[self.delegate unityAdsFetchCompleted:self];
	
	[self _selectCampaign:[self.campaigns lastObject]];
}

#pragma mark - UIWebViewDelegate

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType
{
	// FIXME: this is test code
	NSString *urlString = [[request URL] absoluteString];
	if ([[urlString substringFromIndex:[urlString length] - 1] isEqualToString:@"#"])
	{
		[self _playVideo];
		return NO;
	}
	
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
