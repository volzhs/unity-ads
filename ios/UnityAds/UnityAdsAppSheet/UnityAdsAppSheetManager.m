//
//  UnityAdsAppSheet.m
//  UnityAds
//
//  Created by Ville Orkas on 13/03/14.
//  Copyright (c) 2014 Unity Technologies. All rights reserved.
//

#import "UnityAds.h"
#import "UnityAdsAppSheetManager.h"
#import "UnityAdsCampaignManager.h"
#import "UnityAdsAnalyticsUploader.h"
#import "UnityAdsDevice.h"

@implementation CustomStoreProductViewController

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
  return [UIApplication sharedApplication].statusBarOrientation;
}

- (NSUInteger)supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  return YES;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation {
  return YES;
}

- (BOOL)prefersStatusBarHidden {
  return YES;
}

@end

@interface UnityAdsAppSheetManager () {
@protected
  NSString * _currentItunesId;
}
@property (nonatomic, strong) NSMutableDictionary *appSheetCache;
@end

@implementation UnityAdsAppSheetManager

static UnityAdsAppSheetManager *sharedAppSheetManager = nil;

+ (id)sharedInstance {
  @synchronized(self) {
    if (sharedAppSheetManager == nil) {
      sharedAppSheetManager = [[UnityAdsAppSheetManager alloc] init];
    }
  }
  
  return sharedAppSheetManager;
}

- (id)init {
  if ((self = [super init])) {
    _appSheetCache = [[NSMutableDictionary alloc] init];
  }
  return self;
}

- (void)preloadAppSheetWithId:(NSString *)iTunesId {
  UALOG_DEBUG(@"");
  if ([UnityAdsAppSheetManager canOpenStoreProductViewController]) {
    UALOG_DEBUG(@"Can open storeProductViewController");
    if (![iTunesId isKindOfClass:[NSString class]] || iTunesId == nil || [iTunesId length] < 1) return;
    
    NSDictionary *productParams = @{@"id":iTunesId};
    id storeController = [[CustomStoreProductViewController alloc] init];
    if ([storeController respondsToSelector:@selector(setDelegate:)]) {
      [storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      [storeController loadProductWithParameters:productParams completionBlock:^(BOOL result, NSError *error) {
        if (result) {
          UALOG_DEBUG(@"Preloading product information succeeded for id: %@.", iTunesId);
          @synchronized(_appSheetCache) {
            [_appSheetCache setValue:storeController forKey:iTunesId];
          }
        } else {
          UALOG_DEBUG(@"Preloading product information failed for id: %@ with error: %@", iTunesId, error);
        }
      }];
    });
  }
}

- (id)getAppSheetController:(NSString *)iTunesId {
  @synchronized(_appSheetCache) {
    return [_appSheetCache valueForKey:iTunesId];
  }
}

- (void)openAppSheetWithId:(NSString *)iTunesId toViewController:(UIViewController *)targetViewController withCompletionBlock:(void (^)(BOOL result, NSError *error))completionBlock {
  if ([UnityAdsAppSheetManager canOpenStoreProductViewController]) {
    if (![iTunesId isKindOfClass:[NSString class]] || iTunesId == nil || [iTunesId length] < 1) return;
    
    NSDictionary *productParams = @{@"id":iTunesId};
    id storeController = [[CustomStoreProductViewController alloc] init];
    if ([storeController respondsToSelector:@selector(setDelegate:)]) {
      [storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      [storeController loadProductWithParameters:productParams completionBlock:^(BOOL result, NSError *error) {
        completionBlock(result, error);
        dispatch_async(dispatch_get_main_queue(), ^{
          if(result) {
            [targetViewController presentViewController:storeController animated:YES completion:nil];
          }
        });
      }];
    });
  }
}

+ (BOOL)canOpenStoreProductViewController {
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  return [storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)];
}

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController {
  UALOG_DEBUG(@"");
  if (viewController.presentingViewController != nil) {
    [viewController.presentingViewController dismissViewControllerAnimated:NO completion:nil];
    if ([UnityAdsDevice getIOSMajorVersion] >= 8) {
      __block NSString * currentItunesId = nil;
      @synchronized (_appSheetCache) {
        [_appSheetCache enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
          if ([obj isEqual:viewController]) {
            currentItunesId = key;
            *stop = YES;
          }
        }];
        if (!currentItunesId) return;
        [_appSheetCache removeObjectForKey:currentItunesId];
      }
      [self preloadAppSheetWithId:currentItunesId];
    }
  }
}

@end
