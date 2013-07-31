//
//  UnityAdsViewState.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewState.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"


@interface UnityAdsViewState ()
  @property (nonatomic, weak) UIViewController *targetController;
@end

@implementation UnityAdsViewState

@synthesize storeController = _storeController;

- (id)init {
  self = [super init];
  self.waitingToBeShown = false;
  return self;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = false;
}

- (void)willBeShown {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = true;
}

- (void)wasShown {
  UALOG_DEBUG(@"");
  self.waitingToBeShown = false;
}

- (void)applyOptions:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  if ([options objectForKey:kUnityAdsNativeEventShowSpinner] != nil) {
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventShowSpinner data:[options objectForKey:kUnityAdsNativeEventShowSpinner]];
  }
  else if ([options objectForKey:kUnityAdsNativeEventHideSpinner] != nil) {
    [[UnityAdsWebAppController sharedInstance] sendNativeEventToWebApp:kUnityAdsNativeEventHideSpinner data:[options objectForKey:kUnityAdsNativeEventHideSpinner]];
  }
}

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeInvalid;
}

- (void)openAppStoreWithData:(NSDictionary *)data inViewController:(UIViewController *)targetViewController {
  UALOG_DEBUG(@"");
  
  BOOL bypassAppSheet = false;
  NSString *iTunesId = nil;
  NSString *clickUrl = nil;
  
  if (data != nil) {
    if ([data objectForKey:kUnityAdsWebViewEventDataBypassAppSheetKey] != nil) {
      bypassAppSheet = [[data objectForKey:kUnityAdsWebViewEventDataBypassAppSheetKey] boolValue];
    }
    if ([data objectForKey:kUnityAdsCampaignStoreIDKey] != nil && [[data objectForKey:kUnityAdsCampaignStoreIDKey] isKindOfClass:[NSString class]]) {
      iTunesId = [data objectForKey:kUnityAdsCampaignStoreIDKey];
    }
    if ([data objectForKey:kUnityAdsWebViewEventDataClickUrlKey] != nil && [[data objectForKey:kUnityAdsWebViewEventDataClickUrlKey] isKindOfClass:[NSString class]]) {
      clickUrl = [data objectForKey:kUnityAdsWebViewEventDataClickUrlKey];
    }
  }
  
  if (iTunesId != nil && !bypassAppSheet && [self _canOpenStoreProductViewController]) {
    UALOG_DEBUG(@"Opening Appstore in AppSheet: %@", iTunesId);
    [self openAppSheetWithId:iTunesId toViewController:targetViewController];
  }
  else if (clickUrl != nil) {
    UALOG_DEBUG(@"Opening Appstore with clickUrl: %@", clickUrl);
    [self openAppStoreWithUrl:clickUrl];
  }
}


#pragma mark - AppStore opening

- (void)preloadAppSheetWithId:(NSString *)iTunesId {
  UALOG_DEBUG(@"");
  if ([self _canOpenStoreProductViewController]) {
    UALOG_DEBUG(@"Can open storeProductViewController");
    if (![iTunesId isKindOfClass:[NSString class]] || iTunesId == nil || [iTunesId length] < 1) return;
    Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
    /*
     FIX: This _could_ bug someday. The key @"id" is written literally (and
     not using SKStoreProductParameterITunesItemIdentifier), so that
     with some compiler options (or linker flags) you wouldn't get errors.
     
     The way this could bug someday is that Apple changes the contents of
     SKStoreProductParameterITunesItemIdentifier.
     
     HOWTOFIX: Find a way to reflect global constant SKStoreProductParameterITunesItemIdentifier
     by using string value and not the constant itself.
     */
    NSDictionary *productParams = @{@"id":iTunesId};
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    /*
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"Result: %i", result);
      if (result) {
        dispatch_async(dispatch_get_main_queue(), ^{
          self.storeController = nil;
        });
      }
      else {
        UALOG_DEBUG(@"Loading product information failed: %@", error);
      }
    };*/
    
    SEL loadProduct = @selector(loadProductWithParameters:completionBlock:);
    if ([self.storeController respondsToSelector:loadProduct]) {
      [self performSelectorInBackground:@selector(backgroundLoadProduct:) withObject:productParams];
    }
  }
}

