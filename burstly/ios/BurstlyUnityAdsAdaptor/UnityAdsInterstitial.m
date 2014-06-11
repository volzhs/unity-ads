//
//  UnityAdsInterstitial.m
//  BurstlySampleCL
//
//  Created by Ville Orkas on 7/22/13.
//
//

#import "UnityAdsInterstitial.h"

static NSString const * const kUnityAdsOptionZoneIdKey = @"zoneId";

@implementation UnityAdsInterstitial

@synthesize delegate = _delegate;

- (id)initWithParams:(NSDictionary *)params {
  self = [super init];
  if(self != nil) {
    _params = [[NSMutableDictionary alloc] init];
    
    _zoneId = [params objectForKey:kUnityAdsOptionZoneIdKey];
    
    NSString *noOfferScreenValue = [params objectForKey:kUnityAdsOptionNoOfferscreenKey];
    NSString *openAnimatedValue = [params objectForKey:kUnityAdsOptionOpenAnimatedKey];
    NSString *gamerSidValue = [params objectForKey:kUnityAdsOptionGamerSIDKey];
    NSString *muteVideoSoundsValue = [params objectForKey:kUnityAdsOptionMuteVideoSounds];
    NSString *videoUsesDeviceOrientationValue = [params objectForKey:kUnityAdsOptionVideoUsesDeviceOrientation];
    
    if(noOfferScreenValue != nil) {
      [_params setObject:noOfferScreenValue forKey:kUnityAdsOptionNoOfferscreenKey];
    }
    if(openAnimatedValue != nil) {
      [_params setObject:openAnimatedValue forKey:kUnityAdsOptionOpenAnimatedKey];
    }
    if(gamerSidValue != nil) {
      [_params setObject:gamerSidValue forKey:kUnityAdsOptionGamerSIDKey];
    }
    if(muteVideoSoundsValue != nil) {
      [_params setObject:muteVideoSoundsValue forKey:kUnityAdsOptionMuteVideoSounds];
    }
    if(videoUsesDeviceOrientationValue != nil) {
      [_params setObject:videoUsesDeviceOrientationValue forKey:kUnityAdsOptionVideoUsesDeviceOrientation];
    }
  }
  return self;
}

- (void)dealloc {
  [_params dealloc];
  [super dealloc];
}

/**
 * Starts ad loading on a background thread and immediately returns control.
 * As long as Unity Ads has campaigns available, call the ad loaded delegate.
 */
- (void)loadInterstitialInBackground {
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    if([[UnityAds sharedInstance] canShowAds]) {
      [[self delegate] interstitialDidLoadAd:self];
    } else {
      [[self delegate] interstitial:self didFailToLoadAdWithError:[NSError errorWithDomain:@"UnityAds" code:0 userInfo:nil]];
    }
  });
}

/**
 * Cancels ad loading. Not supported by Unity Ads.
 */
- (void)cancelInterstitialLoading {
}

/**
 * Tries to presents loaded ad interstitial to user.
 *
 */
- (void)presentInterstitial {
  UALOG_DEBUG(@"");
  [[UnityAds sharedInstance] setViewController:[[self delegate] viewControllerForModalPresentation] showImmediatelyInNewController:NO];
  [[UnityAds sharedInstance] setZone:_zoneId];
  [[UnityAds sharedInstance] show:_params];
}

/*=
 * UnityAds method
 *=*/

-(void)unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped {
}

-(void)unityAdsFetchCompleted {
  [[self delegate] interstitialDidLoadAd:self];
}

-(void)unityAdsDidShow {
  [[self delegate] interstitialWillPresentFullScreen:self];
  [[self delegate] interstitialDidPresentFullScreen:self];
}

-(void)unityAdsDidHide {
  [[self delegate] interstitialDidDismissFullScreen:self];
}

-(void)unityAdsWillLeaveApplication {
  [[self delegate] interstitialWillLeaveApplication:self];
}

@end
