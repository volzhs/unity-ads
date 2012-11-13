//
//  UnityAdsDevice.h
//  UnityAds
//
//  Created by bluesun on 10/19/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UnityAdsDevice : NSObject

+ (NSString *)advertisingIdentifier;
+ (BOOL)canUseTracking;
+ (NSString *)machineName;
+ (NSString *)analyticsMachineName;
+ (NSString *)macAddress;
+ (NSString *)md5OpenUDIDString;
+ (NSString *)md5MACAddressString;
+ (NSString *)md5AdvertisingIdentifierString;
+ (NSString *)currentConnectionType;
+ (NSString *)softwareVersion;
@end
