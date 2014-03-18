//
//  UnityAdsAppSheet.h
//  UnityAds
//
//  Created by Ville Orkas on 13/03/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <StoreKit/SKStoreProductViewController.h>

@interface CustomStoreProductViewController : SKStoreProductViewController
@end

@interface UnityAdsAppSheetManager : NSObject
- (void)preloadAppSheetWithId:(NSString *)iTunesId;
- (id)getAppSheetController:(NSString *)iTunesId;
- (void)openAppSheetWithId:(NSString *)iTunesId toViewController:(UIViewController *)targetViewController withCompletionBlock:(void (^)(BOOL result, NSError *error))completionBlock;

+ (BOOL)canOpenStoreProductViewController;

+ (id)sharedInstance;
@end
