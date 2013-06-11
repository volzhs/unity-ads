/*
 * Copyright 2013, Unity Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "UnityAdsMopubEvent.h"

@implementation UnityAdsMopubEvent

static NSString *DEVICE_ORIENTATION_KEY = @"deviceOrientation";
static NSString *MUTE_SOUNDS_KEY = @"muteSounds";

@synthesize delegate;
@synthesize muteSoundsOption;
@synthesize deviceOrientationOption;

- (void)requestInterstitialWithCustomEventInfo:(NSDictionary *)info {
    [[UnityAds sharedInstance] startWithGameId:[info objectForKey:@"gameId"]];
    [[UnityAds sharedInstance] setDelegate:self];
    
    // Default options
    self.muteSoundsOption = @false;
    self.deviceOrientationOption = @false;
    
    // Parse the options, if we have any
    NSEnumerator *keySet = [info keyEnumerator];
    for(NSObject *key in keySet) {
        if([key isKindOfClass:[NSString class]]) {
            if([MUTE_SOUNDS_KEY isEqualToString:(NSString*)key]) {
                if([@"true" isEqualToString:(NSString *)[info objectForKey:MUTE_SOUNDS_KEY]]) {
                    self.muteSoundsOption = @true;
                }
            }
            if([DEVICE_ORIENTATION_KEY isEqualToString:(NSString*)key]) {
                if([@"true" isEqualToString:(NSString *)[info objectForKey:DEVICE_ORIENTATION_KEY]]) {
                    self.deviceOrientationOption = @true;
                }                
            }
            
        }
    }
    
}

- (void)showInterstitialFromRootViewController:(UIViewController *)rootViewController {
    if([[UnityAds sharedInstance] canShowAds]) {
        [[UnityAds sharedInstance] setViewController:rootViewController showImmediatelyInNewController:NO];
        [[UnityAds sharedInstance] show:
         @{kUnityAdsOptionNoOfferscreenKey:@true,
             kUnityAdsOptionMuteVideoSounds:self.muteSoundsOption,
             kUnityAdsOptionVideoUsesDeviceOrientation:self.deviceOrientationOption}];
    }
}

- (void)unityAds:(UnityAds *)unityAds completedVideoWithRewardItemKey:(NSString *)rewardItemKey {
    // Ignored, as no support for incentivised ads via Mopub
}

- (void)unityAdsFetchCompleted:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEvent:self didLoadAd:nil];
}

- (void)unityAdsFetchFailed:(UnityAds *)unityAds {
    NSMutableDictionary* details = [NSMutableDictionary dictionary];
    [details setValue:@"No ads available" forKey:NSLocalizedDescriptionKey];
    [self.delegate interstitialCustomEvent:self didFailToLoadAdWithError:[NSError errorWithDomain:@"unityads_sdk" code:404 userInfo:details]];
}

- (void)unityAdsDidHide:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEventDidDisappear:self];
}

- (void)unityAdsDidShow:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEventDidAppear:self];
}

- (void)unityAdsVideoStarted:(UnityAds *)unityAds {
    // Ignored
}

- (void)unityAdsWillHide:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEventWillDisappear:self];
}

- (void)unityAdsWillShow:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEventWillAppear:self];
}

- (void)unityAdsWillLeaveApplication:(UnityAds *)unityAds {
    [self.delegate interstitialCustomEventWillLeaveApplication:self];
}

@end
