//
//  UnityAdsDevice.h
//  UnityAds
//
//  Created by bluesun on 10/19/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>

extern NSString * const kUnityAdsDeviceIphone;
extern NSString * const kUnityAdsDeviceIphone3g;
extern NSString * const kUnityAdsDeviceIphone3gs;
extern NSString * const kUnityAdsDeviceIphone4;
extern NSString * const kUnityAdsDeviceIphone4s;
extern NSString * const kUnityAdsDeviceIphone5;
extern NSString * const kUnityAdsDeviceIpodTouch1gen;
extern NSString * const kUnityAdsDeviceIpodTouch2gen;
extern NSString * const kUnityAdsDeviceIpodTouch3gen;
extern NSString * const kUnityAdsDeviceIpodTouch4gen;
extern NSString * const kUnityAdsDeviceIpad;
extern NSString * const kUnityAdsDeviceIpad1;
extern NSString * const kUnityAdsDeviceIpad2;
extern NSString * const kUnityAdsDeviceIpad3;
extern NSString * const kUnityAdsDeviceIosUnknown;
extern NSString * const kUnityAdsSimulator;

@interface UnityAdsDevice : NSObject

+ (NSString *)advertisingIdentifier;
+ (BOOL)canUseTracking;
+ (NSString *)machineName;
+ (NSString *)analyticsMachineName;
+ (NSString *)currentConnectionType;
+ (NSString *)softwareVersion;

+ (NSString *)md5DeviceId;
+ (NSString *)md5OpenUDIDString;
+ (NSString *)md5AdvertisingIdentifierString;
+ (NSString *)md5MACAddressString;

+ (int)getIOSMajorVersion;
+ (NSNumber *)getIOSExactVersion;

+ (BOOL)isSimulator;

@end
