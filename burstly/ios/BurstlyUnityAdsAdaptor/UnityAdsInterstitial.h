//
//  UnityAdsInterstitial.h
//  BurstlySampleCL
//
//  Created by Ville Orkas on 7/22/13.
//
//

#import <Foundation/Foundation.h>

#import "BurstlyUnityAdsAdaptor.h"
#import <UnityAds/UnityAds.h>

@interface UnityAdsInterstitial : NSObject <BurstlyAdInterstitialProtocol, UnityAdsDelegate> {
  id<BurstlyAdInterstitialDelegate> _delegate;
  NSString * _zoneId;
  NSMutableDictionary *_params;
}

- (id)initWithParams:(NSDictionary *)params;

@end
