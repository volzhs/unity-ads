//
//  UnityAdsViewStateEndScreen.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/5/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateEndScreen.h"

@interface UnityAdsViewStateEndScreen ()
  @property (nonatomic, strong) UIViewController *storeController;
  @property (nonatomic, assign) UIViewController *targetController;
@end

@implementation UnityAdsViewStateEndScreen

#pragma mark - AppStore opening

- (BOOL)_canOpenStoreProductViewController {
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  return [storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)];
}

- (void)openAppStoreWithData:(NSDictionary *)data inViewController:(UIViewController *)targetViewController {
  UALOG_DEBUG(@"");
  
  if (![self _canOpenStoreProductViewController] || [[UnityAdsCampaignManager sharedInstance] selectedCampaign].bypassAppSheet == YES) {
    NSString *clickUrl = [data objectForKey:kUnityAdsWebViewEventDataClickUrlKey];
    if (clickUrl == nil) return;
    UALOG_DEBUG(@"Cannot open store product view controller, falling back to click URL.");
    [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
    
    if (self.delegate != nil) {
      [self.delegate stateNotification:kUnityAdsStateActionWillLeaveApplication];
    }
    
    // DOES NOT INITIALIZE WEBVIEW
    UALOG_DEBUG(@"CLICK_URL: %@", clickUrl);
    [[UnityAdsWebAppController sharedInstance] openExternalUrl:clickUrl];
    return;
  }
  
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  if ([storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)] == YES) {
    if (![[data objectForKey:kUnityAdsCampaignStoreIDKey] isKindOfClass:[NSString class]]) return;
    NSString *gameId = nil;
    gameId = [data valueForKey:kUnityAdsCampaignStoreIDKey];
    if (gameId == nil || [gameId length] < 1) return;
    
    /*
     FIX: This _could_ bug someday. The key @"id" is written literally (and
     not using SKStoreProductParameterITunesItemIdentifier), so that
     with some compiler options (or linker flags) you wouldn't get errors.
     
     The way this could bug someday is that Apple changes the contents of
     SKStoreProductParameterITunesItemIdentifier.
     
     HOWTOFIX: Find a way to reflect global constant SKStoreProductParameterITunesItemIdentifier
     by using string value and not the constant itself.
     */
    NSDictionary *productParams = @{@"id":gameId};
    
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    if ([self.storeController respondsToSelector:@selector(setDelegate:)]) {
      [self.storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"RESULT: %i", result);
      if (result) {
        dispatch_async(dispatch_get_main_queue(), ^{
          self.targetController = targetViewController;
          [targetViewController presentViewController:self.storeController animated:YES completion:nil];
          [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:[[UnityAdsCampaignManager sharedInstance] selectedCampaign]];
        });
      }
      else {
        UALOG_DEBUG(@"Loading product information failed: %@", error);
      }
      
      [self applyOptions:@{kUnityAdsNativeEventHideSpinner:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}}];
    };
    
    [self applyOptions:@{kUnityAdsNativeEventShowSpinner:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}}];
    
    SEL loadProduct = @selector(loadProductWithParameters:completionBlock:);
    if ([self.storeController respondsToSelector:loadProduct]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
      [self.storeController performSelector:loadProduct withObject:productParams withObject:storeControllerComplete];
#pragma clang diagnostic pop
    }
  }
}

#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController {
  UALOG_DEBUG(@"");
  if (self.targetController != nil) {
    [self.targetController dismissViewControllerAnimated:YES completion:nil];
  }
}

- (void)dealloc {
  self.targetController = nil;
  self.storeController = nil;
}

@end