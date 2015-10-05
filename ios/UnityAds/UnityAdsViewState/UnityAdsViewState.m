//
//  UnityAdsViewState.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewState.h"
#import "../UnityAdsData/UnityAdsAnalyticsUploader.h"
#import "UnityAdsAppSheetManager.h"

@implementation UnityAdsViewState

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
  
  if (iTunesId != nil && !bypassAppSheet && [UnityAdsAppSheetManager canOpenStoreProductViewController]) {
    UALOG_DEBUG(@"Opening Appstore in AppSheet: %@", iTunesId);
    [self openAppSheetWithId:iTunesId toViewController:targetViewController withCompletionBlock:^(BOOL result, NSError *error) {
      if (error)
      {
        UALOG_DEBUG(@"Error %@ opening AppSheet. Opening Appstore with clickUrl instead", error);
        [self openAppStoreWithUrl:clickUrl];
      }
    }];
  }
  else if (clickUrl != nil) {
    UALOG_DEBUG(@"Opening Appstore with clickUrl: %@", clickUrl);
    [self openAppStoreWithUrl:clickUrl];
  }
}

- (void)openAppSheetWithId:(NSString *)iTunesId toViewController:(UIViewController *)targetViewController withCompletionBlock:(void (^)(BOOL result, NSError *error))completionBlock {
  [self applyOptions:@{kUnityAdsNativeEventShowSpinner:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}}];
  id storeController = [[UnityAdsAppSheetManager sharedInstance] getAppSheetController:iTunesId];
  if(storeController != nil) {
    dispatch_async(dispatch_get_main_queue(), ^{
      [self applyOptions:@{kUnityAdsNativeEventHideSpinner:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}}];
      [targetViewController presentViewController:storeController animated:YES completion:nil];
      UnityAdsCampaign *campaign = [[UnityAdsCampaignManager sharedInstance] getCampaignWithITunesId:iTunesId];
      if (campaign != nil) {
        [[UnityAdsAnalyticsUploader sharedInstance] sendOpenAppStoreRequest:campaign];
      }
      completionBlock(YES,nil);
    });
  } else {
    [[UnityAdsAppSheetManager sharedInstance] openAppSheetWithId:iTunesId toViewController:targetViewController withCompletionBlock:^(BOOL result, NSError *error) {
      [self applyOptions:@{kUnityAdsNativeEventHideSpinner:@{kUnityAdsTextKeyKey:kUnityAdsTextKeyLoading}}];
      dispatch_async(dispatch_get_main_queue(), ^{ completionBlock(result,error); });
    }];
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

@end