- (void)backgroundLoadProduct:(id)productParams {
  SEL loadProduct = @selector(loadProductWithParameters:completionBlock:);
  
  void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
    UALOG_DEBUG(@"Result: %i", result);
    if (result) {
      dispatch_async(dispatch_get_main_queue(), ^{
        self.storeController = nil;
      });
    }
    else {
      UALOG_DEBUG(@"Loading product information failed: %@", error);
    }
  };
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
  [self.storeController performSelector:loadProduct withObject:productParams withObject:storeControllerComplete];
#pragma clang diagnostic pop
}

- (BOOL)_canOpenStoreProductViewController {
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  return [storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)];
}

- (void)openAppSheetWithId:(NSString *)iTunesId toViewController:(UIViewController *)targetViewController {
  Class storeProductViewControllerClass = NSClassFromString(@"SKStoreProductViewController");
  if ([storeProductViewControllerClass instancesRespondToSelector:@selector(loadProductWithParameters:completionBlock:)] == YES) {
    if (![iTunesId isKindOfClass:[NSString class]] || iTunesId == nil || [iTunesId length] < 1) return;
    
    /*
     FIX: This _could_ bug someday. The key @"id" is written literally (and
     not using SKStoreProductParameterITunesItemIdentifier), so that
     with some compiler options (or linker flags) you wouldn't get errors.
     
     The way this could bug someday is that Apple changes the contents of
     SKStoreProductParameterITunesItemIdentifier.
     
     HOWTOFIX: Find a way to reflect global constant SKStoreProductParameterITunesItemIdentifier
     by using string value and not the constant itself.
     */
    NSDictionary *productParams = @{@"id":iTunesId};
    
    self.storeController = [[storeProductViewControllerClass alloc] init];
    
    if ([self.storeController respondsToSelector:@selector(setDelegate:)]) {
      [self.storeController performSelector:@selector(setDelegate:) withObject:self];
    }
    
    void (^storeControllerComplete)(BOOL result, NSError *error) = ^(BOOL result, NSError *error) {
      UALOG_DEBUG(@"Result: %i", result);
      if (result) {
        dispatch_async(dispatch_get_main_queue(), ^{
          self.targetController = targetViewController;
          [targetViewController presentViewController:self.storeController animated:YES completion:nil];
          UnityAdsCampaign *campaign = [[UnityAdsCampaignManager sharedInstance] getCampaignWithITunesId:iTunesId];
          
          if (campaign != nil) {
            [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:campaign];
          }
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

- (void)openAppStoreWithUrl:(NSString *)clickUrl {
  if (clickUrl == nil) return;
  
  UnityAdsCampaign *campaign = [[UnityAdsCampaignManager sharedInstance] getCampaignWithClickUrl:clickUrl];
  
  if (campaign != nil) {
    [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:campaign];
  }
  
  if (self.delegate != nil) {
    [self.delegate stateNotification:kUnityAdsStateActionWillLeaveApplication];
  }
  
  // DOES NOT INITIALIZE WEBVIEW
  [[UnityAdsWebAppController sharedInstance] openExternalUrl:clickUrl];
  return;
}


#pragma mark - SKStoreProductViewControllerDelegate

- (void)productViewControllerDidFinish:(SKStoreProductViewController *)viewController {
  UALOG_DEBUG(@"");
  if (self.targetController != nil) {
    [self.targetController dismissViewControllerAnimated:YES completion:nil];
  }
  
  self.storeController = nil;
}

- (void)dealloc {
  self.targetController = nil;
  self.storeController = nil;
}

@end
