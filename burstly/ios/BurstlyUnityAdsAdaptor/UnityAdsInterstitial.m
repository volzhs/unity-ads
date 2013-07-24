//
//  UnityAdsInterstitial.m
//  BurstlySampleCL
//
//  Created by Ville Orkas on 7/22/13.
//
//

#import "UnityAdsInterstitial.h"

@implementation UnityAdsInterstitial

@synthesize delegate = _delegate;

- (id)initWithParams:(NSDictionary *)params {
    self = [super init];
    if(self != nil) {
        _params = [NSMutableDictionary dictionary];
                
        NSString *noOfferScreenValue = [params objectForKey:kUnityAdsOptionNoOfferscreenKey];
        NSString *openAnimatedValue = [params objectForKey:kUnityAdsOptionOpenAnimatedKey];
        NSString *gamerSidValue = [params objectForKey:kUnityAdsOptionGamerSIDKey];
        NSString *muteVideoSoundsValue = [params objectForKey:kUnityAdsOptionMuteVideoSounds];
        NSString *videoUsesDeviceOrientationValue = [params objectForKey:kUnityAdsOptionVideoUsesDeviceOrientation];
        
        if(noOfferScreenValue != nil) {
            [_params setObject:@true forKey:kUnityAdsOptionNoOfferscreenKey];
        }
        if(openAnimatedValue != nil) {
            [_params setObject:@true forKey:kUnityAdsOptionOpenAnimatedKey];
        }
        if(gamerSidValue != nil) {
            [_params setObject:gamerSidValue forKey:kUnityAdsOptionGamerSIDKey];
        }
        if(muteVideoSoundsValue != nil) {
            [_params setObject:@true forKey:kUnityAdsOptionMuteVideoSounds];
        }
        if(videoUsesDeviceOrientationValue != nil) {
            [_params setObject:@true forKey:kUnityAdsOptionVideoUsesDeviceOrientation];
        }
    }
    return self;
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
    [[UnityAds sharedInstance] show:[_params copy]];
}

/*=
 * UnityAds method
 *=*/

-(void)unityAdsVideoCompleted:(UnityAds *)unityAds rewardItemKey:(NSString *)rewardItemKey skipped:(BOOL)skipped {
}

-(void)unityAdsFetchCompleted:(UnityAds *)unityAds {
    [[self delegate] interstitialDidLoadAd:self];
}

-(void)unityAdsDidShow:(UnityAds *)unityAds {
    [[self delegate] interstitialWillPresentFullScreen:self];
    [[self delegate] interstitialDidPresentFillScreen:self];
}

-(void)unityAdsDidHide:(UnityAds *)unityAds {
    [[self delegate] interstitialDidDismissFullScreen:self];
}

-(void)unityAdsWillLeaveApplication:(UnityAds *)unityAds {
    [[self delegate] interstitialWillLeaveApplication:self];
}

@end
