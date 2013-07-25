//
//  BurstlyUnityAdsAdaptor.h
//  BurstlySampleCL
//
//  Created by Tuomas Rinta on 6/28/13.
//
//

#import <Foundation/Foundation.h>

#import "BurstlyAdNetworkAdaptorProtocol.h"
#import "BurstlyAdBannerProtocol.h"
#import "BurstlyAdInterstitialProtocol.h"
#import "UnityAdsInterstitial.h"
#import <UnityAds/UnityAds.h>

@interface BurstlyUnityAdsAdaptor : NSObject <BurstlyAdNetworkAdaptorProtocol>
@end
