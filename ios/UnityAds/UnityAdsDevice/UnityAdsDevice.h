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
+ (NSString *)identifierForVendor;
+ (BOOL)canUseTracking;
+ (NSString *)machineName;
+ (NSString *)analyticsMachineName;


+ (void)launchReachabilityCheck;
+ (void)clearReachabilityCheck;
+ (NSString *)currentConnectionType;

+ (NSString *)softwareVersion;

+ (NSString *)md5DeviceId;
+ (NSString *)md5AdvertisingIdentifierString;
+ (NSString *)md5MACAddressString;

+ (int)getIOSMajorVersion;
+ (NSNumber *)getIOSExactVersion;

+ (BOOL)isSimulator;
+ (BOOL)isEncrypted;
+ (BOOL)isJailbroken;

@end
